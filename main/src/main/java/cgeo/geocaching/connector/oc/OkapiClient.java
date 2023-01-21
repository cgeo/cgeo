package cgeo.geocaching.connector.oc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.UserInfo;
import cgeo.geocaching.connector.UserInfo.UserInfoStatus;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.oc.OCApiConnector.ApiSupport;
import cgeo.geocaching.connector.oc.OCApiConnector.OAuthLevel;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.DifficultyAndTerrainGeocacheFilter;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.FavoritesGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter;
import cgeo.geocaching.filters.core.LogsCountGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import cgeo.geocaching.filters.core.NumberRangeGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;
import cgeo.geocaching.filters.core.RatingGeocacheFilter;
import cgeo.geocaching.filters.core.SizeGeocacheFilter;
import cgeo.geocaching.filters.core.StatusGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.ReportProblemType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.OAuthTokens;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import static cgeo.geocaching.connector.capability.ILogin.UNKNOWN_FINDS;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Pair;
import static android.util.Base64.DEFAULT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import static java.lang.Boolean.FALSE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Client for the OpenCaching API (Okapi).
 *
 * @see <a href="http://www.opencaching.de/okapi/introduction.html">Okapi overview</a>
 */
final class OkapiClient {

    private static final int SEARCH_LOAD_INITIAL = 200;
    private static final int SEARCH_LOAD_NEXTPAGE = 50;

    private static final String PARAMETER_LOGCOUNT_KEY = "lpc";
    private static final String PARAMETER_LOGCOUNT_VALUE = "all";

    private static final String PARAMETER_LOG_FIELDS_KEY = "log_fields";
    private static final String PARAMETER_LOG_FIELDS_VALUE = "uuid|date|user|type|comment|images|internal_id";

    private static final String SEARCH_CONTEXT_FILTER = "sc_oc_filter";
    private static final String SEARCH_CONTEXT_TOOK_TOTAL = "sc_oc_took_total";

    private static final char SEPARATOR = '|';
    private static final String SEPARATOR_STRING = Character.toString(SEPARATOR);
    private static final SynchronizedDateFormat LOG_DATE_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", TimeZone.getTimeZone("UTC"), Locale.US);
    @SuppressLint("ConstantLocale")
    private static final SynchronizedDateFormat ISO8601DATEFORMAT = new SynchronizedDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

    private static final String CACHE_ATTRNAMES = "attrnames";
    private static final String CACHE_ATTR_ACODES = "attr_acodes";
    private static final String WPT_LOCATION = "location";
    private static final String WPT_DESCRIPTION = "description";
    private static final String WPT_TYPE = "type";
    private static final String WPT_NAME = "name";
    private static final String CACHE_IS_WATCHED = "is_watched";
    private static final String CACHE_IS_RECOMMENDED = "is_recommended";
    private static final String CACHE_WPTS = "alt_wpts";
    private static final String CACHE_STATUS_ARCHIVED = "Archived";
    private static final String CACHE_STATUS_DISABLED = "Temporarily unavailable";
    private static final String CACHE_IS_FOUND = "is_found";
    private static final String CACHE_SIZE_DEPRECATED = "size";
    private static final String CACHE_SIZE2 = "size2";
    private static final String CACHE_VOTES = "rating_votes";
    private static final String CACHE_NOTFOUNDS = "notfounds";
    private static final String CACHE_FOUNDS = "founds";
    private static final String CACHE_WILLATTENDS = "willattends";
    private static final String CACHE_HIDDEN = "date_hidden";
    private static final String CACHE_LATEST_LOGS = "latest_logs";
    private static final String CACHE_IMAGE_URL = "url";
    private static final String CACHE_IMAGE_CAPTION = "caption";
    private static final String CACHE_IMAGES = "images";
    private static final String CACHE_HINT = "hint";
    private static final String CACHE_DESCRIPTION = "description";
    private static final String CACHE_SHORT_DESCRIPTION = "short_description";
    private static final String CACHE_RECOMMENDATIONS = "recommendations";
    private static final String CACHE_RATING = "rating";
    private static final String CACHE_TERRAIN = "terrain";
    private static final String CACHE_DIFFICULTY = "difficulty";
    private static final String CACHE_OWNER = "owner";
    private static final String CACHE_STATUS = "status";
    private static final String CACHE_TYPE = "type";
    private static final String CACHE_LOCATION = "location";
    private static final String CACHE_NAME = "name";
    private static final String CACHE_CODE = "code";
    private static final String CACHE_REQ_PASSWORD = "req_passwd";
    private static final String CACHE_MY_NOTES = "my_notes";
    private static final String CACHE_TRACKABLES_COUNT = "trackables_count";
    private static final String CACHE_TRACKABLES = "trackables";
    private static final String CACHE_USER_PROFILE = "profile_url";
    private static final String CACHE_REGION = "region";
    private static final String CACHE_COUNTRY = "country2";

    private static final String TRK_GEOCODE = "code";
    private static final String TRK_NAME = "name";

    private static final String LOG_UUID = "uuid";
    private static final String LOG_TYPE = "type";
    private static final String LOG_COMMENT = "comment";
    private static final String LOG_DATE = "date";
    private static final String LOG_USER = "user";
    private static final String LOG_IMAGES = "images";
    private static final String LOG_INTERNAL_ID = "internal_id";

    private static final String USER_UUID = "uuid";
    private static final String USER_USERNAME = "username";
    private static final String USER_CACHES_FOUND = "caches_found";
    private static final String USER_INTERNAL_ID = "internal_id";
    private static final String USER_INFO_FIELDS = "username|caches_found";

    private static final String IMAGE_CAPTION = "caption";
    private static final String IMAGE_URL = "url";

    // the several realms of possible fields for cache retrieval:
    // Core: for livemap requests (L3 - only with level 3 auth)
    // Additional: additional fields for full cache (L3 - only for level 3 auth, current - only for connectors with current api)
    private static final String SERVICE_CACHE_CORE_FIELDS = "code|name|location|type|status|difficulty|terrain|size|size2|date_hidden|trackables_count|owner|founds|notfounds|rating|rating_votes|recommendations|region|country2";
    private static final String SERVICE_CACHE_CORE_L3_FIELDS = "is_found|is_recommended";
    private static final String SERVICE_CACHE_CORE_CURRENT_L3_FIELDS = "is_watched";
    private static final String SERVICE_CACHE_ADDITIONAL_FIELDS = "description|hint|images|latest_logs|alt_wpts|attrnames|req_passwd|trackables";
    private static final String SERVICE_CACHE_ADDITIONAL_CURRENT_FIELDS = "gc_code|attribution_note|attr_acodes|willattends|short_description";
    private static final String SERVICE_CACHE_ADDITIONAL_L3_FIELDS = "my_notes";
    private static final String SERVICE_CACHE_ADDITIONAL_CURRENT_L3_FIELDS = "";

    private static final String METHOD_SEARCH_ALL = "services/caches/search/all";
    private static final String METHOD_SEARCH_BBOX = "services/caches/search/bbox";
    private static final String METHOD_SEARCH_NEAREST = "services/caches/search/nearest";
    private static final String METHOD_RETRIEVE_CACHES = "services/caches/geocaches";

    private static final Pattern PATTERN_TIMEZONE = Pattern.compile("([+-][01][0-9]):([03])0");

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_RADIUS = "200";

