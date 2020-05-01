package cgeo.geocaching.connector.gc;

import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.functions.Function;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Handles geocaching.com web-api requests.
 *
 * These are all HTTP endpoints with prefix {@link #API_URL}.
 * This is not the official GC Live API.
 */
class GCWebAPI {

    private static final Object CACHE_LOCK = new Object();
    private static final String API_URL = "https://www.geocaching.com/api/proxy";

    /** maximum number of elements to retrieve with one call */
    private static final int MAX_TAKE = 50;

    private static Authorization cachedAuthorization;
    private static long cachedAuthorizationExpires;

    private GCWebAPI() {
        // Utility class, do not instantiate
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Authorization {
        @JsonProperty("token_type")
        String tokenType;
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("expires_in")
        long expiresIn;      // In seconds

        String getAuthorizationField() {
            return tokenType + ' ' + accessToken;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TrackableLog {
        @JsonProperty("logType")
        TrackableLogType logType;
        @JsonProperty("date")
        String date;
        @JsonProperty("geocache")
        Geocache geocache;
        @JsonProperty("referenceCode")
        String referenceCode;

        TrackableLog(final String logTypeId, final String date, final String geocode, final String referenceCode) {
            this.logType = new TrackableLogType(logTypeId);
            this.date = date;
            this.geocache = new Geocache(geocode);
            this.referenceCode = referenceCode;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static final class TrackableLogType {
            @JsonProperty("id")
            String id;

            TrackableLogType(final String id) {
                this.id = id;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static final class Geocache {
            @JsonProperty("gcCode")
            String geocode;

            Geocache(final String geocode) {
                this.geocode = geocode;
            }
        }

    }

    /**
     * [{"referenceCode":"TB....","iconUrl":"http://www.geocaching.com/images/wpttypes/4433.gif","name":"Some-Geocoin","distanceTraveledInMiles":13350.6100050281,"distanceTraveledInKilometers":21485.7241079319425664,"currentGoal":"Goal of the owner.","description":"","dateReleased":"2011-08-31T12:00:00","locationReleased":{"state":"Hessen","country":"Germany","isoCountryCode":"DE"},"allowedToBeCollected":true,"owner":{"avatarUrl":"https://img.geocaching.com/avatar/...jpg","membershipTypeId":3,"code":"PR...","publicGuid":"...","userName":"..."},"holder":{"avatarUrl":"https://img.geocaching.com/avatar/...jpg","membershipTypeId":3,"code":"PR...","publicGuid":"...","userName":"..."},"inHolderCollection":false,"isMissing":false,"isActive":true,"isLocked":false,"journeyStepsCount":1638,"ownerImagesCount":0,"activityImagesCount":0,"activityCount":1688,"trackingNumber":"...","trackingNumberSha512Hash":"...","trackableType":{"id":4433,"name":"...-Geocoin","imageName":"4433.gif"}}]
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TrackableInventoryEntry {
        @JsonProperty("referenceCode")
        String referenceCode;
        @JsonProperty("name")
        String name;
        @JsonProperty("iconUrl")
        String iconUrl;
        @JsonProperty("trackingNumber")
        String trackingNumber;
    }

    private static Single<Authorization> getAuthorization() {
        return Network.getRequest("https://www.geocaching.com/account/oauth/token", Authorization.class, null, null);
    }

    private static Single<Authorization> getCachedAuthorization() {
        synchronized (CACHE_LOCK) {
            if (System.currentTimeMillis() < cachedAuthorizationExpires) {
                return Single.just(cachedAuthorization);
            }
            // We may request several authorizations at the same time. This is not a big deal, and the web
            // implementation does this much more than we will ever do.
            return getAuthorization().map(authorization -> {
                synchronized (CACHE_LOCK) {
                    cachedAuthorization = authorization;
                    // Expires after .8 of authorized caching time.
                    cachedAuthorizationExpires = System.currentTimeMillis() + authorization.expiresIn * 800;
                    return cachedAuthorization;
                }
            });
        }
    }

    static Single<Parameters> getAuthorizationHeader() {
        return getCachedAuthorization().map(authorization -> new Parameters("Authorization", authorization.getAuthorizationField()));
    }

    /**
     * <pre>
     *     {"id":6189730,"referenceCode":"GC74HPM","postedCoordinates":{"latitude":48.818817,"longitude":2.337833},
     *     "callerSpecific":{"favorited":false},
     *     "owner":{"id":15646120,"referenceCode":"PRHBZWF"},
     *     "geocacheType":{"id":3,"name":"Multi-cache"}}
     * </pre>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CacheDetails {
        long id;
        String referenceCode;
        PostedCoordinates postedCoordinates;
        CallerSpecific callerSpecific;
        Owner owner;
        GeocacheType geocacheType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Owner {
        long id;
        String referenceCode;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CacheOwner {
        @JsonProperty("code")
        String code;
        @JsonProperty("username")
        String username;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class MapSearchResultSet {
        @JsonProperty("results")
        List<MapSearchResult> results;
        @JsonProperty("total")
        int total;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class MapSearchResult {
        @JsonProperty("code")
        String code;
        @JsonProperty("name")
        String name;
        @JsonProperty("postedCoordinates")
        PostedCoordinates postedCoordinates;
        @JsonProperty("userCorrectedCoordinates")
        PostedCoordinates userCorrectedCoordinates;
        @JsonProperty("owner")
        CacheOwner owner;
        @JsonProperty("premiumOnly")
        boolean premiumOnly;
        @JsonProperty("geocacheType")
        int geocacheType;
        @JsonProperty("userFound")
        boolean userFound;
        @JsonProperty("cacheStatus")
        int cacheStatus;
        @JsonProperty("difficulty")
        float difficulty;
        @JsonProperty("terrain")
        float terrain;
        @JsonProperty("containerType")
        int containerType;
        @JsonProperty("favoritePoints")
        int favoritePoints;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class GeocacheType {
        long id;
        String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class PostedCoordinates {
        @JsonProperty("latitude")
        double latitude;
        @JsonProperty("longitude")
        double longitude;

        PostedCoordinates(final double latitude, final double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        PostedCoordinates() {
        }

        Geopoint toCoords() {
            return new Geopoint(latitude, longitude);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CallerSpecific {
        boolean favorited;

        CallerSpecific(final boolean favorited) {
            this.favorited = favorited;
        }

        CallerSpecific() {
        }
    }

    /**
     * {"referenceCode":"GL...","logOwner":{"referenceCode":"PR..."},"imageCount":0,"dateTimeCreatedUtc":"2017-09-07T20:52:45.1344278Z","logDate":"2017-09-03T12:00:00","logText":"some log text","logType":4,"isTextRot13":false,"guid":"e6f6cc03-...","geocache":{"referenceCode":"GC..."},"usedFavoritePoint":false,"updatedCoordinates":{"latitude":0.0,"longitude":0.0}}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class PostLogResponse {
        @JsonProperty("referenceCode")
        String referenceCode;
        @JsonProperty("guid")
        String guid;
    }

    /**
     * {"guid":"14242d4d-...","url":"https://img.geocaching.com/14242d4d-...jpg","thumbnailUrl":"https://img.geocaching.com/large/14242d4d-...jpg","success":true}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class PostLogImageResponse {
        @JsonProperty("guid")
        String guid;
        @JsonProperty("url")
        String url;
        @JsonProperty("thumbnailUrl")
        String thumbnailUrl;
        @JsonProperty("success")
        boolean success;
    }

    /**
     * {"name":"","uuid":"","guid":"14242d4d-ca1f-425e-9496-aa830d769350","thumbnailUrl":"https://img.geocaching.com/large/14242d4d-...jpg","dateTaken":"2017-09-07","description":"","qqDropTarget":{},"id":3,"filename":"filename.png","lastModified":1494143750916,"lastModifiedDate":"2017-05-07T07:55:50.916Z","webkitRelativePath":"","size":13959,"type":"image/png"}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class AttachLogImageRequest {
        @JsonProperty("name")
        String name = "";
        @JsonProperty("guid")
        String guid;
        @JsonProperty("thumbnailUrl")
        String thumbnailUrl;
        @JsonProperty("dateTaken")
        String dateTaken;
        @JsonProperty("description")
        String description = "";
        @JsonProperty("type")
        String type;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GeocacheLog {
        Geocache geocache;
        String logType;
        boolean ownerIsViewing;
        String logDate;
        String logText;
        boolean usedFavoritePoint;

        GeocacheLog(final String id, final String referenceCode, final double latitude,
                    final double longitude, final boolean favorited, final String logType,
                    final boolean ownerIsViewing, final String logDate, final String logText,
                    final boolean usedFavoritePoint) {
            this.geocache = new Geocache(id, referenceCode, latitude, longitude, favorited);
            this.logType = logType;
            this.ownerIsViewing = ownerIsViewing;
            this.logDate = logDate;
            this.logText = logText;
            this.usedFavoritePoint = usedFavoritePoint;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static final class Geocache {
            String id;
            String referenceCode;
            PostedCoordinates postedCoordinates;
            CallerSpecific callerSpecific;

            Geocache(final String id, final String referenceCode, final double latitude,
                     final double longitude, final boolean favorited) {
                this.id = id;
                this.referenceCode = referenceCode;
                this.postedCoordinates = new PostedCoordinates(latitude, longitude);
                this.callerSpecific = new CallerSpecific(favorited);
            }
        }

    }


    private static <T> Single<T> getAPI(final String path, final Class<T> clazz) {
        return Network.getRequest(API_URL + path, clazz, null, null).subscribeOn(AndroidRxUtils.networkScheduler);
    }

    private static <T> Single<T> getAPI(final String path, final Parameters parameters, final Class<T> clazz) {
        return getAuthorizationHeader().flatMap((Function<Parameters, SingleSource<T>>) headers -> Network.getRequest(API_URL + path, clazz, parameters, headers).subscribeOn(AndroidRxUtils.networkScheduler));
    }

    private static Single<Response> patchAPI(final String path) {
        return getAuthorizationHeader().flatMap((Function<Parameters, Single<Response>>) headers -> Network.patchRequest(API_URL + path, headers).subscribeOn(AndroidRxUtils.networkScheduler));
    }

    private static Single<Response> postAPI(final String path, final Parameters parameters) {
        return getAuthorizationHeader().flatMap((Function<Parameters, Single<Response>>) headers -> Network.postRequest(API_URL + path, parameters, headers).subscribeOn(AndroidRxUtils.networkScheduler));
    }

    private static <T> Single<T> postAPI(final String path, final Parameters parameters, final Class<T> clazz) {
        return getAuthorizationHeader().flatMap((Function<Parameters, SingleSource<T>>) headers -> Network.postRequest(API_URL + path, clazz, parameters, headers).subscribeOn(AndroidRxUtils.networkScheduler));
    }

    private static <T> Single<T> postAPI(final String path, final Object jsonObject, final Class<T> clazz) throws JsonProcessingException {
        return Network.postJsonRequest(API_URL + path, clazz, jsonObject).subscribeOn(AndroidRxUtils.networkScheduler);
    }

    private static <T> Single<T> postAPI(final String path, final Parameters parameters,
                                         final String fileFieldName, final String fileContentType, final File file, final Class<T> clazz) {
        return getAuthorizationHeader().flatMap((Function<Parameters, SingleSource<T>>) headers -> Network.postRequest(API_URL + path, clazz, parameters, headers, fileFieldName, fileContentType, file).subscribeOn(AndroidRxUtils.networkScheduler));
    }

    private static Single<Response> postAPI(final String path, final Object jsonObject) {
        return getAuthorizationHeader().flatMap((Function<Parameters, Single<Response>>) headers -> Network.postJsonRequest(API_URL + path, headers, jsonObject).subscribeOn(AndroidRxUtils.networkScheduler));
    }

    static Single<CacheDetails> getCacheDetails(final String geocode) {
        return getAPI("/web/v1/geocache/" + StringUtils.lowerCase(geocode), CacheDetails.class);
    }



    static MapSearchResultSet searchMap(@NonNull final Viewport viewport) {
        final Parameters params = new Parameters();

        final StringBuilder box = new StringBuilder();
        box.append(viewport.getLatitudeMax()).append(',').append(viewport.getLongitudeMin());
        box.append(',').append(viewport.getLatitudeMin()).append(',').append(viewport.getLongitudeMax());
        params.put("box", box.toString());

        final StringBuilder origin = new StringBuilder();
        origin.append(viewport.getCenter().getLatitude()).append(',').append(viewport.getCenter().getLongitude());
        params.put("take", "500");
        params.put("asc", "true");
        params.put("skip", "0");
        params.put("sort", "distance");
        params.put("origin", origin.toString());

        if (!Settings.getCacheType().equals(CacheType.ALL)) {
            params.put("ct", Settings.getCacheType().wptTypeId);
        }

        //Hide owned/hide found caches, only works for premium members
        if (Settings.isGCPremiumMember() && Settings.isExcludeMyCaches()) {
            params.put("ho", "1");
            params.put("hf", "1");
        }

        params.put("app", "cgeo");
        return getAPI("/web/search", params, MapSearchResultSet.class).blockingGet();
    }

    @NonNull
    static ImmutablePair<StatusCode, String> postLog(final Geocache geocache,
                                                     final LogType logType, final Date date,
                                                     final String log, @NonNull final List<cgeo.geocaching.log.TrackableLog> trackables,
                                                     final boolean addToFavorites) {
        if (StringUtils.isBlank(log)) {
            Log.w("GCWebAPI.postLog: No log text given");
            return new ImmutablePair<>(StatusCode.NO_LOG_TEXT, "");
        }

        final String logInfo = log.replace("\n", "\r\n").trim(); // windows' eol and remove leading and trailing whitespaces

        Log.i("Trying to post log for cache #" + geocache.getCacheId() + " - action: " + logType
                + "; date: " + date + ", log: " + logInfo
                + "; trackables: " + trackables.size());

        try {
            // coordinates are only used for LogType.UPDATE_COORDINATES, which c:geo doesn't support at the moment
            final double latitude = 0.0;
            final double longitude = 0.0;

            final String logDate = formatGCDate(date);

            // Make the post body
            final Parameters params = new Parameters();
            params.put("geocache[id]", geocache.getCacheId()).
                    put("geocache[referenceCode]", geocache.getGeocode()).
                    put("geocache[postedCoordinates][latitude]", formatDouble(latitude)).
                    put("geocache[postedCoordinates][longitude]", formatDouble(longitude)).
                    put("geocache[callerSpecific][favorited]", formatBoolean(addToFavorites)).
                    put("logType", String.valueOf(logType.id)).
                    put("ownerIsViewing", formatBoolean(geocache.isOwner())).
                    put("logDate", logDate).
                    put("logText", logInfo).
                    put("usedFavoritePoint", formatBoolean(addToFavorites));

            final GeocacheLog geocacheLog = new GeocacheLog(geocache.getCacheId(), geocache.getGeocode(),
                    latitude, longitude, addToFavorites, String.valueOf(logType.id),
                    geocache.isOwner(), logDate, logInfo, addToFavorites);

            final PostLogResponse response =
                    postAPI("/web/v1/geocache/" + StringUtils.lowerCase(geocache.getGeocode())
                            + "/GeocacheLog", geocacheLog, PostLogResponse.class).blockingGet();

            if (response.referenceCode == null) {
                return new ImmutablePair<>(StatusCode.LOG_POST_ERROR, "");
            }

            if (!postLogTrackable(geocache.getGeocode(), logDate, trackables)) {
                return new ImmutablePair<>(StatusCode.LOG_POST_ERROR, "");
            }

            Log.i("Log successfully posted to cache #" + geocache.getCacheId());
            return new ImmutablePair<>(StatusCode.NO_ERROR, response.referenceCode);
        } catch (final Exception e) {
            Log.e("Error posting log", e);
        }
        return new ImmutablePair<>(StatusCode.LOG_POST_ERROR, "");
    }

    private static String formatDouble(final double dbl) {
        return String.format(Locale.US, "%.6f", dbl);
    }

    private static String formatBoolean(final boolean bool) {
        return bool ? "true" : "false";
    }

    private static String formatGCDate(final Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date);
    }

    /**
     * Sends trackable logs in groups of 10.
     * https://github.com/cgeo/cgeo/issues/7249
     *
     * https://www.geocaching.com/api/proxy/trackable/activities
     * <div>
     *   "postData": {
     *       "mimeType": "application/json",
     *       "text": "[{"logType":{"id":"75"},"date":"2017-08-19","geocache":{"gcCode":"GC..."},"referenceCode":"TB..."}]"
     *   }
     * </div>
     */
    static boolean postLogTrackable(final String geocode, final String logDate, final List<cgeo.geocaching.log.TrackableLog> trackables) {
        final List<TrackableLog> trackableLogs = new ArrayList<>(trackables.size());

        for (final cgeo.geocaching.log.TrackableLog tb : trackables) {
            if (tb.action != LogTypeTrackable.DO_NOTHING && tb.brand == TrackableBrand.TRAVELBUG) {
                trackableLogs.add(new TrackableLog(String.valueOf(tb.action.gcApiId), logDate, geocode, tb.geocode));
            }
            if (trackableLogs.size() == 10) {
                if (postLogTrackable(trackableLogs).isSuccessful()) {
                    trackableLogs.clear();
                } else {
                    return false;
                }
            }
        }
        return trackableLogs.isEmpty() || postLogTrackable(trackableLogs).isSuccessful();
    }

    private static Response postLogTrackable(final List<TrackableLog> trackableLogs) {
        final Response response = postAPI("/trackable/activities", trackableLogs).blockingGet();
        if (!response.isSuccessful()) {
            Log.e("Logging trackables failed: " + response.message());
        }
        return response;
    }

    /**
     * https://www.geocaching.com/api/proxy/trackables?inCollection=false&skip=0&take=50
     */
    static List<TrackableInventoryEntry> getTrackableInventory() {
        final List<TrackableInventoryEntry> trackableInventoryEntries = new ArrayList<>();
        int skip = 0;
        TrackableInventoryEntry[] entries;
        do {
            entries = getAPI("/trackables?inCollection=false&take=" + MAX_TAKE + "&skip=" + skip, TrackableInventoryEntry[].class).blockingGet();
            trackableInventoryEntries.addAll(Arrays.asList(entries));
            skip += MAX_TAKE;
        } while (entries.length == MAX_TAKE);
        return trackableInventoryEntries;
    }

    /**
     * https://www.geocaching.com/api/proxy/web/v1/users/PR.../availablefavoritepoints
     */
    static Single<Integer> getAvailableFavoritePoints(final String profile) {
        return getAPI("/web/v1/users/" + profile + "/availablefavoritepoints", Integer.class);
    }

    /**
     * Post an image and attach it to the log.
     *
     * The following sequence of http requests have to be performed:
     *
     * 1)
     * Post image to: https://www.geocaching.com/api/proxy/web/v1/LogDrafts/images
     * Request:
     * Content-Type: multipart/form-data; boundary=----WebKitFormBoundary75K6f...
     * <pre>
     * ------WebKitFormBoundary75K6f...
     * Content-Disposition: form-data; name="guid"
     *
     * 14242d4d-...
     * ------WebKitFormBoundary75K6f...
     * Content-Disposition: form-data; name="qqfilename"
     *
     * ic_launcher.png
     * ------WebKitFormBoundary75K6f...
     * Content-Disposition: form-data; name="qqtotalfilesize"
     *
     * 13959
     * ------WebKitFormBoundary75K6f...
     * Content-Disposition: form-data; name="qqfile"; filename="filename.png"
     * Content-Type: image/png
     *
     *
     * ------WebKitFormBoundary75K6f...--
     * </pre>
     * Response:
     * {"guid":"14242d4d-...","url":"https://img.geocaching.com/14242d4d-...jpg","thumbnailUrl":"https://img.geocaching.com/large/14242d4d-...jpg","success":true}
     *
     *
     * 2)
     * PATCH: https://www.geocaching.com/api/proxy/web/v1/LogDrafts/images/14242d4d-...?geocacheLogReferenceCode=GL...
     * Response (not used):
     * {"guid":"2dc54b26-...","name":" "}
     *
     *
     * 3)
     * POST: https://www.geocaching.com/api/proxy/web/v1/geocaches/logs/GL.../images/14242d4d-...
     * Request:
     * <pre>
     *     {"name":"","uuid":"","guid":"14242d4d-ca1f-425e-9496-aa830d769350","thumbnailUrl":"https://img.geocaching.com/large/14242d4d-...jpg","dateTaken":"2017-09-07","description":"","qqDropTarget":{},"id":3,"filename":"filename.png","lastModified":1494143750916,"lastModifiedDate":"2017-05-07T07:55:50.916Z","webkitRelativePath":"","size":13959,"type":"image/png"}
     * </pre>
     * Response not used
     */
    static ImmutablePair<StatusCode, String> postLogImage(final String logId, final Image image) {
        // 1) upload image to drafts
        final Parameters params = new Parameters();
        params.put("guid", UUID.randomUUID().toString());

        final PostLogImageResponse postImageResponse = postAPI("/web/v1/LogDrafts/images", params, "qqfilename", "image/jpeg", image.getFile(), PostLogImageResponse.class).blockingGet();
        if (!postImageResponse.success) {
            return new ImmutablePair<>(StatusCode.LOGIMAGE_POST_ERROR, null);
        }

        // 2) patch draft image with logId
        final Response patchResponse = patchAPI("/web/v1/LogDrafts/images/" + postImageResponse.guid + "?geocacheLogReferenceCode=" + logId).blockingGet();
        if (!patchResponse.isSuccessful()) {
            return new ImmutablePair<>(StatusCode.LOGIMAGE_POST_ERROR, null);
        }

        // 3) attach image to log
        final AttachLogImageRequest attachImageRequest = new AttachLogImageRequest();
        attachImageRequest.guid = postImageResponse.guid;
        attachImageRequest.type = "image/jpeg";
        attachImageRequest.thumbnailUrl = postImageResponse.thumbnailUrl;
        attachImageRequest.name = StringUtils.defaultString(image.getTitle());
        attachImageRequest.description = StringUtils.defaultString(image.getDescription());
        final Response attachResponse = postAPI("/web/v1/geocaches/logs/" + logId + "/images/" + postImageResponse.guid, attachImageRequest).blockingGet();
        if (!attachResponse.isSuccessful()) {
            return new ImmutablePair<>(StatusCode.LOGIMAGE_POST_ERROR, null);
        }

        return new ImmutablePair<>(StatusCode.NO_ERROR, postImageResponse.url);
    }
}
