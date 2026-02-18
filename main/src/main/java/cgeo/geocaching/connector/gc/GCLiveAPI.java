package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyAndTerrainGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyGeocacheFilter;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.HiddenGeocacheFilter;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.filters.core.SizeGeocacheFilter;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.filters.core.TerrainGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.network.HttpResponse;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.sorting.GeocacheSort;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.reactivex.rxjava3.core.Single;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Handles requests against the official GC Live API at https://api.groundspeak.com/v1.
 * Uses OAuth2 Bearer tokens (separate from GCAuthAPI's session-based tokens).
 */
public final class GCLiveAPI {

    private static final String API_BASE_URL = "https://api.groundspeak.com/v1";
    private static final String TOKEN_ENDPOINT = "https://oauth.geocaching.com/token";

    /** Maximum number of geocaches per batch request */
    private static final int BATCH_SIZE = 50;

    /** Access token TTL in seconds — refresh before expiry (GC tokens last ~3600s) */
    private static final long TOKEN_TTL_SECONDS = 3400;

    /** Maximum retry attempts for rate-limited or server-error requests */
    private static final int MAX_RETRIES = 2;

    /** Delay in ms before retrying after a 5xx server error */
    private static final long SERVER_ERROR_DELAY_MS = 1000;

    /** Callback interface for rate limit countdown notifications. */
    public interface RateLimitCallback {
        void onRateLimited(int secondsRemaining);
    }

    /** Callback interface for batch fetch progress notifications. */
    public interface BatchProgressCallback {
        void onProgress(int fetched, int total);
    }

    /**
     * Callback references are updated from the main/UI thread (typically via {@link Settings})
     * and read/invoked from worker threads (e.g. network scheduler threads).
     * <p>
     * Access is intentionally lock-free; the {@code volatile} keyword provides visibility
     * guarantees only. Implementations must be thread-safe and tolerate being invoked
     * concurrently with {@code set*Callback} updates.
     */
    private static volatile RateLimitCallback rateLimitCallback;
    private static volatile BatchProgressCallback batchProgressCallback;

    /**
     * Sets a callback to be notified during rate limit waits.
     * The callback may be invoked from background threads. Pass {@code null} to clear.
     */
    public static void setRateLimitCallback(@Nullable final RateLimitCallback callback) {
        rateLimitCallback = callback;
    }

    /**
     * Sets a callback to be notified of batch fetch progress.
     * The callback may be invoked from background threads. Pass {@code null} to clear.
     */
    public static void setBatchProgressCallback(@Nullable final BatchProgressCallback callback) {
        batchProgressCallback = callback;
    }

    private static final Object TOKEN_LOCK = new Object();
    private static String cachedAccessToken;
    private static long cachedTokenExpires;

    // Fields to request from the Live API — never include owner.profileText
    private static final String FIELDS_DETAIL = "referenceCode,name,difficulty,terrain,"
            + "favoritePoints,trackableCount,placedDate,geocacheType,geocacheSize,"
            + "status,location,postedCoordinates,lastVisitedDate,ownerCode,"
            + "owner[referenceCode,username],ownerAlias,isPremiumOnly,"
            + "shortDescription,longDescription,hints,attributes,"
            + "additionalWaypoints,findCount,hasSolutionChecker,userData,containsHtml";

    private GCLiveAPI() {
        // utility class
    }

    // ---- Token Management ----

    /**
     * Ensures a valid access token is available, refreshing if necessary.
     * Intended for pre-warming from the UI (e.g. home screen status check).
     * Blocks the calling thread — call from a background scheduler.
     */
    @WorkerThread
    public static void ensureTokenValid() {
        getAccessToken();
    }

    /**
     * Returns a valid access token, refreshing if necessary.
     * Blocks the calling thread.
     */
    @WorkerThread
    @Nullable
    private static String getAccessToken() {
        synchronized (TOKEN_LOCK) {
            final long now = System.currentTimeMillis() / 1000;
            if (cachedAccessToken != null && now < cachedTokenExpires) {
                return cachedAccessToken;
            }

            // Try from settings
            final String storedToken = Settings.getGCLiveAccessToken();
            final long issuedAt = Settings.getGCLiveTokenIssuedAt();
            if (storedToken != null && issuedAt + TOKEN_TTL_SECONDS > now) {
                cachedAccessToken = storedToken;
                cachedTokenExpires = issuedAt + TOKEN_TTL_SECONDS;
                return cachedAccessToken;
            }

            // Need to refresh
            final String refreshToken = Settings.getGCLiveRefreshToken();
            if (StringUtils.isBlank(refreshToken)) {
                Log.w("GCLiveAPI: No refresh token available");
                return null;
            }

            return refreshAccessToken(refreshToken);
        }
    }

    /**
     * Refreshes the access token using the refresh token.
     * Must be called within TOKEN_LOCK.
     */
    @Nullable
    private static String refreshAccessToken(final String refreshToken) {
        try {
            final String clientId = LocalizationUtils.getString(R.string.gc_live_client_id);
            final String clientSecret = LocalizationUtils.getString(R.string.gc_live_client_secret);

            final Parameters body = new Parameters();
            body.put("grant_type", "refresh_token");
            body.put("client_id", clientId);
            body.put("client_secret", clientSecret);
            body.put("refresh_token", refreshToken);

            final HttpResponse response = new HttpRequest()
                    .uriBase(TOKEN_ENDPOINT)
                    .uri("")
                    .bodyForm(body)
                    .request()
                    .blockingGet();

            final int status = response.getStatusCode();
            if (status != 200) {
                Log.e("GCLiveAPI: Token refresh HTTP " + status);
                return null;
            }

            final TokenResponse token = response.parseJson(TokenResponse.class, null);
            if (token == null || StringUtils.isBlank(token.accessToken)) {
                Log.e("GCLiveAPI: Token refresh returned empty access token");
                return null;
            }

            final long now = System.currentTimeMillis() / 1000;
            Settings.setGCLiveAccessToken(token.accessToken);
            Settings.setGCLiveTokenIssuedAt(now);
            if (StringUtils.isNotBlank(token.refreshToken)) {
                Settings.setGCLiveRefreshToken(token.refreshToken);
            }

            cachedAccessToken = token.accessToken;
            cachedTokenExpires = now + TOKEN_TTL_SECONDS;
            Log.i("GCLiveAPI: Token refreshed successfully");
            return cachedAccessToken;
        } catch (final Exception e) {
            Log.e("GCLiveAPI: Token refresh failed", e);
            return null;
        }
    }

    /** Creates an HttpRequest with Bearer auth targeting the Live API base URL. */
    static HttpRequest apiReq() {
        return new HttpRequest().requestPreparer(reqBuilder -> {
            final String token = getAccessToken();
            if (token != null) {
                reqBuilder.addHeader("Authorization", "Bearer " + token);
            } else {
                Log.w("GCLiveAPI: No access token available");
            }
            return Single.just(reqBuilder);
        }).uriBase(API_BASE_URL);
    }

    // ---- Single Cache Fetch ----

    @WorkerThread
    @Nullable
    static SearchResult searchByGeocode(@NonNull final String geocode, @Nullable final DisposableHandler handler) {
        DisposableHandler.sendLoadProgressDetail(handler, R.string.cache_dialog_loading_details_status_loadpage);

        final LiveApiGeocache apiCache = fetchGeocacheWithRetry(geocode);
        if (apiCache == null) {
            Log.e("GCLiveAPI.searchByGeocode: No data for " + geocode);
            final SearchResult search = new SearchResult();
            search.setError(GCConnector.getInstance(), StatusCode.COMMUNICATION_ERROR);
            return search;
        }

        final Geocache cache = mapToGeocache(apiCache);
        DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));

        // Fetch logs and spoiler images via API
        fetchAndSaveLogs(geocode);
        fetchAndSaveSpoilers(cache);

        final SearchResult result = new SearchResult();
        result.addGeocode(cache.getGeocode());
        return result;
    }

    // ---- Search ----

    /** Max results per search API call (Live API maximum is 100) */
    private static final int SEARCH_PAGE_SIZE = 100;

    /** Lightweight fields for search results (no descriptions, hints, etc.) */
    private static final String FIELDS_SEARCH = "referenceCode,name,difficulty,terrain,"
            + "favoritePoints,trackableCount,placedDate,geocacheType,geocacheSize,"
            + "status,postedCoordinates,lastVisitedDate,ownerCode,"
            + "owner[referenceCode,username],ownerAlias,isPremiumOnly,"
            + "location,userData,findCount,attributes";

    /**
     * Searches for geocaches using the Live API search endpoint.
     *
     * @param filter   the filter to apply
     * @param sort     the sort order
     * @param take     max results to return (will be capped to {@link #SEARCH_PAGE_SIZE})
     * @param skip     number of results to skip (for pagination)
     * @return search result with geocodes
     */
    @WorkerThread
    @NonNull
    static SearchResult searchByFilter(@NonNull final GeocacheFilter filter, @NonNull final GeocacheSort sort,
                                       final int take, final int skip) {
        final SearchResult result = new SearchResult();

        try {
            final String q = buildSearchQuery(filter);
            if (q == null) {
                return result;
            }

            final int effectiveTake = Math.min(take, SEARCH_PAGE_SIZE);
            final String sortField = mapSortType(sort.getEffectiveType());
            final boolean sortAsc = sort.isEffectiveAscending();

            final HttpResponse response = apiReq()
                    .uri("/geocaches/search")
                    .uriParams("q", q, "sort", sortField, "asc", String.valueOf(sortAsc),
                            "skip", String.valueOf(skip), "take", String.valueOf(effectiveTake),
                            "fields", FIELDS_SEARCH, "lite", "true")
                    .request()
                    .blockingGet();

            final int status = response.getStatusCode();
            if (status != 200) {
                Log.w("GCLiveAPI: search HTTP " + status + ": " + response.getBodyString());
                result.setError(GCConnector.getInstance(), StatusCode.COMMUNICATION_ERROR);
                return result;
            }

            // Parse total count from header for pagination
            final String totalHeader = response.getResponse() != null
                    ? response.getResponse().header("x-total-count") : null;
            int totalCount = 0;
            if (StringUtils.isNotBlank(totalHeader)) {
                try {
                    totalCount = Integer.parseInt(totalHeader);
                } catch (final NumberFormatException ignored) {
                    // ignore
                }
            }

            final LiveApiGeocache[] apiCaches = response.parseJson(LiveApiGeocache[].class, null);
            if (apiCaches == null || apiCaches.length == 0) {
                return result;
            }

            final List<Geocache> caches = new ArrayList<>(apiCaches.length);
            for (final LiveApiGeocache apiCache : apiCaches) {
                final Geocache cache = mapToGeocache(apiCache);
                cache.setDetailed(false);  // search results are not fully detailed
                caches.add(cache);
            }

            result.addAndPutInCache(caches);

            // Pagination info
            final int leftToFetch = Math.max(0, totalCount - skip - effectiveTake);
            result.setLeftToFetch(GCConnector.getInstance(), leftToFetch);
            result.setPartialResult(GCConnector.getInstance(), apiCaches.length == effectiveTake && leftToFetch > 0);

        } catch (final Exception e) {
            Log.w("GCLiveAPI: search failed", e);
            result.setError(GCConnector.getInstance(), StatusCode.COMMUNICATION_ERROR);
        }

        return result;
    }

    /**
     * Builds the 'q' query parameter for the Live API search from a GeocacheFilter.
     * Returns null if no valid location/origin filter is present.
     */
    @Nullable
    private static String buildSearchQuery(@NonNull final GeocacheFilter filter) {
        final List<String> parts = new ArrayList<>();

        for (final BaseGeocacheFilter bf : filter.getAndChainIfPossible()) {
            if (bf.getType() == GeocacheFilterType.DISTANCE) {
                // Handle DISTANCE specially — it produces location/box
                final DistanceGeocacheFilter df = (DistanceGeocacheFilter) bf;
                final Geopoint coord = df.getEffectiveCoordinate();
                if (coord != null) {
                    if (df.getMaxRangeValue() != null) {
                        final Viewport box = new Viewport(coord, df.getMaxRangeValue());
                        parts.add("box:[[" + box.getLatitudeMax() + "," + box.getLongitudeMin()
                                + "],[" + box.getLatitudeMin() + "," + box.getLongitudeMax() + "]]");
                    } else {
                        parts.add("location:[" + coord.getLatitude() + "," + coord.getLongitude() + "]");
                    }
                }
            } else {
                addFilterToParts(bf, parts);
            }
        }

        // Must have at least a location/box in the query
        if (parts.isEmpty()) {
            final Geopoint currentPos = cgeo.geocaching.sensors.LocationDataProvider.getInstance().currentGeo().getCoords();
            if (currentPos != null) {
                parts.add(0, "location:[" + currentPos.getLatitude() + "," + currentPos.getLongitude() + "]");
            } else {
                return null;
            }
        }

        return StringUtils.join(parts, '+');
    }

    @Nullable
    private static String formatRange(@Nullable final Float min, @Nullable final Float max,
                                       final float defaultMin, final float defaultMax) {
        if (min == null && max == null) {
            return null;
        }
        final float rawFrom = min != null ? Math.round(Math.max(defaultMin, Math.min(defaultMax, min)) * 2f) / 2f : defaultMin;
        final float rawTo = max != null ? Math.round(Math.max(defaultMin, Math.min(defaultMax, max)) * 2f) / 2f : defaultMax;
        return Math.min(rawFrom, rawTo) + "-" + Math.max(rawFrom, rawTo);
    }

    private static final ThreadLocal<SimpleDateFormat> DATE_PARAM_FORMAT = ThreadLocal.withInitial(() -> {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    });

    private static String formatDateParam(final long millis) {
        return DATE_PARAM_FORMAT.get().format(new Date(millis));
    }

    /** Maps cgeo sort type to Live API sort field name. */
    @NonNull
    private static String mapSortType(@NonNull final GeocacheSort.SortType sortType) {
        switch (sortType) {
            case NAME:
                return "name";
            case FAVORITES:
            case FAVORITES_RATIO:
                return "favoritepoints";
            case DIFFICULTY:
                return "difficulty";
            case TERRAIN:
                return "terrain";
            case HIDDEN_DATE:
                return "placeddate";
            case LAST_FOUND:
                return "founddate";
            case DISTANCE:
            default:
                return "distance";
        }
    }

    // ---- Viewport Search (Map) ----

    /** Max caches to load for a viewport search */
    private static final int VIEWPORT_MAX_CACHES = 500;

    /** Page size for viewport search (Live API max is 100) */
    private static final int VIEWPORT_PAGE_SIZE = 100;

    /**
     * Searches for geocaches within a viewport bounding box via the Live API.
     * Fires all page requests in parallel for minimal latency.
     */
    @WorkerThread
    @NonNull
    static SearchResult searchByViewport(@NonNull final Viewport viewport, @Nullable final GeocacheFilter filter) {
        final SearchResult result = new SearchResult();

        try {
            // Build query: box + optional filter operators
            final List<String> parts = new ArrayList<>();
            parts.add("box:[[" + viewport.getLatitudeMax() + "," + viewport.getLongitudeMin()
                    + "],[" + viewport.getLatitudeMin() + "," + viewport.getLongitudeMax() + "]]");

            if (filter != null) {
                for (final BaseGeocacheFilter bf : filter.getAndChainIfPossible()) {
                    addFilterToParts(bf, parts);
                }
            }

            final String q = StringUtils.join(parts, '+');
            final int numPages = (VIEWPORT_MAX_CACHES + VIEWPORT_PAGE_SIZE - 1) / VIEWPORT_PAGE_SIZE;

            // Fetch all pages sequentially to avoid concurrent rate-limit issues
            final List<Geocache> allCaches = new ArrayList<>();

            for (int page = 0; page < numPages; page++) {
                final int skip = page * VIEWPORT_PAGE_SIZE;
                try {
                    final HttpResponse response = apiReq()
                            .uri("/geocaches/search")
                            .uriParams("q", q, "skip", String.valueOf(skip),
                                    "take", String.valueOf(VIEWPORT_PAGE_SIZE),
                                    "fields", FIELDS_SEARCH, "lite", "true")
                            .request()
                            .blockingGet();

                    final int status = response.getStatusCode();
                    if (status == 200) {
                        final LiveApiGeocache[] apiCaches = response.parseJson(LiveApiGeocache[].class, null);
                        if (apiCaches != null) {
                            for (final LiveApiGeocache apiCache : apiCaches) {
                                final Geocache cache = mapToGeocache(apiCache);
                                cache.setDetailed(false);
                                allCaches.add(cache);
                            }
                        }
                    } else if (status == 429) {
                        waitForRateLimit(response);
                        // retry this page after rate limit wait
                        page--;
                    } else {
                        Log.w("GCLiveAPI: viewport page skip=" + skip + " HTTP " + status);
                    }
                } catch (final Exception e) {
                    Log.w("GCLiveAPI: viewport page skip=" + skip + " failed", e);
                }
            }

            result.addAndPutInCache(allCaches);

        } catch (final Exception e) {
            Log.w("GCLiveAPI: viewport search failed", e);
            result.setError(GCConnector.getInstance(), StatusCode.COMMUNICATION_ERROR);
        }

        return result;
    }

    /**
     * Adds filter operators to the query parts list (shared by searchByFilter and searchByViewport).
     * Only adds operators for filter types supported by the Live API.
     */
    private static void addFilterToParts(@NonNull final BaseGeocacheFilter bf, @NonNull final List<String> parts) {
        switch (bf.getType()) {
            case TYPE: {
                final TypeGeocacheFilter tf = (TypeGeocacheFilter) bf;
                final List<String> ids = new ArrayList<>();
                for (final CacheType ct : tf.getRawValues()) {
                    if (ct != CacheType.ALL) {
                        ids.add(ct.wptTypeId);
                    }
                }
                if (!ids.isEmpty()) {
                    parts.add("type:" + StringUtils.join(ids, ','));
                }
                break;
            }
            case SIZE: {
                final SizeGeocacheFilter sf = (SizeGeocacheFilter) bf;
                final List<String> ids = new ArrayList<>();
                for (final CacheSize cs : sf.getValues()) {
                    for (final int gcId : CacheSize.getGcIdsForSize(cs)) {
                        ids.add(String.valueOf(gcId));
                    }
                }
                if (!ids.isEmpty()) {
                    parts.add("size:" + StringUtils.join(ids, ','));
                }
                break;
            }
            case DIFFICULTY: {
                final DifficultyGeocacheFilter df = (DifficultyGeocacheFilter) bf;
                final String range = formatRange(df.getMinRangeValue(), df.getMaxRangeValue(), 1f, 5f);
                if (range != null) {
                    parts.add("diff:" + range);
                }
                break;
            }
            case TERRAIN: {
                final TerrainGeocacheFilter tf = (TerrainGeocacheFilter) bf;
                final String range = formatRange(tf.getMinRangeValue(), tf.getMaxRangeValue(), 1f, 5f);
                if (range != null) {
                    parts.add("terr:" + range);
                }
                break;
            }
            case DIFFICULTY_TERRAIN: {
                final DifficultyAndTerrainGeocacheFilter dtf = (DifficultyAndTerrainGeocacheFilter) bf;
                final String dr = formatRange(dtf.difficultyGeocacheFilter.getMinRangeValue(),
                        dtf.difficultyGeocacheFilter.getMaxRangeValue(), 1f, 5f);
                if (dr != null) {
                    parts.add("diff:" + dr);
                }
                final String tr = formatRange(dtf.terrainGeocacheFilter.getMinRangeValue(),
                        dtf.terrainGeocacheFilter.getMaxRangeValue(), 1f, 5f);
                if (tr != null) {
                    parts.add("terr:" + tr);
                }
                break;
            }
            case NAME: {
                final NameGeocacheFilter nf = (NameGeocacheFilter) bf;
                final String text = nf.getStringFilter().getTextValue();
                if (StringUtils.isNotBlank(text)) {
                    parts.add("name:" + text);
                }
                break;
            }
            case OWNER: {
                final OwnerGeocacheFilter of = (OwnerGeocacheFilter) bf;
                final String owner = of.getStringFilter().getTextValue();
                if (StringUtils.isNotBlank(owner)) {
                    parts.add("hby:" + owner);
                }
                break;
            }
            case STATUS: {
                final StatusGeocacheFilter stf = (StatusGeocacheFilter) bf;
                if (stf.getStatusFound() != null) {
                    final String user = Settings.getUserName();
                    if (StringUtils.isNotBlank(user)) {
                        parts.add(stf.getStatusFound() ? "fby:" + user : "fby:not(" + user + ")");
                    }
                }
                if (stf.getStatusOwned() != null) {
                    final String user = Settings.getUserName();
                    if (StringUtils.isNotBlank(user)) {
                        parts.add(stf.getStatusOwned() ? "hby:" + user : "hby:not(" + user + ")");
                    }
                }
                if (!stf.isExcludeArchived()) {
                    parts.add("ia:true");
                }
                break;
            }
            case HIDDEN:
            case EVENT_DATE: {
                final HiddenGeocacheFilter hf = (HiddenGeocacheFilter) bf;
                final String from = hf.getMinDate() != null ? formatDateParam(hf.getMinDate().getTime()) : null;
                final String to = hf.getMaxDate() != null ? formatDateParam(hf.getMaxDate().getTime()) : null;
                if (from != null || to != null) {
                    parts.add("pd:[" + (from != null ? from : "") + "," + (to != null ? to : "") + "]");
                }
                break;
            }
            case LOG_ENTRY: {
                final LogEntryGeocacheFilter lf = (LogEntryGeocacheFilter) bf;
                final String logUser = lf.getFoundByUser();
                if (StringUtils.isNotBlank(logUser)) {
                    parts.add(lf.isInverse() ? "fby:not(" + logUser + ")" : "fby:" + logUser);
                }
                break;
            }
            default:
                break;
        }
    }

    // ---- Cache Actions (watchlist, favorites, coords, notes) ----

    /** Adds a cache to the user's watchlist. */
    @WorkerThread
    static boolean addToWatchlist(@NonNull final String geocode) {
        return simpleApiCall(HttpRequest.Method.PUT, "/geocaches/" + geocode + "/watchlist");
    }

    /** Removes a cache from the user's watchlist. */
    @WorkerThread
    static boolean removeFromWatchlist(@NonNull final String geocode) {
        return simpleApiCall(HttpRequest.Method.DELETE, "/geocaches/" + geocode + "/watchlist");
    }

    /** Adds a cache to the user's favorites. */
    @WorkerThread
    static boolean addToFavorites(@NonNull final String geocode) {
        return simpleApiCall(HttpRequest.Method.PUT, "/geocaches/" + geocode + "/favoritedby");
    }

    /** Removes a cache from the user's favorites. */
    @WorkerThread
    static boolean removeFromFavorites(@NonNull final String geocode) {
        return simpleApiCall(HttpRequest.Method.DELETE, "/geocaches/" + geocode + "/favoritedby");
    }

    /** Uploads corrected coordinates for a cache. */
    @WorkerThread
    static boolean uploadModifiedCoordinates(@NonNull final String geocode, @NonNull final Geopoint coords) {
        try {
            final ObjectNode bodyNode = JsonUtils.createObjectNode();
            bodyNode.put("latitude", coords.getLatitude());
            bodyNode.put("longitude", coords.getLongitude());
            final String body = bodyNode.toString();
            final HttpResponse response = apiReq()
                    .uri("/geocaches/" + geocode + "/correctedcoordinates")
                    .bodyJson(body)
                    .method(HttpRequest.Method.PUT)
                    .request()
                    .blockingGet();
            final int status = response.getStatusCode();
            if (status >= 200 && status < 300) {
                Log.i("GCLiveAPI: uploaded corrected coords for " + geocode);
                return true;
            }
            Log.w("GCLiveAPI: uploadModifiedCoordinates " + geocode + " HTTP " + status);
            return false;
        } catch (final Exception e) {
            Log.w("GCLiveAPI: uploadModifiedCoordinates failed for " + geocode, e);
            return false;
        }
    }

    /** Deletes corrected coordinates for a cache. */
    @WorkerThread
    static boolean deleteModifiedCoordinates(@NonNull final String geocode) {
        return simpleApiCall(HttpRequest.Method.DELETE, "/geocaches/" + geocode + "/correctedcoordinates");
    }

    /** Uploads a personal note for a cache. */
    @WorkerThread
    static boolean uploadPersonalNote(@NonNull final String geocode, @NonNull final String note) {
        try {
            final ObjectNode bodyNode = JsonUtils.createObjectNode();
            bodyNode.put("note", note);
            final String body = bodyNode.toString();
            final HttpResponse response = apiReq()
                    .uri("/geocaches/" + geocode + "/notes")
                    .bodyJson(body)
                    .method(HttpRequest.Method.PUT)
                    .request()
                    .blockingGet();
            final int status = response.getStatusCode();
            if (status >= 200 && status < 300) {
                Log.i("GCLiveAPI: uploaded personal note for " + geocode);
                return true;
            }
            Log.w("GCLiveAPI: uploadPersonalNote " + geocode + " HTTP " + status);
            return false;
        } catch (final Exception e) {
            Log.w("GCLiveAPI: uploadPersonalNote failed for " + geocode, e);
            return false;
        }
    }

    /** Adds a cache to the user's ignore list. */
    @WorkerThread
    static boolean ignoreCache(@NonNull final String geocode) {
        return simpleApiCall(HttpRequest.Method.PUT, "/geocaches/" + geocode + "/ignorelist");
    }

    // ---- Trackable Search ----

    private static final String TRACKABLE_FIELDS = "referenceCode,trackingNumber,name,goal,description,"
            + "releasedDate,originCountry,ownerCode,owner[referenceCode,username],"
            + "holder[referenceCode,username],currentGeocacheCode,currentGeocacheName,"
            + "trackableType[id,name,imageUrl],isMissing,isLocked,kilometersTraveled,"
            + "images[url,name,description]";

    /** Searches for a trackable via the Live API. */
    @WorkerThread
    @Nullable
    public static Trackable searchTrackable(@Nullable final String geocode, @Nullable final String guid, @Nullable final String id) {
        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid) && StringUtils.isBlank(id)) {
            return null;
        }

        try {
            final HttpResponse response;
            if (StringUtils.isNotBlank(geocode) && StringUtils.startsWithIgnoreCase(geocode, "TB")) {
                // Reference code lookup
                response = apiReq()
                        .uri("/trackables/" + geocode)
                        .uriParams("fields", TRACKABLE_FIELDS)
                        .request()
                        .blockingGet();
            } else {
                // Tracking number lookup (6-char codes, numeric IDs, etc.)
                final String trackingNumber = StringUtils.isNotBlank(geocode) ? geocode : id;
                if (StringUtils.isBlank(trackingNumber)) {
                    return null;
                }
                response = apiReq()
                        .uri("/trackables")
                        .uriParams("trackingNumber", trackingNumber, "fields", TRACKABLE_FIELDS)
                        .request()
                        .blockingGet();
            }

            final int status = response.getStatusCode();
            if (status != 200) {
                Log.w("GCLiveAPI: searchTrackable HTTP " + status);
                return null;
            }

            final JsonNode json = JsonUtils.reader.readTree(response.getBodyString());
            // Tracking number endpoint returns an array; reference code endpoint returns a single object
            final JsonNode tb = json.isArray() ? (json.size() > 0 ? json.get(0) : null) : json;
            if (tb == null) {
                return null;
            }

            return mapToTrackable(tb);
        } catch (final Exception e) {
            Log.w("GCLiveAPI: searchTrackable failed", e);
            return null;
        }
    }

    @NonNull
    private static Trackable mapToTrackable(@NonNull final JsonNode tb) {
        final Trackable trackable = new Trackable();
        trackable.forceSetBrand(TrackableBrand.TRAVELBUG);

        trackable.setGeocode(tb.path("referenceCode").asText());
        trackable.setTrackingcode(tb.path("trackingNumber").asText(null));
        trackable.setName(tb.path("name").asText());
        trackable.setGoal(tb.path("goal").asText(null));
        trackable.setDetails(tb.path("description").asText(null));
        trackable.setReleased(parseDate(tb.path("releasedDate").asText(null)));
        trackable.setOrigin(tb.path("originCountry").asText(null));

        // Owner
        final JsonNode owner = tb.path("owner");
        if (owner.has("username")) {
            trackable.setOwner(owner.path("username").asText());
            trackable.setOwnerGuid(owner.path("referenceCode").asText(null));
        }

        // Holder / spotted location
        final JsonNode holder = tb.path("holder");
        final String currentGeocacheCode = tb.path("currentGeocacheCode").asText(null);
        if (StringUtils.isNotBlank(currentGeocacheCode)) {
            trackable.setSpottedCacheGeocode(currentGeocacheCode);
            trackable.setSpottedName(tb.path("currentGeocacheName").asText(null));
            trackable.setSpottedType(Trackable.SPOTTED_CACHE);
        } else if (holder.has("username")) {
            trackable.setSpottedName(holder.path("username").asText());
            trackable.setSpottedGuid(holder.path("referenceCode").asText(null));
            // Check if holder is the owner
            final String ownerCode = tb.path("ownerCode").asText(null);
            final String holderCode = holder.path("referenceCode").asText(null);
            if (StringUtils.isNotBlank(ownerCode) && ownerCode.equals(holderCode)) {
                trackable.setSpottedType(Trackable.SPOTTED_OWNER);
            } else {
                trackable.setSpottedType(Trackable.SPOTTED_USER);
            }
        }

        // Type
        final JsonNode tbType = tb.path("trackableType");
        if (tbType.has("name")) {
            trackable.setType(tbType.path("name").asText());
        }
        if (tbType.has("imageUrl")) {
            trackable.setIconUrl(tbType.path("imageUrl").asText());
        }

        // Status
        trackable.setMissing(tb.path("isMissing").asBoolean(false));
        if (tb.path("isLocked").asBoolean(false)) {
            trackable.setIsLocked();
        }

        // Distance
        final double km = tb.path("kilometersTraveled").asDouble(0);
        if (km > 0) {
            trackable.setDistance((float) km);
        }

        // Image
        final JsonNode images = tb.path("images");
        if (images.isArray() && images.size() > 0) {
            trackable.setImage(images.get(0).path("url").asText(null));
        }

        return trackable;
    }

    /**
     * Returns the D/T combinations still needed for the 81 matrix.
     * Uses GET /v1/statistics/difficultyterrain which returns all 81 combos with counts.
     * We filter for count == 0 to get the needed ones.
     */
    @WorkerThread
    @NonNull
    static Collection<ImmutablePair<Float, Float>> getNeededDifficultyTerrainCombisFor81Matrix() {
        try {
            final HttpResponse response = apiReq()
                    .uri("/statistics/difficultyterrain")
                    .request()
                    .blockingGet();

            final int status = response.getStatusCode();
            if (status != 200) {
                Log.w("GCLiveAPI: getDTmatrix HTTP " + status);
                return Collections.emptyList();
            }

            final JsonNode json = JsonUtils.reader.readTree(response.getBodyString());
            final JsonNode counts = json.path("difficultyTerrainCounts");
            if (!counts.isArray()) {
                return Collections.emptyList();
            }

            final List<ImmutablePair<Float, Float>> needed = new ArrayList<>();
            // Build set of found combos
            final float[] levels = {1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f, 4.5f, 5f};
            final java.util.Set<String> found = new java.util.HashSet<>();
            for (final JsonNode entry : counts) {
                if (entry.path("count").asInt(0) > 0) {
                    found.add(entry.path("difficulty").asDouble() + "-" + entry.path("terrain").asDouble());
                }
            }
            // Anything not found is needed
            for (final float d : levels) {
                for (final float t : levels) {
                    if (!found.contains((double) d + "-" + (double) t)) {
                        needed.add(new ImmutablePair<>(d, t));
                    }
                }
            }
            return needed;
        } catch (final Exception e) {
            Log.w("GCLiveAPI: getDTmatrix failed", e);
            return Collections.emptyList();
        }
    }

    /** Uploads a field notes file via the Live API. */
    @WorkerThread
    static boolean uploadFieldNotes(@NonNull final File exportFile) {
        try {
            final HttpResponse response = apiReq()
                    .uri("/logdrafts/upload")
                    .method(HttpRequest.Method.POST)
                    .bodyForm(null, "file", "text/plain", exportFile)
                    .request()
                    .blockingGet();

            final int status = response.getStatusCode();
            if (status >= 200 && status < 300) {
                Log.i("GCLiveAPI: field notes uploaded successfully");
                return true;
            }
            Log.w("GCLiveAPI: uploadFieldNotes HTTP " + status);
            return false;
        } catch (final Exception e) {
            Log.w("GCLiveAPI: uploadFieldNotes failed", e);
            return false;
        }
    }

    // ---- Bookmark List Operations ----

    /** Fetches the user's bookmark lists via the Live API. */
    @WorkerThread
    @Nullable
    static List<GCList> searchBookmarkLists() {
        try {
            final HttpResponse response = apiReq()
                    .uri("/lists")
                    .uriParams("types", "bm", "skip", "0", "take", "100")
                    .request()
                    .blockingGet();

            final int status = response.getStatusCode();
            if (status != 200) {
                Log.w("GCLiveAPI: searchBookmarkLists HTTP " + status);
                return null;
            }

            final JsonNode json = JsonUtils.reader.readTree(response.getBodyString());
            final List<GCList> list = new ArrayList<>();
            for (final JsonNode row : json) {
                final String referenceCode = row.path("referenceCode").asText();
                final String name = row.path("name").asText();
                final int count = row.path("count").asInt();
                final Date date = parseDate(row.path("lastUpdatedDateUtc").asText());
                final long dateMs = date != null ? date.getTime() : 0;
                list.add(new GCList(referenceCode, name, count, true, dateMs, -1, true, null, null));
            }
            return list;
        } catch (final Exception e) {
            Log.w("GCLiveAPI: searchBookmarkLists failed", e);
            return null;
        }
    }

    /** Fetches the user's pocket queries via the Live API. */
    @WorkerThread
    @Nullable
    static List<GCList> searchPocketQueries() {
        try {
            final HttpResponse response = apiReq()
                    .uri("/users/me/lists")
                    .uriParams("types", "pq", "skip", "0", "take", "100",
                            "fields", "referenceCode,name,count,lastUpdatedDateUtc")
                    .request()
                    .blockingGet();

            final int status = response.getStatusCode();
            if (status != 200) {
                Log.w("GCLiveAPI: searchPocketQueries HTTP " + status);
                return null;
            }

            final JsonNode json = JsonUtils.reader.readTree(response.getBodyString());
            final List<GCList> list = new ArrayList<>();
            for (final JsonNode row : json) {
                final String referenceCode = row.path("referenceCode").asText();
                final String name = row.path("name").asText();
                final int count = row.path("count").asInt();
                final Date date = parseDate(row.path("lastUpdatedDateUtc").asText());
                final long dateMs = date != null ? date.getTime() : 0;
                // Store referenceCode as guid; PQs are always downloadable; not a bookmark list
                list.add(new GCList(referenceCode, name, count, true, dateMs, -1, false, null, null));
            }
            return list;
        } catch (final Exception e) {
            Log.w("GCLiveAPI: searchPocketQueries failed", e);
            return null;
        }
    }

    /** Fetches geocaches from a bookmark list. */
    @WorkerThread
    @Nullable
    public static SearchResult searchByBookmarkList(@NonNull final IConnector con, @NonNull final String listReferenceCode, final int alreadyTaken) {
        try {
            final HttpResponse response = apiReq()
                    .uri("/lists/" + listReferenceCode + "/geocaches")
                    .uriParams("skip", String.valueOf(alreadyTaken), "take", "100",
                            "lite", "true", "fields", "referenceCode,name,difficulty,terrain,geocacheType,geocacheSize,postedCoordinates,status,isPremiumOnly,owner[username]")
                    .request()
                    .blockingGet();

            final int status = response.getStatusCode();
            if (status != 200) {
                Log.w("GCLiveAPI: searchByBookmarkList HTTP " + status);
                return null;
            }

            final String totalHeader = response.getResponse() != null
                    ? response.getResponse().header("x-total-count") : null;
            int totalCount = 0;
            if (totalHeader != null) {
                try {
                    totalCount = Integer.parseInt(totalHeader);
                } catch (final NumberFormatException e) {
                    Log.w("GCLiveAPI: invalid x-total-count header: " + totalHeader);
                }
            }

            final JsonNode json = JsonUtils.reader.readTree(response.getBodyString());
            final List<Geocache> caches = new ArrayList<>();
            for (final JsonNode node : json) {
                final Geocache cache = new Geocache();
                cache.setGeocode(node.path("referenceCode").asText());
                cache.setName(node.path("name").asText());
                cache.setDifficulty((float) node.path("difficulty").asDouble());
                cache.setTerrain((float) node.path("terrain").asDouble());
                cache.setType(CacheType.getByWaypointType(String.valueOf(node.path("geocacheType").path("id").asInt())));
                cache.setSize(CacheSize.getByGcId(node.path("geocacheSize").path("id").asInt()));
                final JsonNode owner = node.path("owner");
                if (owner.has("username")) {
                    cache.setOwnerDisplayName(owner.path("username").asText());
                }
                final JsonNode coords = node.path("postedCoordinates");
                if (coords.has("latitude") && coords.has("longitude")) {
                    cache.setCoords(new Geopoint(coords.path("latitude").asDouble(), coords.path("longitude").asDouble()));
                }
                final String cacheStatus = node.path("status").asText();
                cache.setDisabled("Disabled".equals(cacheStatus));
                cache.setArchived("Archived".equals(cacheStatus) || "Locked".equals(cacheStatus));
                cache.setPremiumMembersOnly(node.path("isPremiumOnly").asBoolean());
                caches.add(cache);
            }

            final int currentFetched = alreadyTaken + caches.size();
            final SearchResult searchResult = new SearchResult(caches);
            searchResult.setLeftToFetch(con, totalCount - currentFetched);
            searchResult.setToContext(con, b -> {
                b.putInt(GCConnector.SEARCH_CONTEXT_TOOK_TOTAL, currentFetched);
                b.putString(GCConnector.SEARCH_CONTEXT_BOOKMARK, listReferenceCode);
            });

            return searchResult;
        } catch (final Exception e) {
            Log.w("GCLiveAPI: searchByBookmarkList failed", e);
            return null;
        }
    }

    /** Creates a new bookmark list via the Live API. Returns the reference code or null. */
    @WorkerThread
    @Nullable
    static String createBookmarkList(@NonNull final String name) {
        try {
            final ObjectNode typeNode = JsonUtils.createObjectNode();
            typeNode.put("code", "bm");
            final ObjectNode bodyNode = JsonUtils.createObjectNode();
            bodyNode.put("name", name);
            bodyNode.set("type", typeNode);
            final String body = bodyNode.toString();
            final HttpResponse response = apiReq()
                    .uri("/lists")
                    .method(HttpRequest.Method.POST)
                    .bodyJson(body)
                    .request()
                    .blockingGet();

            final int status = response.getStatusCode();
            if (status < 200 || status >= 300) {
                Log.w("GCLiveAPI: createBookmarkList HTTP " + status);
                return null;
            }

            final JsonNode json = JsonUtils.reader.readTree(response.getBodyString());
            return json.path("referenceCode").asText(null);
        } catch (final Exception e) {
            Log.w("GCLiveAPI: createBookmarkList failed", e);
            return null;
        }
    }

    /** Adds geocaches to a bookmark list via the Live API. */
    @WorkerThread
    static boolean addCachesToBookmarkList(@NonNull final String listReferenceCode, @NonNull final List<Geocache> geocaches) {
        try {
            final ArrayNode arrayNode = JsonUtils.createArrayNode();
            for (final Geocache geocache : geocaches) {
                if (ConnectorFactory.getConnector(geocache) instanceof GCConnector) {
                    arrayNode.add(new ObjectNode(JsonUtils.factory).put("referenceCode", geocache.getGeocode()));
                }
            }

            final HttpResponse response = apiReq()
                    .uri("/lists/" + listReferenceCode + "/geocaches")
                    .method(HttpRequest.Method.PUT)
                    .bodyJson(arrayNode.toString())
                    .request()
                    .blockingGet();

            final int status = response.getStatusCode();
            if (status >= 200 && status < 300) {
                Log.i("GCLiveAPI: added " + arrayNode.size() + " caches to list " + listReferenceCode);
                return true;
            }
            Log.w("GCLiveAPI: addCachesToBookmarkList HTTP " + status);
            return false;
        } catch (final Exception e) {
            Log.w("GCLiveAPI: addCachesToBookmarkList failed", e);
            return false;
        }
    }

    /**
     * Helper for simple PUT/DELETE API calls that expect a 2xx response with no body parsing.
     */
    private static boolean simpleApiCall(@NonNull final HttpRequest.Method method, @NonNull final String uri) {
        try {
            final HttpResponse response = apiReq()
                    .uri(uri)
                    .method(method)
                    .request()
                    .blockingGet();
            final int status = response.getStatusCode();
            if (status >= 200 && status < 300) {
                Log.i("GCLiveAPI: " + method + " " + uri + " OK");
                return true;
            }
            Log.w("GCLiveAPI: " + method + " " + uri + " HTTP " + status);
            return false;
        } catch (final Exception e) {
            Log.w("GCLiveAPI: " + method + " " + uri + " failed", e);
            return false;
        }
    }

    // ---- Log Fetching ----

    private static final String LOG_FIELDS = "referenceCode,text,loggedDate,type,owner[username,referenceCode],"
            + "images[url,name,description]";

    /** Fetches logs for a geocache via the Live API and saves them to DB. */
    @WorkerThread
    public static void fetchAndSaveLogs(@NonNull final String geocode) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                final HttpResponse response = apiReq()
                        .uri("/geocaches/" + geocode + "/geocachelogs")
                        .uriParams("fields", LOG_FIELDS, "take", "35")
                        .request()
                        .blockingGet();

                final int status = response.getStatusCode();
                if (status == 429) {
                    waitForRateLimit(response);
                    continue;
                }
                if (status >= 500) {
                    TimeUnit.MILLISECONDS.sleep(SERVER_ERROR_DELAY_MS);
                    continue;
                }
                if (status != 200) {
                    Log.w("GCLiveAPI: fetchLogs " + geocode + " HTTP " + status);
                    return;
                }

                final LiveApiLog[] apiLogs = response.parseJson(LiveApiLog[].class, null);
                if (apiLogs == null || apiLogs.length == 0) {
                    return;
                }

                final List<LogEntry> logs = new ArrayList<>(apiLogs.length);
                for (final LiveApiLog apiLog : apiLogs) {
                    logs.add(mapToLogEntry(apiLog));
                }

                DataStore.saveLogs(geocode, logs, true);
                Log.i("GCLiveAPI: Saved " + logs.size() + " logs for " + geocode);
                return;
            } catch (final Exception e) {
                Log.w("GCLiveAPI: fetchLogs attempt " + attempt + " failed for " + geocode, e);
            }
        }
    }

    @NonNull
    private static LogEntry mapToLogEntry(@NonNull final LiveApiLog apiLog) {
        final LogEntry.Builder builder = new LogEntry.Builder();

        if (StringUtils.isNotBlank(apiLog.referenceCode)) {
            builder.setServiceLogId(apiLog.referenceCode);
        }
        if (StringUtils.isNotBlank(apiLog.type)) {
            builder.setLogType(LogType.getByType(apiLog.type));
        }
        if (apiLog.owner != null) {
            builder.setAuthor(StringUtils.defaultString(apiLog.owner.username));
        }
        builder.setLog(StringUtils.defaultString(apiLog.text));

        final Date logDate = parseDate(apiLog.loggedDate);
        if (logDate != null) {
            builder.setDate(logDate.getTime());
        }

        return builder.build();
    }

    // ---- Spoiler / Gallery Image Fetching ----

    /** Fetches spoiler/gallery images for a geocache via the Live API and saves them on the cache. */
    @WorkerThread
    public static void fetchAndSaveSpoilers(@NonNull final Geocache cache) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                final HttpResponse response = apiReq()
                        .uri("/geocaches/" + cache.getGeocode() + "/images")
                        .uriParams("fields", "url,name,description")
                        .request()
                        .blockingGet();

                final int status = response.getStatusCode();
                if (status == 429) {
                    waitForRateLimit(response);
                    continue;
                }
                if (status >= 500) {
                    TimeUnit.MILLISECONDS.sleep(SERVER_ERROR_DELAY_MS);
                    continue;
                }
                if (status != 200) {
                    Log.w("GCLiveAPI: fetchImages " + cache.getGeocode() + " HTTP " + status);
                    return;
                }

                final LiveApiImage[] apiImages = response.parseJson(LiveApiImage[].class, null);
                if (apiImages == null || apiImages.length == 0) {
                    return;
                }

                final List<Image> spoilers = new ArrayList<>(apiImages.length);
                for (final LiveApiImage apiImg : apiImages) {
                    if (StringUtils.isNotBlank(apiImg.url)) {
                        spoilers.add(new Image.Builder()
                                .setUrl(apiImg.url)
                                .setTitle(StringUtils.defaultString(apiImg.name))
                                .setDescription(apiImg.description)
                                .build());
                    }
                }

                cache.setSpoilers(spoilers);
                DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                Log.i("GCLiveAPI: Saved " + spoilers.size() + " spoiler images for " + cache.getGeocode());
                return;
            } catch (final Exception e) {
                Log.w("GCLiveAPI: fetchImages attempt " + attempt + " failed for " + cache.getGeocode(), e);
            }
        }
    }

    @WorkerThread
    @Nullable
    private static LiveApiGeocache fetchGeocacheWithRetry(final String geocode) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                final HttpResponse response = apiReq()
                        .uri("/geocaches/" + geocode)
                        .uriParams("lite", "false", "fields", FIELDS_DETAIL)
                        .request()
                        .blockingGet();

                final int status = response.getStatusCode();
                if (status == 200) {
                    final LiveApiGeocache result = response.parseJson(LiveApiGeocache.class, null);
                    if (result == null) {
                        Log.w("GCLiveAPI: parseJson returned null for " + geocode);
                    }
                    return result;
                }
                Log.w("GCLiveAPI: fetch " + geocode + " HTTP " + status);
                if (status == 429) {
                    waitForRateLimit(response);
                    continue;
                }
                if (status >= 500) {
                    TimeUnit.MILLISECONDS.sleep(SERVER_ERROR_DELAY_MS);
                    continue;
                }
                Log.w("GCLiveAPI: fetch " + geocode + " returned HTTP " + status);
                return null;
            } catch (final Exception e) {
                Log.w("GCLiveAPI: fetch " + geocode + " attempt " + attempt + " failed", e);
            }
        }
        return null;
    }

    // ---- Batch Fetch ----

    /**
     * Fetches multiple geocaches in chunks of {@link #BATCH_SIZE}.
     * Returns list of mapped Geocache objects, already saved to DB.
     */
    @WorkerThread
    @NonNull
    public static List<Geocache> fetchGeocachesBatch(@NonNull final List<String> geocodes) {
        final List<Geocache> result = new ArrayList<>();
        final int total = geocodes.size();
        final BatchProgressCallback progressCallback = batchProgressCallback;

        if (progressCallback != null) {
            progressCallback.onProgress(0, total);
        }

        for (int offset = 0; offset < total; offset += BATCH_SIZE) {
            final int end = Math.min(offset + BATCH_SIZE, total);
            final List<String> chunk = geocodes.subList(offset, end);
            final String codes = StringUtils.join(chunk, ',');

            final LiveApiGeocache[] apiCaches = fetchBatchWithRetry(codes);
            if (apiCaches != null) {
                final List<Geocache> mapped = new ArrayList<>(apiCaches.length);
                for (final LiveApiGeocache apiCache : apiCaches) {
                    mapped.add(mapToGeocache(apiCache));
                }
                DataStore.saveCaches(mapped, EnumSet.of(SaveFlag.DB));
                result.addAll(mapped);
            }

            if (progressCallback != null) {
                progressCallback.onProgress(Math.min(end, total), total);
            }
        }

        return result;
    }

    @WorkerThread
    @Nullable
    private static LiveApiGeocache[] fetchBatchWithRetry(final String referenceCodes) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                final HttpResponse response = apiReq()
                        .uri("/geocaches")
                        .uriParams("referenceCodes", referenceCodes,
                                "fields", FIELDS_DETAIL)
                        .request()
                        .blockingGet();

                final int status = response.getStatusCode();
                if (status == 200) {
                    return response.parseJson(LiveApiGeocache[].class, null);
                }
                if (status == 429) {
                    waitForRateLimit(response);
                    continue;
                }
                if (status >= 500) {
                    TimeUnit.MILLISECONDS.sleep(SERVER_ERROR_DELAY_MS);
                    continue;
                }
                Log.w("GCLiveAPI: batch fetch returned HTTP " + status);
                return null;
            } catch (final Exception e) {
                Log.w("GCLiveAPI: batch fetch attempt " + attempt + " failed", e);
            }
        }
        return null;
    }

    // ---- Rate Limit Handling ----

    private static void waitForRateLimit(final HttpResponse response) {
        int waitSeconds = 1;
        final String resetHeader = response.getResponse() != null
                ? response.getResponse().header("x-rate-limit-reset") : null;
        if (StringUtils.isNotBlank(resetHeader)) {
            try {
                waitSeconds = Integer.parseInt(resetHeader);
            } catch (final NumberFormatException ignored) {
                // use default
            }
        }
        Log.i("GCLiveAPI: Rate limited, waiting " + waitSeconds + "s");
        final RateLimitCallback callback = rateLimitCallback;
        try {
            for (int remaining = waitSeconds; remaining > 0; remaining--) {
                if (callback != null) {
                    callback.onRateLimited(remaining);
                }
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (callback != null) {
            callback.onRateLimited(0);
        }
    }

    // ---- JSON to Geocache Mapper ----

    @NonNull
    static Geocache mapToGeocache(@NonNull final LiveApiGeocache api) {
        final Geocache cache = new Geocache();
        cache.setGeocode(api.referenceCode);
        cache.setName(api.name);
        cache.setType(api.geocacheType != null
                ? CacheType.getByWaypointType(String.valueOf(api.geocacheType.id))
                : CacheType.UNKNOWN);
        cache.setSize(api.geocacheSize != null
                ? CacheSize.getByGcId(api.geocacheSize.id)
                : CacheSize.UNKNOWN);
        cache.setDifficulty(api.difficulty);
        cache.setTerrain(api.terrain);
        cache.setFavoritePoints(api.favoritePoints);
        cache.setInventoryItems(api.trackableCount);
        cache.setPremiumMembersOnly(api.isPremiumOnly);
        cache.setHidden(parseDate(api.placedDate));
        cache.setLastFound(parseDate(api.lastVisitedDate));

        // Owner
        if (api.owner != null) {
            cache.setOwnerDisplayName(api.owner.username);
            cache.setOwnerUserId(api.owner.username);
        } else if (StringUtils.isNotBlank(api.ownerAlias)) {
            cache.setOwnerDisplayName(api.ownerAlias);
            cache.setOwnerUserId(api.ownerAlias);
        }

        // Status
        if ("Disabled".equalsIgnoreCase(api.status)) {
            cache.setDisabled(true);
            cache.setArchived(false);
        } else if ("Archived".equalsIgnoreCase(api.status)) {
            cache.setDisabled(false);
            cache.setArchived(true);
        } else {
            cache.setDisabled(false);
            cache.setArchived(false);
        }

        // Coordinates
        if (api.userData != null && api.userData.correctedCoordinates != null) {
            cache.setCoords(new Geopoint(api.userData.correctedCoordinates.latitude,
                    api.userData.correctedCoordinates.longitude));
            cache.setUserModifiedCoords(true);
        } else if (api.postedCoordinates != null) {
            cache.setCoords(new Geopoint(api.postedCoordinates.latitude,
                    api.postedCoordinates.longitude));
            cache.setUserModifiedCoords(false);
        }

        // Location
        if (api.location != null) {
            final String loc = StringUtils.isNotBlank(api.location.state)
                    ? api.location.state + ", " + api.location.country
                    : api.location.country;
            cache.setLocation(loc);
        }

        // Text fields
        cache.setShortDescription(StringUtils.defaultString(api.shortDescription));
        cache.setDescription(StringUtils.defaultString(api.longDescription));
        cache.setHint(StringUtils.defaultString(api.hints));

        // User data
        if (api.userData != null) {
            if (StringUtils.isNotBlank(api.userData.foundDate)) {
                cache.setFound(true);
            } else if (StringUtils.isNotBlank(api.userData.dnfDate)) {
                cache.setDNF(true);
            }
            if (StringUtils.isNotBlank(api.userData.note)) {
                cache.setPersonalNote(api.userData.note);
            }
        }

        // Attributes
        if (api.attributes != null) {
            final List<String> attrs = new ArrayList<>(api.attributes.length);
            for (final LiveApiAttribute attr : api.attributes) {
                final CacheAttribute cacheAttribute = CacheAttribute.getById(attr.id);
                if (cacheAttribute != null) {
                    attrs.add(cacheAttribute.getValue(attr.isOn));
                } else {
                    Log.w("GCLiveAPI: unknown cache attribute id from API: " + attr.id);
                }
            }
            cache.setAttributes(attrs);
        }

        // Additional waypoints
        if (api.additionalWaypoints != null) {
            for (final LiveApiWaypoint apiWp : api.additionalWaypoints) {
                final WaypointType wpType = mapWaypointType(apiWp.typeId);
                final Waypoint wp = new Waypoint(
                        StringUtils.defaultString(apiWp.name, "Waypoint"),
                        wpType,
                        false);
                wp.setPrefix(StringUtils.defaultString(apiWp.prefix));
                if (apiWp.coordinates != null) {
                    wp.setCoords(new Geopoint(apiWp.coordinates.latitude,
                            apiWp.coordinates.longitude));
                } else {
                    wp.setOriginalCoordsEmpty(true);
                }
                if (StringUtils.isNotBlank(apiWp.description)) {
                    wp.setNote(apiWp.description);
                }
                cache.addOrChangeWaypoint(wp, false);
            }
        }

        // Log counts — the API gives us findCount directly
        if (api.findCount > 0) {
            final Map<LogType, Integer> logCounts = new EnumMap<>(LogType.class);
            logCounts.put(LogType.FOUND_IT, api.findCount);
            cache.setLogCounts(logCounts);
        }

        cache.setDetailed(true);
        cache.setDetailedUpdatedNow();
        return cache;
    }

    private static final ThreadLocal<SimpleDateFormat> DATE_PARSE_FORMAT = ThreadLocal.withInitial(() -> {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    });

    /** Parses ISO-8601 date strings from the GC Live API (e.g. "2024-12-13T00:00:00" or "2024-12-13T00:00:00.000"). */
    @Nullable
    private static Date parseDate(@Nullable final String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        try {
            // Strip fractional seconds if present
            final String normalized = dateStr.contains(".") ? dateStr.substring(0, dateStr.indexOf('.')) : dateStr;
            return DATE_PARSE_FORMAT.get().parse(normalized);
        } catch (final ParseException e) {
            Log.w("GCLiveAPI: Failed to parse date: " + dateStr);
            return null;
        }
    }

    /**
     * Maps GC Live API waypoint type IDs to cgeo WaypointType.
     * GC API type IDs: 217=Parking, 218=Final, 219=Virtual Stage (Puzzle/Question),
     * 220=Physical Stage, 221=Trailhead, 452=Reference Point
     */
    @NonNull
    private static WaypointType mapWaypointType(final int typeId) {
        switch (typeId) {
            case 217:
                return WaypointType.PARKING;
            case 218:
                return WaypointType.FINAL;
            case 219:
                return WaypointType.PUZZLE;
            case 220:
                return WaypointType.STAGE;
            case 221:
                return WaypointType.TRAILHEAD;
            case 452:
                return WaypointType.WAYPOINT;
            default:
                return WaypointType.WAYPOINT;
        }
    }

    // ---- Jackson Model Classes ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class TokenResponse {
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("refresh_token")
        String refreshToken;
        @JsonProperty("token_type")
        String tokenType;
        @JsonProperty("expires_in")
        long expiresIn;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiGeocache {
        @JsonProperty("referenceCode")
        String referenceCode;
        @JsonProperty("name")
        String name;
        @JsonProperty("difficulty")
        float difficulty;
        @JsonProperty("terrain")
        float terrain;
        @JsonProperty("favoritePoints")
        int favoritePoints;
        @JsonProperty("trackableCount")
        int trackableCount;
        @JsonProperty("placedDate")
        String placedDate;
        @JsonProperty("geocacheType")
        LiveApiType geocacheType;
        @JsonProperty("geocacheSize")
        LiveApiSize geocacheSize;
        @JsonProperty("status")
        String status;
        @JsonProperty("location")
        LiveApiLocation location;
        @JsonProperty("postedCoordinates")
        LiveApiCoordinates postedCoordinates;
        @JsonProperty("lastVisitedDate")
        String lastVisitedDate;
        @JsonProperty("ownerCode")
        String ownerCode;
        @JsonProperty("owner")
        LiveApiOwner owner;
        @JsonProperty("ownerAlias")
        String ownerAlias;
        @JsonProperty("isPremiumOnly")
        boolean isPremiumOnly;
        @JsonProperty("shortDescription")
        String shortDescription;
        @JsonProperty("longDescription")
        String longDescription;
        @JsonProperty("hints")
        String hints;
        @JsonProperty("attributes")
        LiveApiAttribute[] attributes;
        @JsonProperty("additionalWaypoints")
        LiveApiWaypoint[] additionalWaypoints;
        @JsonProperty("findCount")
        int findCount;
        @JsonProperty("hasSolutionChecker")
        boolean hasSolutionChecker;
        @JsonProperty("userData")
        LiveApiUserData userData;
        @JsonProperty("containsHtml")
        boolean containsHtml;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiOwner {
        @JsonProperty("referenceCode")
        String referenceCode;
        @JsonProperty("username")
        String username;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiLocation {
        @JsonProperty("state")
        String state;
        @JsonProperty("country")
        String country;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiCoordinates {
        @JsonProperty("latitude")
        double latitude;
        @JsonProperty("longitude")
        double longitude;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiType {
        @JsonProperty("id")
        int id;
        @JsonProperty("name")
        String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiSize {
        @JsonProperty("id")
        int id;
        @JsonProperty("name")
        String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiAttribute {
        @JsonProperty("id")
        int id;
        @JsonProperty("name")
        String name;
        @JsonProperty("isOn")
        boolean isOn;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiWaypoint {
        @JsonProperty("prefix")
        String prefix;
        @JsonProperty("name")
        String name;
        @JsonProperty("type")
        int typeId;
        @JsonProperty("coordinates")
        LiveApiCoordinates coordinates;
        @JsonProperty("description")
        String description;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiUserData {
        @JsonProperty("foundDate")
        String foundDate;
        @JsonProperty("dnfDate")
        String dnfDate;
        @JsonProperty("correctedCoordinates")
        LiveApiCoordinates correctedCoordinates;
        @JsonProperty("note")
        String note;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiLog {
        @JsonProperty("referenceCode")
        String referenceCode;
        @JsonProperty("text")
        String text;
        @JsonProperty("loggedDate")
        String loggedDate;
        @JsonProperty("type")
        String type;
        @JsonProperty("owner")
        LiveApiOwner owner;
        // images is omitted — the API returns an object (not array) when empty,
        // which causes Jackson type mismatch. Log images are fetched via description images instead.
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class LiveApiImage {
        @JsonProperty("url")
        String url;
        @JsonProperty("name")
        String name;
        @JsonProperty("description")
        String description;
    }
}