    private OkapiClient() {
        // utility class
    }

    @Nullable
    @WorkerThread
    public static Geocache getCache(final String geoCode) {
        final IConnector connector = ConnectorFactory.getConnector(geoCode);
        if (!(connector instanceof OCApiConnector)) {
            return null;
        }

        final OCApiConnector ocapiConn = (OCApiConnector) connector;

        final Parameters params = new Parameters("cache_code", geoCode);
        params.add("fields", getFullFields(ocapiConn));
        params.add("attribution_append", "none");
        params.add(PARAMETER_LOGCOUNT_KEY, PARAMETER_LOGCOUNT_VALUE);
        params.add(PARAMETER_LOG_FIELDS_KEY, PARAMETER_LOG_FIELDS_VALUE);

        final JSONResult result = getRequest(ocapiConn, OkapiService.SERVICE_CACHE, params);

        return result.isSuccess ? parseCache(result.data) : null;
    }

    @NonNull
    @WorkerThread
    public static List<Geocache> getCachesAround(@NonNull final Geopoint center, @NonNull final OCApiConnector connector) {
        final String centerString = GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, center) + SEPARATOR + GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, center);
        final Parameters params = new Parameters("search_method", METHOD_SEARCH_NEAREST);
        final Map<String, String> valueMap = new LinkedHashMap<>();
        valueMap.put("center", centerString);
        valueMap.put("limit", getCacheLimit());
        valueMap.put("radius", DEFAULT_RADIUS);

        return requestCaches(connector, params, valueMap, false);
    }

    @NonNull
    @WorkerThread
    public static List<Geocache> getCachesByOwner(@NonNull final String username, @NonNull final OCApiConnector connector) {
        return getCachesByUser(username, connector, "owner_uuid");
    }

    @NonNull
    @WorkerThread
    public static List<Geocache> getCachesByFinder(@NonNull final String username, @NonNull final OCApiConnector connector) {
        return getCachesByUser(username, connector, "found_by");
    }

    @NonNull
    @WorkerThread
    private static List<Geocache> getCachesByUser(@NonNull final String username, @NonNull final OCApiConnector connector, final String userRequestParam) {
        final String uuid = getUserUUID(connector, username);
        if (StringUtils.isEmpty(uuid)) {
            return Collections.emptyList();
        }
        final Parameters params = new Parameters("search_method", METHOD_SEARCH_ALL);
        final Map<String, String> valueMap = new LinkedHashMap<>();
        valueMap.put(userRequestParam, uuid);

        return requestCaches(connector, params, valueMap, false);
    }

    @NonNull
    @WorkerThread
    public static List<Geocache> getCachesNamed(@Nullable final Geopoint center, final String namePart, @NonNull final OCApiConnector connector) {
        final Map<String, String> valueMap = new LinkedHashMap<>();
        final Parameters params;

        // search around current position, if there is a position
        if (center != null) {
            final String centerString = GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, center) + SEPARATOR + GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, center);
            params = new Parameters("search_method", METHOD_SEARCH_NEAREST);
            valueMap.put("center", centerString);
        } else {
            params = new Parameters("search_method", METHOD_SEARCH_ALL);
        }
        valueMap.put("limit", getCacheLimit());

        // full wildcard search, maybe we need to change this after some testing and evaluation
        valueMap.put("name", "*" + namePart + "*");
        return requestCaches(connector, params, valueMap, false);
    }

    @NonNull
    @WorkerThread
    public static SearchResult getCachesByFilter(@NonNull final OCApiConnector connector, @NonNull final GeocacheFilter filter) {
        return retrieveCaches(connector, filter, SEARCH_LOAD_INITIAL, 0);
    }

    @NonNull
    @WorkerThread
    public static SearchResult getCachesByNextPage(@NonNull final OCApiConnector connector, @NonNull final Bundle context) {

        final String filterConfig = context.getString(SEARCH_CONTEXT_FILTER);
        if (filterConfig == null) {
            return new SearchResult();
        }
        final GeocacheFilter filter = GeocacheFilter.createFromConfig(filterConfig);
        final OriginGeocacheFilter origin = GeocacheFilter.findInChain(filter.getAndChainIfPossible(), OriginGeocacheFilter.class);
        if (origin != null && !origin.allowsCachesOf(connector)) {
            return new SearchResult();
        }
        final int alreadyTook = context.getInt(SEARCH_CONTEXT_TOOK_TOTAL, 0);

        return retrieveCaches(connector, filter, SEARCH_LOAD_NEXTPAGE, alreadyTook);
    }

    @NonNull
    @WorkerThread
    private static SearchResult retrieveCaches(@NonNull final OCApiConnector connector, @NonNull final GeocacheFilter filter, final int take, final int skip) {

        final List<BaseGeocacheFilter> filters = filter.getAndChainIfPossible();

        final Geopoint searchCoords;
        final String radius;

        final DistanceGeocacheFilter df = GeocacheFilter.findInChain(filters, DistanceGeocacheFilter.class);
        if (df != null) {
            searchCoords = df.getEffectiveCoordinate();
            radius = df.getMaxRangeValue() == null ? DEFAULT_RADIUS : "" + (df.getMaxRangeValue().intValue() * 1000);
        } else {
            // search around current position by default
            searchCoords = null;
            radius = DEFAULT_RADIUS;
        }

        //fill in the defaults
        final Parameters params = new Parameters("search_method", METHOD_SEARCH_ALL);
        final Map<String, String> valueMap = new LinkedHashMap<>();
        valueMap.put("limit", "" + take);
        valueMap.put("offset", "" + skip);
        fillSearchParameterCenter(valueMap, params, searchCoords, radius);

        String finder = null;

        for (BaseGeocacheFilter baseFilter : filter.getAndChainIfPossible()) {
            if (baseFilter instanceof OriginGeocacheFilter && !((OriginGeocacheFilter) baseFilter).allowsCachesOf(connector)) {
                return new SearchResult(); //no need to search if connector is filtered out itself
            }
            if (baseFilter instanceof LogEntryGeocacheFilter) {
                finder = ((LogEntryGeocacheFilter) baseFilter).getFoundByUser();
            }
            fillForBasicFilter(baseFilter, params, valueMap, connector);
        }

        //do the search
        final Pair<List<Geocache>, Boolean> rawResult = requestCachesWithMore(connector, params, valueMap, true);
        final SearchResult result = new SearchResult(rawResult.first);

        //store metainfo needed for later for paging
        result.setLeftToFetch(connector, rawResult.second ? 1 : 0);
        result.setToContext(connector, b -> b.putString(SEARCH_CONTEXT_FILTER, filter.toConfig()));
        result.setToContext(connector, b -> b.putInt(SEARCH_CONTEXT_TOOK_TOTAL, skip + rawResult.first.size()));

        if (finder != null) {
            result.setFinder(finder);
        }
        return result;

    }

    private static void fillForBasicFilter(final BaseGeocacheFilter basicFilter, final Parameters params, final Map<String, String> valueMap, @NonNull final OCApiConnector connector) {
        switch (basicFilter.getType()) {
            case TYPE:
                valueMap.put("type", CollectionStream.of(((TypeGeocacheFilter) basicFilter).getRawValues())
                        .map(OkapiClient::getFilterFromType).filter(StringUtils::isNotBlank).toJoinedString("|"));
                break;
            case NAME:
                valueMap.put("name", "*" + ((NameGeocacheFilter) basicFilter).getStringFilter().getTextValue().replace('?', '_') + "*");
                break;
            case SIZE:
                valueMap.put("size2", CollectionStream.of(((SizeGeocacheFilter) basicFilter).getRawValues())
                        .map(CacheSize::getOcSize2).filter(StringUtils::isNotBlank).toJoinedString("|"));
                break;
            case DISTANCE:
                final DistanceGeocacheFilter distanceFilter = (DistanceGeocacheFilter) basicFilter;
                final Geopoint coord = distanceFilter.getEffectiveCoordinate();
                if (distanceFilter.getMaxRangeValue() != null) {
                    fillSearchParameterBox(valueMap, params, new Viewport(coord, distanceFilter.getMaxRangeValue()));
                } else {
                    fillSearchParameterCenter(valueMap, params, coord, DEFAULT_RADIUS);
                }
                break;
            case DIFFICULTY:
            case TERRAIN:
                final NumberRangeGeocacheFilter<Float> nrFilter = (NumberRangeGeocacheFilter<Float>) basicFilter;
                if (nrFilter.isFiltering()) {
                    valueMap.put(nrFilter.getType() == GeocacheFilterType.DIFFICULTY ? "difficulty" : "terrain",
                            (nrFilter.getMinRangeValue() == null ? "1" : ((int) Math.floor(nrFilter.getMinRangeValue()))) + "-" + (nrFilter.getMaxRangeValue() == null ? "5" : Math.round(nrFilter.getMaxRangeValue())));
                }
                break;
            case DIFFICULTY_TERRAIN:
                fillForBasicFilter(((DifficultyAndTerrainGeocacheFilter) basicFilter).difficultyGeocacheFilter, params, valueMap, connector);
                fillForBasicFilter(((DifficultyAndTerrainGeocacheFilter) basicFilter).terrainGeocacheFilter, params, valueMap, connector);
                break;
            case RATING:
                final RatingGeocacheFilter ratingFilter = (RatingGeocacheFilter) basicFilter;
                if (ratingFilter.getMinRangeValue() != null) {
                    valueMap.put("rating", (ratingFilter.getMinRangeValue() == null ? "1" : ((int) Math.floor(ratingFilter.getMinRangeValue()))) + "-" + (ratingFilter.getMaxRangeValue() == null ? "5" : Math.round(ratingFilter.getMaxRangeValue())));
                }
                break;
            case OWNER:
                final String uuid = getUserUUID(connector, ((OwnerGeocacheFilter) basicFilter).getStringFilter().getTextValue());
                // If uuid ==null then user is not known on oc platform.
                // In that case, set a nonexisting uuid so the search will return no result
                valueMap.put("owner_uuid", uuid == null ? "unknown-user" : uuid);
                break;
            case FAVORITES:
                final FavoritesGeocacheFilter favFilter = (FavoritesGeocacheFilter) basicFilter;
                if (favFilter.getMinRangeValue() != null) {
                    valueMap.put("min_rcmds", ((int) Math.round(favFilter.getMinRangeValue())) + (favFilter.isPercentage() ? "%" : ""));
                }
                break;
            case STATUS:
                final StatusGeocacheFilter statusFilter = (StatusGeocacheFilter) basicFilter;
                String value = "";
                if (!statusFilter.isExcludeActive()) {
                    value += "|Available";
                }
                if (!statusFilter.isExcludeDisabled()) {
                    value += "|Temporarily unavailable";
                }
                //DON'T return archived caches even if the filter says so. See #11208 for explanation
                //Note: if at a later point in time Archived shall be included, just add following codeline:
                //   if (!statusFilter.isExcludeArchived()) {
                //       value += "|Archived";
                //   }
                if (!value.isEmpty()) {
                    valueMap.put("status", value.substring(1));
                }

                if (statusFilter.getStatusFound() != null) {
                    valueMap.put("found_status", statusFilter.getStatusFound() ? "found_only" : "notfound_only");
                }
                if (FALSE.equals(statusFilter.getStatusOwned())) {
                    valueMap.put("exclude_my_own", "true");
                }
                break;
            case LOGS_COUNT:
                final LogsCountGeocacheFilter logsCountFilter = (LogsCountGeocacheFilter) basicFilter;
                if (logsCountFilter.getLogType().equals(LogType.FOUND_IT)) {
                    if (logsCountFilter.getMinRangeValue() != null) {
                        valueMap.put("min_founds", "" + logsCountFilter.getMinRangeValue());
                    }
                    if (logsCountFilter.getMaxRangeValue() != null) {
                        valueMap.put("max_founds", "" + logsCountFilter.getMaxRangeValue());
                    }
                }
                break;
            case LOG_ENTRY:
                final LogEntryGeocacheFilter logEntryFilter = (LogEntryGeocacheFilter) basicFilter;
                if (StringUtils.isNotBlank(logEntryFilter.getFoundByUser())) {
                    String uuid2 = getUserUUID(connector, logEntryFilter.getFoundByUser());
                    if (uuid2 == null) {
                        uuid2 = "unknown-user"; //set a nonexisting uuid so the search will return nothing
                    }
                    if (logEntryFilter.isInverse()) {
                        valueMap.put("not_found_by", uuid2);
                    } else {
                        valueMap.put("found_by", uuid2);
                    }
                }
                break;
            default:
                break;
        }

    }

    public static void fillSearchParameterCenter(@NonNull final Map<String, String> valueMap, @NonNull final Parameters params, @Nullable final Geopoint center, final String radius) {
        final Geopoint usedCenter = center != null ? center : LocationDataProvider.getInstance().currentGeo().getCoords();
        final String centerString = GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, usedCenter) + SEPARATOR + GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, usedCenter);
        valueMap.put("center", centerString);
        valueMap.put("radius", radius);
        params.removeKey("search_method");
        params.put("search_method", METHOD_SEARCH_NEAREST);
    }

    public static void fillSearchParameterBox(@NonNull final Map<String, String> valueMap, @NonNull final Parameters params, @Nullable final Viewport viewport) {

        final String bboxString = GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, viewport.bottomLeft)
                + SEPARATOR + GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, viewport.bottomLeft)
                + SEPARATOR + GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, viewport.topRight)
                + SEPARATOR + GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, viewport.topRight);
        valueMap.put("bbox", bboxString);

        fillSearchParameterCenter(valueMap, params, viewport.getCenter(), DEFAULT_RADIUS);
        params.removeKey("search_method");
        params.put("search_method", METHOD_SEARCH_BBOX);
    }


    /**
     * pass 'null' as value for 'my' to exclude the legacy global application of own/filtered/disabled/archived-flags
     */
    @NonNull
    @WorkerThread
    private static List<Geocache> requestCaches(@NonNull final OCApiConnector connector, @NonNull final Parameters params, @NonNull final Map<String, String> valueMap, final boolean forFilterSearch) {
        return requestCachesWithMore(connector, params, valueMap, forFilterSearch).first;
    }

    @NonNull
    @WorkerThread
    private static Pair<List<Geocache>, Boolean> requestCachesWithMore(@NonNull final OCApiConnector connector, @NonNull final Parameters params, @NonNull final Map<String, String> valueMap, final boolean forFilterSearch) {

        if (!forFilterSearch) {
            addFilterParams(valueMap);
        }
        // OKAPI returns ignored caches, we have to actively suppress them
        if (connector.getSupportedAuthLevel() == OAuthLevel.Level3) {
            valueMap.put("ignored_status", "notignored_only");
        }

        try {
            params.add("search_params", JsonUtils.writer.writeValueAsString(valueMap));
        } catch (final JsonProcessingException e) {
            Log.e("requestCaches", e);
            return new Pair(Collections.emptyList(), false);
        }
        addRetrieveParams(params, connector);

        final ObjectNode data = getRequest(connector, OkapiService.SERVICE_SEARCH_AND_RETRIEVE, params).data;

        if (data == null) {
            return new Pair(Collections.emptyList(), false);
        }

        return parseCaches(data);
    }

    /**
     * Assumes level 3 OAuth.
     */
    @NonNull
    @WorkerThread
    public static List<Geocache> getCachesBBox(final Viewport viewport, @NonNull final OCApiConnector connector) {

        if (viewport.getLatitudeSpan() == 0 || viewport.getLongitudeSpan() == 0) {
            return Collections.emptyList();
        }

        final String bboxString = GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, viewport.bottomLeft)
                + SEPARATOR + GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, viewport.bottomLeft)
                + SEPARATOR + GeopointFormatter.format(GeopointFormatter.Format.LAT_DECDEGREE_RAW, viewport.topRight)
                + SEPARATOR + GeopointFormatter.format(GeopointFormatter.Format.LON_DECDEGREE_RAW, viewport.topRight);
        final Parameters params = new Parameters("search_method", METHOD_SEARCH_BBOX);
        final Map<String, String> valueMap = new LinkedHashMap<>();
        valueMap.put("bbox", bboxString);

        return requestCaches(connector, params, valueMap, false);
    }

    @WorkerThread
    public static boolean setWatchState(@NonNull final Geocache cache, final boolean watched, @NonNull final OCApiConnector connector) {
        final Parameters params = new Parameters("cache_code", cache.getGeocode());
        params.add("watched", watched ? "true" : "false");

        final ObjectNode data = getRequest(connector, OkapiService.SERVICE_MARK_CACHE, params).data;

        if (data == null) {
            return false;
        }

        cache.setOnWatchlist(watched);

        return true;
    }

    @WorkerThread
    public static boolean setIgnored(@NonNull final Geocache cache, @NonNull final OCApiConnector connector) {
        final Parameters params = new Parameters("cache_code", cache.getGeocode());
        params.add("ignored", "true");

        final ObjectNode data = getRequest(connector, OkapiService.SERVICE_MARK_CACHE, params).data;

        return data != null;
    }

    @NonNull
    @WorkerThread
    public static LogResult postLog(@NonNull final Geocache cache, @NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log, @Nullable final String logPassword, @NonNull final OCApiConnector connector, @NonNull final ReportProblemType reportProblem) {
        final Parameters params = new Parameters("cache_code", cache.getGeocode());
        params.add("logtype", logType.ocType);
        params.add("comment", log);
        params.add("comment_format", "plaintext");
        params.add("when", LOG_DATE_FORMAT.format(date.getTime()));
        if (logType == LogType.NEEDS_MAINTENANCE) {
            params.add("needs_maintenance", "true");
        }
        if (logPassword != null) {
            params.add("password", logPassword);
        }
        if (reportProblem == ReportProblemType.NEEDS_MAINTENANCE) { // OKAPI only knows this one problem type
            params.add("needs_maintenance2", "true");
        }

        final ObjectNode data = getRequest(connector, OkapiService.SERVICE_SUBMIT_LOG, params).data;

        if (data == null) {
            return new LogResult(StatusCode.LOG_POST_ERROR, "");
        }

        try {
            if (data.get("success").asBoolean()) {
                //unfortunately we only have the uuid here (not the internal id)
                return new LogResult(StatusCode.NO_ERROR, data.get("log_uuid").asText());
            }

            return new LogResult(StatusCode.LOG_POST_ERROR, "");
        } catch (final NullPointerException e) {
            Log.e("OkapiClient.postLog", e);
        }
        return new LogResult(StatusCode.LOG_POST_ERROR, "");
    }

    @NonNull
    @WorkerThread
    public static ImageResult postLogImage(final String logId, final Image image, @NonNull final OCApiConnector connector) {
        final Parameters params = new Parameters("log_uuid", logId);
        final File file = image.getFile();
        if (file == null) {
            return new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, "");
        }
        FileInputStream fileStream = null;
        try {
            fileStream = new FileInputStream(file);
            params.add("image", Base64.encodeToString(IOUtils.readFully(fileStream, (int) file.length()), DEFAULT));
            params.add("caption", createImageCaption(image));

            final ObjectNode data = postRequest(connector, OkapiService.SERVICE_ADD_LOG_IMAGE, params).data;

            if (data == null) {
                return new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, "");
            }

            if (data.get("success").asBoolean()) {
                return new ImageResult(StatusCode.NO_ERROR, data.get("image_url").asText());
            }

            return new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, "");
        } catch (final Exception e) {
            Log.e("OkapiClient.postLogImage", e);
        } finally {
            IOUtils.closeQuietly(fileStream);
        }
        return new ImageResult(StatusCode.LOGIMAGE_POST_ERROR, "");
    }

    @NonNull
    private static String createImageCaption(final Image image) {
        final StringBuilder caption = new StringBuilder(StringUtils.trimToEmpty(image.getTitle()));
        if (StringUtils.isNotEmpty(caption) && StringUtils.isNotBlank(image.getDescription())) {
            caption.append(": ");
        }
        caption.append(StringUtils.trimToEmpty(image.getDescription()));
        return caption.toString();
    }

    @WorkerThread
    public static boolean uploadPersonalNotes(@NonNull final OCApiConnector connector, @NonNull final Geocache cache) {
        Log.d("Uploading personal note for opencaching");

        final Parameters notesParam = new Parameters("cache_code", cache.getGeocode(), "fields", CACHE_MY_NOTES);
        final ObjectNode notesData = getRequest(connector, OkapiService.SERVICE_CACHE, notesParam).data;

        String prevNote = StringUtils.EMPTY;

        if (notesData != null && notesData.get(CACHE_MY_NOTES) != null) {
            prevNote = notesData.get(CACHE_MY_NOTES).asText();
        }

        final String currentNote = StringUtils.defaultString(cache.getPersonalNote());

        final Parameters params = new Parameters("cache_code", cache.getGeocode(), "new_value", currentNote, "old_value", prevNote);
        final ObjectNode data = getRequest(connector, OkapiService.SERVICE_UPLOAD_PERSONAL_NOTE, params).data;

        if (data == null) {
            return false;
        }

        if (data.get("replaced") != null && data.get("replaced").asBoolean()) {
            Log.d("Successfully uploaded");
            return true;
        }
        return false;
    }

    /**
     * returns list of parsed geocaches (left) and a floag indicating whether there are more results on serer (right)
     */
    @NonNull
    private static Pair<List<Geocache>, Boolean> parseCaches(final ObjectNode response) {
        try {
            final JsonNode moreNode = response.path("more");
            final boolean more = moreNode != null && moreNode.isBoolean() && moreNode.asBoolean();

            // Check for empty result
            final JsonNode results = response.path("results");
            if (!results.isObject()) {
                return new Pair(Collections.emptyList(), more);
            }

            // Get and iterate result list
            final List<Geocache> caches = new ArrayList<>(results.size());
            for (final JsonNode cache : results) {
                caches.add(parseSmallCache((ObjectNode) cache));
            }
            return new Pair(caches, more);
        } catch (ClassCastException | NullPointerException e) {
            Log.e("OkapiClient.parseCachesResult", e);
        }
        return new Pair(Collections.emptyList(), false);
    }

    @NonNull
    private static Geocache parseSmallCache(final ObjectNode response) {
        final Geocache cache = new Geocache();
        try {
            parseCoreCache(response, cache);
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.CACHE));
        } catch (final NullPointerException e) {
            // FIXME: here we may return a partially filled cache
            Log.e("OkapiClient.parseSmallCache", e);
        }
        return cache;
    }


    @NonNull
    private static Geocache parseCache(final ObjectNode response) {
        final Geocache cache = new Geocache();
        try {

            parseCoreCache(response, cache);

            // not used: url
            // not used: req_password
            // Prepend gc-link to description if available
            final StringBuilder description = new StringBuilder(500);
            if (response.hasNonNull("gc_code")) {
                final String gccode = response.get("gc_code").asText();
                description.append(Geocache.getAlternativeListingText(gccode));
            }
            description.append(response.get(CACHE_DESCRIPTION).asText());
            cache.setDescription(description.toString());

            if (response.has(CACHE_SHORT_DESCRIPTION)) {
                final String shortDescription = StringUtils.trim(response.get(CACHE_SHORT_DESCRIPTION).asText());
                if (StringUtils.isNotEmpty(shortDescription)) {
                    cache.setShortDescription(shortDescription);
                }
            }

            // currently the hint is delivered as HTML (contrary to OKAPI documentation), so we can store it directly
            cache.setHint(response.get(CACHE_HINT).asText());
            // not used: hints

            //set images
            final List<Image> cacheImages = new ArrayList<>();
            final ArrayNode images = (ArrayNode) response.get(CACHE_IMAGES);
            if (images != null) {
                for (final JsonNode imageResponse : images) {
                    final String title = imageResponse.get(CACHE_IMAGE_CAPTION).asText();
                    final String url = absoluteUrl(imageResponse.get(CACHE_IMAGE_URL).asText(), cache.getGeocode());
                    cacheImages.add(new Image.Builder().setUrl(url).setTitle(title).build());
                }
            }
            // all images are added as spoiler images, although OKAPI has spoiler and non spoiler images
            cache.setSpoilers(cacheImages);

            cache.setAttributes(parseAttributes((ArrayNode) response.path(CACHE_ATTRNAMES), (ArrayNode) response.get(CACHE_ATTR_ACODES)));
            //TODO: Store license per cache
            //cache.setLicense(response.getString("attribution_note"));
            cache.setWaypoints(parseWaypoints((ArrayNode) response.path(CACHE_WPTS)), false);

            cache.mergeInventory(parseTrackables((ArrayNode) response.path(CACHE_TRACKABLES)), EnumSet.of(TrackableBrand.GEOKRETY));

            if (response.has(CACHE_IS_WATCHED)) {
                cache.setOnWatchlist(response.get(CACHE_IS_WATCHED).asBoolean());
            }
            if (response.hasNonNull(CACHE_MY_NOTES)) {
                cache.setPersonalNote(response.get(CACHE_MY_NOTES).asText(), true);
            }
            cache.setLogPasswordRequired(response.get(CACHE_REQ_PASSWORD).asBoolean());

            cache.setDetailedUpdatedNow();
            // save full detailed caches
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
            DataStore.saveLogs(cache.getGeocode(), parseLogs((ArrayNode) response.path(CACHE_LATEST_LOGS), cache.getGeocode()), true);
        } catch (ClassCastException | NullPointerException e) {
            Log.e("OkapiClient.parseCache", e);
        }
        return cache;
    }

    private static void parseCoreCache(final ObjectNode response, @NonNull final Geocache cache) {
        cache.setGeocode(response.get(CACHE_CODE).asText());
        cache.setName(response.get(CACHE_NAME).asText());
        // not used: names
        setLocation(cache, response.get(CACHE_LOCATION).asText());
        cache.setType(getCacheType(response.get(CACHE_TYPE).asText()));

        final String status = response.get(CACHE_STATUS).asText();
        cache.setDisabled(status.equalsIgnoreCase(CACHE_STATUS_DISABLED));
        cache.setArchived(status.equalsIgnoreCase(CACHE_STATUS_ARCHIVED));

        cache.setSize(getCacheSize(response));
        cache.setDifficulty((float) response.get(CACHE_DIFFICULTY).asDouble());
        cache.setTerrain((float) response.get(CACHE_TERRAIN).asDouble());

        cache.setInventoryItems(response.get(CACHE_TRACKABLES_COUNT).asInt());

        final String region = response.get(CACHE_REGION) == null ? null : response.get(CACHE_REGION).asText();
        final String country = response.get(CACHE_COUNTRY) == null ? null : response.get(CACHE_COUNTRY).asText();
        cache.setLocation(region == null ? country : (country == null ? region : region + ", " + country));

        if (response.has(CACHE_IS_FOUND)) {
            cache.setFound(response.get(CACHE_IS_FOUND).asBoolean());
        }
        if (response.has(CACHE_IS_WATCHED)) {
            cache.setOnWatchlist(response.get(CACHE_IS_WATCHED).asBoolean());
        }
        if (response.has(CACHE_IS_RECOMMENDED)) {
            cache.setFavorite(response.get(CACHE_IS_RECOMMENDED).asBoolean());
        }
        cache.setHidden(parseDate(response.get(CACHE_HIDDEN).asText()));

        final String owner = parseUser(response.get(CACHE_OWNER));
        cache.setOwnerDisplayName(owner);
        // OpenCaching has no distinction between user id and user display name. Set the ID anyway to simplify c:geo workflows.
        cache.setOwnerUserId(owner);
        final String profile = response.get(CACHE_OWNER).get(CACHE_USER_PROFILE).asText();
        if (StringUtils.isNotEmpty(profile)) {
            final String id = StringUtils.substringAfter(profile, "userid=");
            if (StringUtils.isNotEmpty(id)) {
                cache.setOwnerUserId(id);
            }
        }

        final Map<LogType, Integer> logCounts = cache.getLogCounts();
        logCounts.put(LogType.FOUND_IT, response.get(CACHE_FOUNDS).asInt());
        logCounts.put(LogType.DIDNT_FIND_IT, response.get(CACHE_NOTFOUNDS).asInt());
        // only current Api
        logCounts.put(LogType.WILL_ATTEND, response.path(CACHE_WILLATTENDS).asInt());
        cache.setLogCounts(logCounts);

        if (response.has(CACHE_RATING)) {
            cache.setRating((float) response.get(CACHE_RATING).asDouble());
        }
        cache.setVotes(response.get(CACHE_VOTES).asInt());

        cache.setFavoritePoints(response.get(CACHE_RECOMMENDATIONS).asInt());

        //set basic properties which are constant / not used for OC platforms
        cache.setPremiumMembersOnly(false);
        cache.setUserModifiedCoords(false);

        DataStore.saveCache(cache, EnumSet.of(SaveFlag.CACHE));
    }

    private static String absoluteUrl(final String url, final String geocode) {
        final Uri uri = Uri.parse(url);

        if (!uri.isAbsolute()) {
            final IConnector connector = ConnectorFactory.getConnector(geocode);
            final String hostUrl = connector.getHostUrl();
            if (StringUtils.isNotBlank(hostUrl)) {
                return hostUrl + "/" + url;
            }
        }
        return url;
    }

    private static String parseUser(final JsonNode user) {
        return user.get(USER_USERNAME).asText();
    }

    @NonNull
    private static List<LogEntry> parseLogs(final ArrayNode logsJSON, final String geocode) {
        final List<LogEntry> result = new LinkedList<>();
        for (final JsonNode logResponse : logsJSON) {
            try {
                final Date date = parseDate(logResponse.get(LOG_DATE).asText());
                if (date == null) {
                    continue;
                }
                final LogEntry log = new LogEntry.Builder()
                        .setServiceLogId(logResponse.get(LOG_UUID).asText().trim() + ":" + logResponse.get(LOG_INTERNAL_ID).asText().trim())
                        .setAuthor(parseUser(logResponse.get(LOG_USER)))
                        .setDate(date.getTime())
                        .setLogType(parseLogType(logResponse.get(LOG_TYPE).asText()))
                        .setLogImages(parseLogImages((ArrayNode) logResponse.path(LOG_IMAGES), geocode))
                        .setLog(logResponse.get(LOG_COMMENT).asText().trim()).build();
                result.add(log);
            } catch (final NullPointerException e) {
                Log.e("OkapiClient.parseLogs", e);
            }
        }
        return result;
    }

    private static List<Image> parseLogImages(final ArrayNode imagesNode, final String geocode) {
        final List<Image> images = new ArrayList<>();
        for (final JsonNode image : imagesNode) {
            images.add(new Image.Builder().setUrl(absoluteUrl(image.get(IMAGE_URL).asText(), geocode)).setTitle(image.get(IMAGE_CAPTION).asText()).build());
        }
        return images;
    }

    @Nullable
    private static List<Waypoint> parseWaypoints(final ArrayNode wptsJson) {
        List<Waypoint> result = null;
        final Geopoint pt0 = new Geopoint(0, 0);
        for (final JsonNode wptResponse : wptsJson) {
            try {
                final Waypoint wpt = new Waypoint(wptResponse.get(WPT_NAME).asText(),
                        parseWptType(wptResponse.get(WPT_TYPE).asText()),
                        false);
                wpt.setNote(wptResponse.get(WPT_DESCRIPTION).asText());
                final Geopoint pt = parseCoords(wptResponse.get(WPT_LOCATION).asText());
                if (pt != null && !pt.equals(pt0)) {
                    wpt.setCoords(pt);
                } else {
                    wpt.setOriginalCoordsEmpty(true);
                }
                if (result == null) {
                    result = new ArrayList<>();
                }
                wpt.setPrefix(wpt.getName());
                result.add(wpt);
            } catch (final NullPointerException e) {
                Log.e("OkapiClient.parseWaypoints", e);
            }
        }
        return result;
    }

    @NonNull
    private static List<Trackable> parseTrackables(final ArrayNode trackablesJson) {
        if (trackablesJson.size() == 0) {
            return Collections.emptyList();
        }
        final List<Trackable> result = new ArrayList<>();
        for (final JsonNode trackableResponse : trackablesJson) {
            try {
                final Trackable trk = new Trackable();
                trk.setGeocode(trackableResponse.get(TRK_GEOCODE).asText());
                trk.setName(trackableResponse.get(TRK_NAME).asText());
                result.add(trk);
            } catch (final NullPointerException e) {
                Log.e("OkapiClient.parseWaypoints", e);
            }
        }
        return result;
    }

    @NonNull
    private static LogType parseLogType(@Nullable final String logType) {
        if ("Found it".equalsIgnoreCase(logType)) {
            return LogType.FOUND_IT;
        }
        if ("Didn't find it".equalsIgnoreCase(logType)) {
            return LogType.DIDNT_FIND_IT;
        }
        if ("Will attend".equalsIgnoreCase(logType)) {
            return LogType.WILL_ATTEND;
        }
        if ("Attended".equalsIgnoreCase(logType)) {
            return LogType.ATTENDED;
        }
        if ("Temporarily unavailable".equalsIgnoreCase(logType)) {
            return LogType.TEMP_DISABLE_LISTING;
        }
        if ("Ready to search".equalsIgnoreCase(logType)) {
            return LogType.ENABLE_LISTING;
        }
        if ("Archived".equalsIgnoreCase(logType)) {
            return LogType.ARCHIVE;
        }
        if ("Locked".equalsIgnoreCase(logType)) {
            return LogType.ARCHIVE;
        }
        if ("Needs maintenance".equalsIgnoreCase(logType)) {
            return LogType.NEEDS_MAINTENANCE;
        }
        if ("Maintenance performed".equalsIgnoreCase(logType)) {
            return LogType.OWNER_MAINTENANCE;
        }
        if ("Moved".equalsIgnoreCase(logType)) {
            return LogType.UPDATE_COORDINATES;
        }
        if ("OC Team comment".equalsIgnoreCase(logType)) {
            return LogType.POST_REVIEWER_NOTE;
        }
        return LogType.NOTE;
    }

    @NonNull
    private static WaypointType parseWptType(@Nullable final String wptType) {
        if ("parking".equalsIgnoreCase(wptType)) {
            return WaypointType.PARKING;
        }
        if ("path".equalsIgnoreCase(wptType)) {
            return WaypointType.TRAILHEAD;
        }
        if ("stage".equalsIgnoreCase(wptType)) {
            return WaypointType.STAGE;
        }
        if ("physical-stage".equalsIgnoreCase(wptType)) {
            return WaypointType.STAGE;
        }
        if ("virtual-stage".equalsIgnoreCase(wptType)) {
            return WaypointType.PUZZLE;
        }
        if ("final".equalsIgnoreCase(wptType)) {
            return WaypointType.FINAL;
        }
        if ("poi".equalsIgnoreCase(wptType)) {
            return WaypointType.WAYPOINT;
        }
        if ("trailhead".equalsIgnoreCase(wptType)) {
            return WaypointType.TRAILHEAD;
        }
        return WaypointType.WAYPOINT;
    }

    @Nullable
    private static Date parseDate(final String date) {
        final String strippedDate = PATTERN_TIMEZONE.matcher(date).replaceAll("$1$20");
        try {
            return ISO8601DATEFORMAT.parse(strippedDate);
        } catch (final ParseException e) {
            Log.e("OkapiClient.parseDate", e);
        }
        return null;
    }

    @Nullable
    private static Geopoint parseCoords(final String location) {
        final String latitude = StringUtils.substringBefore(location, SEPARATOR_STRING);
        final String longitude = StringUtils.substringAfter(location, SEPARATOR_STRING);
        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
            return new Geopoint(Double.parseDouble(latitude), Double.parseDouble(longitude));
        }

        return null;
    }

    @NonNull
    private static List<String> parseAttributes(final ArrayNode nameList, final ArrayNode acodeList) {

        final List<String> result = new ArrayList<>();

        for (int i = 0; i < nameList.size(); i++) {
            try {
                final String name = nameList.get(i).asText();
                final int acode = acodeList != null ? Integer.parseInt(acodeList.get(i).asText().substring(1)) : CacheAttribute.NO_ID;
                final CacheAttribute attr = CacheAttribute.getByOcACode(acode);

                if (attr != null) {
                    result.add(attr.rawName);
                } else {
                    result.add(name);
                }
            } catch (final NullPointerException e) {
                Log.e("OkapiClient.parseAttributes", e);
            }
        }

        return result;
    }

    private static void setLocation(@NonNull final Geocache cache, final String location) {
        final String latitude = StringUtils.substringBefore(location, SEPARATOR_STRING);
        final String longitude = StringUtils.substringAfter(location, SEPARATOR_STRING);
        cache.setCoords(new Geopoint(Double.parseDouble(latitude), Double.parseDouble(longitude)));
    }

    @NonNull
    private static CacheSize getCacheSize(final ObjectNode response) {
        if (!response.has(CACHE_SIZE2)) {
            return getCacheSizeDeprecated(response);
        }
        try {
            final String size = response.get(CACHE_SIZE2).asText();
            return CacheSize.getById(size);
        } catch (final NullPointerException e) {
            Log.e("OkapiClient.getCacheSize", e);
            return getCacheSizeDeprecated(response);
        }
    }

    @NonNull
    private static CacheSize getCacheSizeDeprecated(final ObjectNode response) {
        if (!response.has(CACHE_SIZE_DEPRECATED)) {
            return CacheSize.NOT_CHOSEN;
        }
        double size = 0;
        try {
            size = response.get(CACHE_SIZE_DEPRECATED).asDouble();
        } catch (final NullPointerException e) {
            Log.e("OkapiClient.getCacheSize", e);
        }
        switch ((int) Math.round(size)) {
            case 1:
                return CacheSize.MICRO;
            case 2:
                return CacheSize.SMALL;
            case 3:
                return CacheSize.REGULAR;
            case 4:
                return CacheSize.LARGE;
            case 5:
                return CacheSize.VERY_LARGE;
            default:
                break;
        }
        return CacheSize.NOT_CHOSEN;
    }

    @NonNull
    private static CacheType getCacheType(@Nullable final String cacheType) {
        if ("Traditional".equalsIgnoreCase(cacheType)) {
            return CacheType.TRADITIONAL;
        }
        if ("Multi".equalsIgnoreCase(cacheType)) {
            return CacheType.MULTI;
        }
        if ("Quiz".equalsIgnoreCase(cacheType)) {
            return CacheType.MYSTERY;
        }
        if ("Virtual".equalsIgnoreCase(cacheType)) {
            return CacheType.VIRTUAL;
        }
        if ("Event".equalsIgnoreCase(cacheType)) {
            return CacheType.EVENT;
        }
        if ("Webcam".equalsIgnoreCase(cacheType)) {
            return CacheType.WEBCAM;
        }
        if ("Math/Physics".equalsIgnoreCase(cacheType)) {
            return CacheType.MYSTERY;
        }
        if ("Drive-In".equalsIgnoreCase(cacheType)) {
            return CacheType.TRADITIONAL;
        }
        return CacheType.UNKNOWN;
    }

    @NonNull
    private static String getCoreFields(@NonNull final OCApiConnector connector) {
        final StringBuilder res = new StringBuilder(SERVICE_CACHE_CORE_FIELDS);

        if (connector.getSupportedAuthLevel() == OAuthLevel.Level3) {
            res.append(SEPARATOR).append(SERVICE_CACHE_CORE_L3_FIELDS);

            if (connector.getApiSupport() == ApiSupport.current) {
                res.append(SEPARATOR).append(SERVICE_CACHE_CORE_CURRENT_L3_FIELDS);
            }
        }

        return res.toString();
    }

    @NonNull
    private static String getFullFields(@NonNull final OCApiConnector connector) {
        final StringBuilder res = new StringBuilder(500);

        res.append(SERVICE_CACHE_CORE_FIELDS);
        res.append(SEPARATOR).append(SERVICE_CACHE_ADDITIONAL_FIELDS);
        if (connector.getSupportedAuthLevel() == OAuthLevel.Level3) {
            res.append(SEPARATOR).append(SERVICE_CACHE_CORE_L3_FIELDS);
            res.append(SEPARATOR).append(SERVICE_CACHE_ADDITIONAL_L3_FIELDS);
        }
        if (connector.getApiSupport() == ApiSupport.current) {
            res.append(SEPARATOR).append(SERVICE_CACHE_ADDITIONAL_CURRENT_FIELDS);
            if (connector.getSupportedAuthLevel() == OAuthLevel.Level3 && !SERVICE_CACHE_ADDITIONAL_CURRENT_L3_FIELDS.isEmpty()) {
                res.append(SEPARATOR).append(SERVICE_CACHE_ADDITIONAL_CURRENT_L3_FIELDS);
            }
        }

        return res.toString();
    }

    @NonNull
    @WorkerThread
    private static JSONResult request(@NonNull final OCApiConnector connector, @NonNull final OkapiService service, @NonNull final String method, @NonNull final Parameters params) {
        final String host = connector.getHost();
        if (StringUtils.isBlank(host)) {
            return new JSONResult("unknown OKAPI connector host");
        }

        params.add("langpref", getPreferredLanguage());

        switch (connector.getSupportedAuthLevel()) {
            case Level3: {
                final OAuthTokens tokens = new OAuthTokens(connector);
                if (!tokens.isValid()) {
                    return new JSONResult("invalid oauth tokens");
                }
                OAuth.signOAuth(host, service.methodName, method, connector.isHttps(), params, tokens, connector.getCK(), connector.getCS());
                break;
            }
            case Level1: {
                connector.addAuthentication(params);
                break;
            }
            default:
                // do nothing, anonymous access
                break;
        }

        final String uri = connector.getHostUrl() + service.methodName;
        try {
            if ("GET".equals(method)) {
                return new JSONResult(Network.getRequest(uri, params).blockingGet());
            }
            return new JSONResult(Network.postRequest(uri, params).blockingGet());
        } catch (final Exception e) {
            return new JSONResult("connection error");
        }
    }

    @NonNull
    @WorkerThread
    private static JSONResult getRequest(@NonNull final OCApiConnector connector, @NonNull final OkapiService service, @NonNull final Parameters params) {
        return request(connector, service, "GET", params);
    }

    @NonNull
    @WorkerThread
    private static JSONResult postRequest(@NonNull final OCApiConnector connector, @NonNull final OkapiService service, @NonNull final Parameters params) {
        return request(connector, service, "POST", params);
    }

    /**
     * Return a pipe-separated list of preferred languages. English and the device default language (if different) will
     * always be in the list. A user-set language will be first (if set).
     */
    @NonNull
    static String getPreferredLanguage() {
        final String userLanguage = StringUtils.lowerCase(Settings.getApplicationLocale().getLanguage());
        final String defaultLanguage = StringUtils.defaultIfBlank(StringUtils.lowerCase(Locale.getDefault().getLanguage()), "en");
        return userLanguage + (userLanguage.equals(defaultLanguage) ? "" : "|" + defaultLanguage) + ("en".equals(userLanguage) || "en".equals(defaultLanguage) ? "" : "|en");
    }

    private static void addFilterParams(@NonNull final Map<String, String> valueMap) {
        valueMap.put("status", "Available|Temporarily unavailable");
    }

    private static void addRetrieveParams(@NonNull final Parameters params, @NonNull final OCApiConnector connector) {
        params.add("retr_method", METHOD_RETRIEVE_CACHES);
        params.add("retr_params", "{\"fields\": \"" + getCoreFields(connector) + "\"}");
        params.add("wrap", "true");
    }

    private static String getFilterFromType(final CacheType ct) {

        switch (ct) {
            case EVENT:
                return "Event";
            case MULTI:
                return "Multi";
            case MYSTERY:
                return "Quiz";
            case TRADITIONAL:
                return "Traditional";
            case VIRTUAL:
                return "Virtual";
            case WEBCAM:
                return "Webcam";
            default:
                return "";
        }
    }

    @Nullable
    @WorkerThread
    public static String getUserUUID(@NonNull final OCApiConnector connector, @NonNull final String userName) {
        //try username as id
        JSONResult result = getRequest(connector, OkapiService.SERVICE_USER_BY_USERID, new Parameters("fields", USER_UUID, USER_INTERNAL_ID, userName));
        if (!result.isSuccess) {
            //try username as username
            result = getRequest(connector, OkapiService.SERVICE_USER_BY_USERNAME, new Parameters("fields", USER_UUID, USER_USERNAME, userName));
            if (!result.isSuccess) {
                final OkapiError error = new OkapiError(result.data);
                Log.e("OkapiClient.getUserUUID: error getting user info via id or username: '" + error.getMessage() + "'");
                return null;
            }
        }

        return result.data.path(USER_UUID).asText(null);
    }

    @NonNull
    @WorkerThread
    public static UserInfo getUserInfo(@NonNull final OCApiLiveConnector connector) {
        final Parameters params = new Parameters("fields", USER_INFO_FIELDS);

        final JSONResult result = getRequest(connector, OkapiService.SERVICE_USER, params);

        if (!result.isSuccess) {
            final OkapiError error = new OkapiError(result.data);
            Log.w("OkapiClient.getUserInfo: error getting user info: '" + error.getMessage() + "'");
            return new UserInfo(StringUtils.EMPTY, UNKNOWN_FINDS, UserInfoStatus.getFromOkapiError(error.getResult()));
        }

        final ObjectNode data = result.data;
        final boolean successUserName = data.has(USER_USERNAME);
        final String name = data.path(USER_USERNAME).asText();
        final boolean successFinds = data.has(USER_CACHES_FOUND);
        final int finds = data.path(USER_CACHES_FOUND).asInt();

        return new UserInfo(name, finds, successUserName && successFinds ? UserInfoStatus.SUCCESSFUL : UserInfoStatus.FAILED);
    }

    /**
     * Retrieves error information from an unsuccessful Okapi-response
     *
     * @param response response containing an error object
     * @return OkapiError object with detailed information
     */
    @NonNull
    public static OkapiError decodeErrorResponse(final Response response) {
        final JSONResult result = new JSONResult(response);
        if (!result.isSuccess) {
            return new OkapiError(result.data);
        }
        return new OkapiError(new ObjectNode(JsonUtils.factory));
    }

    /**
     * Encapsulates response state and content of an HTTP-getRequest that expects a JSON result. {@code isSuccess} is
     * only true, if the response state was success and {@code data} is not null.
     */
    private static class JSONResult {

        public final boolean isSuccess;
        public final ObjectNode data;

        JSONResult(final Response response) {
            ObjectNode tempData = null;
            try {
                tempData = (ObjectNode) JsonUtils.reader.readTree(response.body().byteStream());
            } catch (final Exception e) {
                // ignore
            } finally {
                response.close();
            }
            data = tempData;
            isSuccess = response.isSuccessful() && tempData != null;
        }

        JSONResult(@NonNull final String errorMessage) {
            isSuccess = false;
            data = new ObjectNode(JsonUtils.factory);
            data.putObject("error").put("developer_message", errorMessage);
        }
    }

    /**
     * extract the geocode from an URL, by using a backward mapping on the server
     */
    @Nullable
    @WorkerThread
    public static String getGeocodeByUrl(@NonNull final OCApiConnector connector, @NonNull final String url) {
        final Parameters params = new Parameters("urls", url);
        final ObjectNode data = getRequest(connector, OkapiService.SERVICE_RESOLVE_URL, params).data;

        if (data == null) {
            return null;
        }

        return data.path("results").path(0).asText(null);
    }

    /**
     * get the registration url for mobile devices
     */
    public static String getMobileRegistrationUrl(@NonNull final OCApiConnector connector) {
        return getInstallationInformation(connector).mobileRegistrationUrl;
    }

    /**
     * get the normal registration url
     */
    public static String getRegistrationUrl(@NonNull final OCApiConnector connector) {
        return getInstallationInformation(connector).registrationUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InstallationInformation {
        @JsonProperty("site_url")
        String siteUrl;
        @JsonProperty("okapi_base_url")
        String okapiBaseUrl;
        @JsonProperty("okapi_base_urls")
        String[] okapiBaseUrls;
        @JsonProperty("site_name")
        String siteName;
        @JsonProperty("okapi_version_number")
        String okapiVersionNumber;
        @JsonProperty("okapi_revision")
        String okapiRevision;
        @JsonProperty("git_revision")
        String gitRevision;
        @JsonProperty("registration_url")
        String registrationUrl;
        @JsonProperty("mobile_registration_url")
        String mobileRegistrationUrl;
        @JsonProperty("image_max_upload_size")
        Long imageMaxUploadSize;
        @JsonProperty("image_rcmd_max_pixels")
        Long imageRcmdMaxPixels;

        @Override
        @NonNull
        public String toString() {
            return "InstallationInformation{" +
                    "siteUrl='" + siteUrl + '\'' +
                    ", okapiBaseUrl='" + okapiBaseUrl + '\'' +
                    ", okapiBaseUrls=" + Arrays.toString(okapiBaseUrls) +
                    ", siteName='" + siteName + '\'' +
                    ", okapiVersionNumber='" + okapiVersionNumber + '\'' +
                    ", okapiRevision='" + okapiRevision + '\'' +
                    ", gitRevision='" + gitRevision + '\'' +
                    ", registrationUrl='" + registrationUrl + '\'' +
                    ", mobileRegistrationUrl='" + mobileRegistrationUrl + '\'' +
                    ", imageMaxUploadSize=" + imageMaxUploadSize +
                    ", imageRcmdMaxPixels=" + imageRcmdMaxPixels +
                    '}';
        }
    }

    @NonNull
    @WorkerThread
    static InstallationInformation getInstallationInformation(final OCApiConnector connector) {
        if (connector.getInstallationInformation() != null) {
            return connector.getInstallationInformation();
        }
        final ObjectNode data = getRequest(connector, OkapiService.SERVICE_API_INSTALLATION, new Parameters()).data;

        if (data == null) {
            return new InstallationInformation();
        }

        try {
            final InstallationInformation info = MAPPER.readValue(data.traverse(), InstallationInformation.class);
            connector.setInstallationInformation(info);
            Log.i("OkapiClient.getInstallationInformation: " + info);
            return info;
        } catch (final IOException e) {
            Log.e("OkapiClient.getInstallationInformation: Couldn't read InstallationInformation", e);
        }

        return new InstallationInformation();
    }

    /**
     * Fetch more caches, if the GC connector is not active at all.
     */
    private static String getCacheLimit() {
        return GCConnector.getInstance().isActive() ? "20" : "100";
    }
}
