package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.network.HttpResponse;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.util.Consumer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.functions.Function;
import okhttp3.Response;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Handles geocaching.com web-api requests.
 *
 * These are all HTTP endpoints with prefix {@link #API_URL}.
 * This is not the official GC Live API.
 */
public class GCWebAPI {

    private static final Object CACHE_LOCK = new Object();
    private static final String WEBSITE_URL = "https://www.geocaching.com";
    private static final String API_URL = WEBSITE_URL + "/api/proxy";

    //<input name="__RequestVerificationToken" type="hidden" value="(token-value)" />
    private static final Pattern PATTERN_REQUEST_VERIFICATION_TOKEN = Pattern.compile("name=\"__RequestVerificationToken\"\\s+type=\"hidden\"\\s+value=\"([^\"]+)\"");

    private static final String HTML_HEADER_CSRF_TOKEN = "CSRF-Token";

    /**
     * maximum number of elements to retrieve with one call
     */
    private static final int MAX_TAKE = 50;

    private static Authorization cachedAuthorization;
    private static long cachedAuthorizationExpires;

    private GCWebAPI() {
        // Utility class, do not instantiate
    }

    /**
     * This class encapsulates, explains and mimics the search against gc.com WebApi at https://www.geocaching.com/api/proxy/web/search/v2
     */
    public static class WebApiSearch {

        public enum SortType { DISTANCE, FAVORITEPOINT, DIFFICULTY, TERRAIN }

        private static final DateFormat PARAM_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        private static final long ONE_DAY_MILLISECONDS = 24 * 60 * 60 * 1000;

        private Viewport box;
        private Geopoint origin;

        private Boolean statusOwn = null;
        private Boolean statusFound = null;
        private Boolean statusMembership = null;
        private Boolean statusEnabled = null;
        private Boolean statusCorrectedCoordinates = null;

        private final Set<CacheType> cacheTypes = new HashSet<>();
        private final Set<CacheSize> cacheSizes = new HashSet<>();
        private final Set<CacheAttribute> cacheAttributes = new HashSet<>();

        private String hiddenBy = null;
        private String notFoundBy = null;
        private String difficulty = null;
        private String terrain = null;
        private String difficultyTerrainCombis = null;
        private String placedFrom;
        private String placedTo;
        private String keywords;
        private int minFavoritePoints = -1;

        private SortType sort = SortType.DISTANCE;
        private boolean sortAsc = true;

        private int skip = 0;
        private int take = 500;

        public WebApiSearch setPage(final int take, final int skip) {
            this.take = take;
            this.skip = skip;
            return this;
        }

        public WebApiSearch setSort(final SortType sort, final boolean sortAsc) {
            this.sort = sort;
            this.sortAsc = sortAsc;
            return this;
        }

        public int getTake() {
            return take;
        }

        public int getSkip() {
            return skip;
        }

        public SortType getSort() {
            return this.sort;
        }

        public boolean getSortAsc() {
            return this.sortAsc;
        }

        /**
         * filters for given cache types. Works for V1
         */
        public WebApiSearch addCacheTypes(final Collection<CacheType> ct) {
            cacheTypes.addAll(CollectionStream.of(ct).filter(type -> type != CacheType.ALL).toList());
            return this;
        }

        /**
         * filters for given cache sizes. Works for V1
         */
        public WebApiSearch addCacheSizes(final Collection<CacheSize> cs) {
            cacheSizes.addAll(cs);
            return this;
        }

        /**
         * filters for given cache attriutes. Only positive attributes can be filtered, no exclude possible
         * TODO does not work with V1, only works with V2!
         */
        public WebApiSearch addCacheAttributes(final CacheAttribute... ca) {
            cacheAttributes.addAll(Arrays.asList(ca));
            return this;
        }

        public Set<CacheAttribute> getCacheAttributes() {
            return this.cacheAttributes;
        }

        /**
         * set to true to show ONLY own caches, false to HIDE own caches, null if both should be shown.
         * Works only for Premium members!
         * Works with V1
         */
        public WebApiSearch setStatusOwn(final Boolean statusOwn) {
            this.statusOwn = statusOwn;
            return this;
        }

        /**
         * set to true to show ONLY found caches, false to HIDE found caches, null if both should be shown.
         * Works only for Premium members!
         * Works with V1
         */
        public WebApiSearch setStatusFound(final Boolean statusFound) {
            this.statusFound = statusFound;
            return this;
        }

        /**
         * set to true to show ONLY basic caches, false show ONLY premium caches, null if both should be shown.
         */
        public WebApiSearch setStatusMembership(final Boolean statusMembership) {
            this.statusMembership = statusMembership;
            return this;
        }

        /**
         * set to true to show ONLY enabled caches, false show ONLY disabled caches, null if both should be shown.
         */
        public WebApiSearch setStatusEnabled(final Boolean statusEnabled) {
            this.statusEnabled = statusEnabled;
            return this;
        }

        /**
         * set to true to show ONLY caches with original coordinates, false show ONLY caches with corrected coordinates, null if both should be shown.
         */
        public WebApiSearch setStatusCorrectedCoordinates(final Boolean statusCorrectedCoordinates) {
            this.statusCorrectedCoordinates = statusCorrectedCoordinates;
            return this;
        }

        /**
         * Works only if 'hiddenBy' is the exact owner name, also case muist match! Withs with V1
         */
        public WebApiSearch setHiddenBy(final String hiddenBy) {
            this.hiddenBy = hiddenBy;
            return this;
        }

        /**
         * Works only if 'notFoundBy' is the exact name of a geocache user. case does not need to match though. Works with V1
         */
        public WebApiSearch setNotFoundBy(final String notFoundBy) {
            this.notFoundBy = notFoundBy;
            return this;
        }

        /**
         * set to a value > 0 to trigger search. Works with V1
         */
        public WebApiSearch setMinFavoritepoints(final int minFavoritePoints) {
            this.minFavoritePoints = minFavoritePoints;
            return this;
        }

        /**
         * Searches on DAY level only. from or to may be null, then "before"/"After" search logic is used. Works for V1
         */
        public WebApiSearch setPlacementDate(final Date from, final Date to) {
            // after: pad
            // between: psd - ped
            // before: pbd
            // on: pod
            //date format: yyyy-mm-dd
            //Note: gc.com beans "before" and "after" literally: palcements on the given dates itself are NOT included in search result!
            //in "between" search, given dates are included
            if (from == null && to == null) {
                placedFrom = null;
                placedTo = null;
            } else if (from == null) {
                // -> before "to", set "placedTo" to one day AFTER
                placedFrom = null;
                placedTo = PARAM_DATE_FORMATTER.format(new Date(to.getTime() + ONE_DAY_MILLISECONDS));
            } else if (to == null) {
                // -> after "from", set "placedFrom" to one day BEFORE
                placedFrom = PARAM_DATE_FORMATTER.format(new Date(from.getTime() - ONE_DAY_MILLISECONDS));
                placedTo = null;
            } else {
                final boolean fromBeforeTo = from.before(to);
                placedFrom = PARAM_DATE_FORMATTER.format(fromBeforeTo ? from : to);
                placedTo = PARAM_DATE_FORMATTER.format(fromBeforeTo ? to : from);
            }
            return this;
        }

        /**
         * Searches for keywords in cache name only. Search uses "contains" logic.
         * Must be whole word(s), e.g. "amburg" won't find caches with "Hamburg" in them.
         * In case multiple words are given they must occur in this order. E.g. "Hamburger Hafen" will not find "Hafen in Hamburg"
         * Is case insensitive
         */
        public WebApiSearch setKeywords(final String keywords) {
            this.keywords = keywords;
            return this;
        }

        /**
         * Sets the area to search in. Woirks with V1
         */
        public WebApiSearch setBox(final Viewport box) {
            this.box = box;
            return this;
        }

        /**
         * Sets the starting point of the search and the reference point for sort by distance. Does not restrict/filter the result. Works with V1
         */
        public WebApiSearch setOrigin(final Geopoint origin) {
            this.origin = origin;
            return this;
        }

        public Geopoint getOrigin() {
            return this.origin;
        }

        /**
         * Works with V1
         */
        public WebApiSearch setDifficulty(final Float pFrom, final Float pTo) {
            this.difficulty = getRangeString(pFrom, pTo);
            return this;
        }

        /**
         * Works with V1
         */
        public WebApiSearch setTerrain(final Float pFrom, final Float pTo) {
            this.terrain = getRangeString(pFrom, pTo);
            return this;
        }

        // Example: m=1-4.5%2C2.5-4.5%2C3-5%2C4.5-5%2C5-3.5%2C5-4%2C5-4.5
        public WebApiSearch setDifficultyTerrainCombis(final Collection<ImmutablePair<Float, Float>> combis) {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (ImmutablePair<Float, Float> combi : combis) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(getCombiString(combi.left, combi.right));
            }
            this.difficultyTerrainCombis = sb.toString();
            return this;
        }

        /**
         * Returns a string specifying a range from 1-5 (in 0.5-steps) as used for parameters difficulty and terrain
         */
        private String getRangeString(final Float pFrom, final Float pTo) {
            if (pFrom == null && pTo == null) {
                return null;
            }

            final float from = pFrom == null ? 1f : Math.round(Math.max(1, Math.min(5, pFrom)) * 2f) / 2f;
            final float to = pTo == null ? 5f : Math.round(Math.max(1, Math.min(5, pTo)) * 2f) / 2f;
            if (from > to) {
                return to + "-" + from;
            }
            return from + "-" + to;
        }

        private String getCombiString(final float diff, final float terrain) {
           return diff + "-" + terrain;
        }

        @WorkerThread
        MapSearchResultSet execute() {
            final Parameters params = new Parameters();

            if (box != null) {
                // on empty viewport silently log stacktrace + return empty searchresult without calling search provider
                if (box.isJustADot()) {
                    try {
                        throw new RuntimeException("searching map with empty viewport");
                    } catch (RuntimeException e) {
                        Log.d("searching map with empty viewport: " + ExceptionUtils.getStackTrace(e));
                    }
                    return new MapSearchResultSet();
                }
                params.put("box", String.valueOf(this.box.getLatitudeMax()) + ',' + this.box.getLongitudeMin() +
                        ',' + this.box.getLatitudeMin() + ',' + this.box.getLongitudeMax());

                //set origin to middle of viewport (will be overridden if origin is set explicitely later)
                params.put("origin", String.valueOf(this.box.getCenter().getLatitude()) + ',' + this.box.getCenter().getLongitude());
            }

            if (origin != null) {
                params.put("origin", String.valueOf(origin.getLatitude()) + ',' + origin.getLongitude());
            }

            if (!this.cacheTypes.isEmpty()) {
                params.put("ct", CollectionStream.of(this.cacheTypes).map(ct -> ct.wptTypeId).toJoinedString(","));
            }

            if (!this.cacheSizes.isEmpty()) {
                params.put("cs", CollectionStream.of(this.cacheSizes).filter(cs -> CacheSize.getGcIdsForSize(cs).length > 0)
                        .map(cs -> CollectionStream.of(ArrayUtils.toObject(CacheSize.getGcIdsForSize(cs))).toJoinedString(",")).toJoinedString(","));
            }

            if (!this.cacheAttributes.isEmpty()) {
                params.put("att", CollectionStream.of(this.cacheAttributes).map(ct -> ct.gcid).toJoinedString(","));
            }

            //Hide owned/hide found caches, only works for premium members
            if (this.statusOwn != null) {
                params.put("ho", this.statusOwn ? "0" : "1");
            }

            if (this.statusFound != null) {
                params.put("hf", this.statusFound ? "0" : "1");
            }

            if (this.statusMembership != null) {
                params.put("sp", this.statusMembership ? "0" : "1");
            }

            if (this.statusEnabled != null) {
                params.put("sd", this.statusEnabled ? "0" : "1");
            }

            if (this.statusCorrectedCoordinates != null) {
                params.put("cc", this.statusCorrectedCoordinates ? "0" : "1");
            }

            if (this.hiddenBy != null) {
                params.put("hb", this.hiddenBy);
            }

            if (this.notFoundBy != null) {
                params.put("nfb", this.notFoundBy);
            }

            if (this.minFavoritePoints > 0) {
                params.put("fp", "" + this.minFavoritePoints);
            }

            if (this.difficulty != null) {
                params.put("d", this.difficulty);
            }

            if (this.terrain != null) {
                params.put("t", this.terrain);
            }

            if (this.difficultyTerrainCombis != null) {
                params.put("m", this.difficultyTerrainCombis);
            }

            if (this.placedFrom != null || this.placedTo != null) {
                // after: pad
                // between: psd - ped
                // before: pbd
                // on: pod (not used by us)
                if (this.placedFrom == null) {
                    params.put("pbd", this.placedTo);
                } else if (this.placedTo == null) {
                    params.put("pad", this.placedFrom);
                } else {
                    params.put("psd", this.placedFrom);
                    params.put("ped", this.placedTo);
                }
            }

            if (this.keywords != null) {
                params.put("cn", this.keywords);
            }

            //paging / result size
            params.put("take", "" + take);
            params.put("skip", "" + skip);

            //sort
            params.put("sort", sort.name().toLowerCase(Locale.getDefault()));
            params.put("asc", "" + sortAsc);

            //ALWAYS send cgeo as an identifier
            params.put("app", "cgeo"); //identify us towards Groundspeak due to gentlemens agreement

            return getAPI("/web/search/v2", params, MapSearchResultSet.class).blockingGet();
        }

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
        @JsonProperty("logType") final TrackableLogType logType;
        @JsonProperty("date") final String date;
        @JsonProperty("geocache") final Geocache geocache;
        @JsonProperty("referenceCode") final String referenceCode;

        TrackableLog(final String logTypeId, final String date, final String geocode, final String referenceCode) {
            this.logType = new TrackableLogType(logTypeId);
            this.date = date;
            this.geocache = new Geocache(geocode);
            this.referenceCode = referenceCode;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static final class TrackableLogType {
            @JsonProperty("id") final String id;

            TrackableLogType(final String id) {
                this.id = id;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static final class Geocache {
            @JsonProperty("gcCode") final String geocode;

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
        String referenceCode; // The public one, starting with "TB"
        @JsonProperty("name")
        String name;
        @JsonProperty("iconUrl")
        String iconUrl;
        @JsonProperty("trackingNumber")
        String trackingNumber; // The secret one
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

    //Complete example for reference
    //    {
    //      "id": 3866836,
    //      "name": "Ness Bridge",
    //      "code": "GC4KJHJ",
    //      "premiumOnly": true,
    //      "favoritePoints": 847,
    //      "geocacheType": 2,
    //      "containerType": 6,
    //      "difficulty": 2,
    //      "terrain": 1.5,
    //      "userFound": false,
    //      "userDidNotFind": false,
    //      "cacheStatus": 0,
    //      "postedCoordinates": {
    //        "latitude": 57.476967,
    //        "longitude": -4.2278
    //      },
    //      "detailsUrl": "/geocache/GC4KJHJ",
    //      "hasGeotour": false,
    //      "hasLogDraft": false,
    //      "placedDate": "2013-08-22T00:00:00",
    //      "owner": {
    //        "code": "PR1ZE74",
    //        "username": "Ah!"
    //      },
    //      "lastFoundDate": "2022-06-22T18:00:49",
    //      "trackableCount": 0,
    //      "region": "Northern Scotland",
    //      "country": "United Kingdom",
    //      "attributes": [
    //        {
    //          "id": 24,
    //          "name": "Wheelchair accessible",
    //          "isApplicable": false
    //        },
    //        {
    //          "id": 8,
    //          "name": "Scenic view",
    //          "isApplicable": true
    //        },
    //        {
    //          "id": 13,
    //          "name": "Available 24/7",
    //          "isApplicable": true
    //        },
    //        {
    //          "id": 7,
    //          "name": "Takes less than one hour",
    //          "isApplicable": true
    //        },
    //        {
    //          "id": 14,
    //          "name": "Recommended at night",
    //          "isApplicable": true
    //        },
    //        {
    //          "id": 40,
    //          "name": "Stealth required",
    //          "isApplicable": true
    //        }
    //      ],
    //      "distance": "Here",
    //      "bearing": ""
    //    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class MapSearchResult {
        @JsonProperty
        int id;
        @JsonProperty("name")
        String name;
        @JsonProperty("code")
        String code;
        @JsonProperty("premiumOnly")
        boolean premiumOnly;
        @JsonProperty("favoritePoints")
        int favoritePoints;
        @JsonProperty("geocacheType")
        int geocacheType;
        @JsonProperty("containerType")
        int containerType;
        @JsonProperty("difficulty")
        float difficulty;
        @JsonProperty("terrain")
        float terrain;
        @JsonProperty("userFound")
        boolean userFound;
        @JsonProperty("userDidNotFind")
        boolean userDidNotFind;
        @JsonProperty("cacheStatus")
        int cacheStatus;
        @JsonProperty("postedCoordinates")
        PostedCoordinates postedCoordinates;
        @JsonProperty("userCorrectedCoordinates")
        PostedCoordinates userCorrectedCoordinates;
        @JsonProperty("detailsUrl")
        String detailsUrl;
        @JsonProperty("hasGeotour")
        boolean hasGeotour;
        @JsonProperty("hasLogDraft")
        boolean hasLogDraft;
        @JsonProperty("placedDate")
        Date placedDate;
        @JsonProperty("owner")
        CacheOwner owner;
        @JsonProperty("lastFoundDate")
        Date lastFoundDate;
        @JsonProperty("trackableCount")
        int trackableCount;
        @JsonProperty("region")
        String region;
        @JsonProperty("country")
        String country;
        @JsonProperty("attributes")
        List<Attribute> attributes;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class GeocacheType {
        long id;
        String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Attribute {
        @JsonProperty("id")
        int id;
        @JsonProperty("name")
        String name;
        @JsonProperty("isApplicable")
        boolean isApplicable;
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
        @JsonProperty("favorited")
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
    static final class PostLogImageResponse extends HttpResponse {
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
        @JsonProperty("geocache") final Geocache geocache;
        @JsonProperty("logType") final String logType;
        @JsonProperty("ownerIsViewing") final boolean ownerIsViewing;
        @JsonProperty("logDate") final String logDate;
        @JsonProperty("logText") final String logText;
        @JsonProperty("usedFavoritePoint") final boolean usedFavoritePoint;

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
            @JsonProperty("id") final String id;
            @JsonProperty("referenceCode") final String referenceCode;
            @JsonProperty("postedCoordinates") final PostedCoordinates postedCoordinates;
            @JsonProperty("callerSpecific") final CallerSpecific callerSpecific;

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

    private static Single<Response> postAPI(final String path, final Object jsonObject, final Consumer<Parameters> headerAdder) {
        return getAuthorizationHeader().flatMap((Function<Parameters, Single<Response>>) headers -> {
            if (headerAdder != null) {
                headerAdder.accept(headers);
            }
            return Network.postJsonRequest(API_URL + path, headers, jsonObject).subscribeOn(AndroidRxUtils.networkScheduler);
        });
    }

    static Single<CacheDetails> getCacheDetails(final String geocode) {
        return getAPI("/web/v1/geocache/" + StringUtils.lowerCase(geocode), CacheDetails.class);
    }

    @WorkerThread
    static SearchResult searchCaches(final IConnector con, final WebApiSearch search, final boolean includeGcVote) {
        final SearchResult result = new SearchResult();

        final MapSearchResultSet mapSearchResultSet = search.execute();
        result.setLeftToFetch(con, mapSearchResultSet.total - search.getTake() - search.getSkip());
        final List<Geocache> foundCaches = new ArrayList<>();

        if (mapSearchResultSet.results != null) {
            for (final GCWebAPI.MapSearchResult r : mapSearchResultSet.results) {

                final Geopoint cacheCoord = r.postedCoordinates == null ? null : new Geopoint(r.postedCoordinates.latitude, r.postedCoordinates.longitude);

                final Geocache c = new Geocache();
                c.setDetailed(false);
                c.setGeocode(r.code);
                c.setName(r.name);
                if (r.userCorrectedCoordinates != null) {
                    c.setCoords(new Geopoint(r.userCorrectedCoordinates.latitude, r.userCorrectedCoordinates.longitude));
                    c.setUserModifiedCoords(true);
                } else if (cacheCoord != null) {
                    c.setCoords(cacheCoord);
                    c.setUserModifiedCoords(false);
                } else {
                    //this can only happen for PREMIUM caches when searched by BASIC members.
                    //Open issue: what to do with those?
                    c.setCoords(null);
                }
                c.setType(CacheType.getByWaypointType(Integer.toString(r.geocacheType)));
                c.setDifficulty(r.difficulty);
                c.setTerrain(r.terrain);
                c.setSize(CacheSize.getByGcId(r.containerType));
                c.setPremiumMembersOnly(r.premiumOnly);
                c.setHidden(r.placedDate);
                c.setLastFound(r.lastFoundDate);
                c.setInventoryItems(r.trackableCount);
                c.setLocation(r.region + ", " + r.country);

                //Only set found if the map returns a "found",
                //the map API will possibly lag behind and break
                //cache merging if "not found" is set
                if (r.userFound) {
                    c.setFound(true);
                } else if (r.userDidNotFind) {
                    c.setDNF(true);
                }

                c.setFavoritePoints(r.favoritePoints);
                c.setDisabled(r.cacheStatus == 1);
                c.setArchived(r.cacheStatus == 2);
                if (r.owner != null) {
                    c.setOwnerDisplayName(r.owner.username);
                    c.setOwnerUserId(r.owner.username);
                }

                // parse attributes
                final List<String> attributes = new ArrayList<>();
                if (r.attributes != null) {
                    for (Attribute attribute : r.attributes) {
                        attributes.add(CacheAttribute.getById(attribute.id).getValue(attribute.isApplicable));
                    }
                }
                c.setAttributes(attributes);

                foundCaches.add(c);
            }

        }

        tryGuessMissingDistances(foundCaches, search);

        result.addAndPutInCache(foundCaches);
        if (includeGcVote) {
            GCVote.loadRatings(foundCaches);
        }
        return result;
    }

    /**
     * For BASIC members, PREMIUM caches don't contain coordinates. This helper methods guesses distances for those caches
     */
    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity"})
    private static void tryGuessMissingDistances(final List<Geocache> caches, final WebApiSearch search) {
        if (caches == null || caches.isEmpty()) {
            return;
        }
        //This heuristic only works if origin is given and sort is of type DISTANCE
        if (search.getOrigin() == null || search.getSort() != WebApiSearch.SortType.DISTANCE) {
            return;
        }

        //inverse the list in case of inverse sort
        List<Geocache> loopCaches = caches;
        if (!search.getSortAsc()) {
            loopCaches = new ArrayList<>(caches);
            Collections.reverse(loopCaches);
        }

        //This heuristic will assign each cache without coordinates the middle of the distance of two surrounding caches with known coordinates
        //    to given pos
        //All caches AFTER the last cache with know coords will get assigend its distance to pos plus 1
        //If ALL caches have no coordinates, they get assigned a distance of 1
        float lastDistance = 0;
        final List<Geocache> emptyCoordCaches = new ArrayList<>();

        for (Geocache c : loopCaches) {
            if (c.getCoords() != null) {
                final float newDistance = search.getOrigin().distanceTo(c.getCoords());
                for (Geocache emptyC : emptyCoordCaches) {
                    emptyC.setDistance(Units.generateSmartRoundedAverageDistance(newDistance, lastDistance));
                }
                emptyCoordCaches.clear();
                lastDistance = newDistance;
            } else {
                emptyCoordCaches.add(c);
            }
        }

        if (!emptyCoordCaches.isEmpty()) {
            for (Geocache emptyC : emptyCoordCaches) {
                emptyC.setDistance(lastDistance == 0 ? 1 : lastDistance + 1);
            }
        }
    }

    @NonNull
    @WorkerThread
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


        return postLogInternal(geocache, logType, date, log, trackables, addToFavorites);
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

    @WorkerThread
    private static Response postLogTrackable(final List<TrackableLog> trackableLogs) {
        final Response response = postAPI("/trackable/activities", trackableLogs, (Consumer<Parameters>) null).blockingGet();
        if (!response.isSuccessful()) {
            Log.e("Logging trackables failed: " + response.message());
        }
        return response;
    }

    /**
     * https://www.geocaching.com/api/proxy/trackables?inCollection=false&skip=0&take=50
     */
    @NonNull
    @WorkerThread
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

    @NonNull
    public static Collection<ImmutablePair<Float, Float>> getNeededDifficultyTerrainCombisFor81Matrix() {
        // Request URI: see code below
        // Answer is a json string array, something like: ["1-4.5","2.5-4.5","3-5","4.5-5","5-3.5","5-4","5-4.5"]

        final String[] rawCombis = apiReq().uri("/web/v1/statistics/difficultyterrainmatrix/needed")
                .requestJson(String[].class).blockingGet();
        if (rawCombis == null || rawCombis.length == 0) {
            return Collections.emptyList();
        }
        final List<ImmutablePair<Float, Float>> result = new ArrayList<>(rawCombis.length);
        try {
            for (String rawCombi : rawCombis) {
                final String[] parts = rawCombi.split("-");
                final float difficulty = Float.parseFloat(parts[0]);
                final float terrain = Float.parseFloat((parts[1]));
                result.add(new ImmutablePair<>(difficulty, terrain));
            }
        } catch (Exception ex) {
            Log.w("Problems parsing as list of dt-combis: " + Arrays.asList(rawCombis));
        }
        return result;
    }

    //Generic Format of a "log entry" Request towards GC.com (for cache or trackable) is as follows:
    // (note request to edit existing log is very similar, but this is not supported by c:geo as of now)
    // {
    //   'geocacheReferenceCode': "GCxyz", // Used only for trackable Logs of type RETRIEVED. Contains GCcode of geocache where tb was retrieved from. mandatory!
    //   'images': [], // an array of image GUIDs  (String). Can be used to assign images uploaded previously with log entry
    //   'logDate': logdate, // timestamp, e.g. "2023-09-08T22:31:54.004Z"
    //   'logText': logtext, //string (logtext)
    //   'logType': logtype, //integer. Available types depend on whether log is for cache or tb, and on state of that cache/tb
    //   'trackables': [], //Only used on cache logs to incidate for own inventory what to do with it. Array of object. Example: [{"trackableCode":"TBxyz","trackableLogTypeId":75}]
    //   'updatedCoordinates': null, //unknown, most likely only used for Owner log when setting new header coords
    //   'usedFavoritePoint': false //boolean. Used on cache logs to add a fav point to the cache
    //  }
    //
    //Generic Format of a log entry Reply from gc.com:
    // {"guid":"xyz","logReferenceCode":"GLxyz","dateTimeCreatedUtc":"2023-09-17T14:03:26","dateTimeLastUpdatedUtc":"2023-09-17T14:03:26","logDate":"2023-09-08T12:00:00","logType":4,"images":[],"trackables":[],"cannotDelete":false,"usedFavoritePoint":false}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GCWebLogTrackable {
        //Log Creation Fields
        @JsonProperty("trackableCode")
        String trackableCode; // e.g. "TBxyz"
        @JsonProperty("trackableLogTypeId")
        Integer trackableLogTypeId;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GCWebLogRequest extends GCWebLogBase {
        @JsonProperty("logType")
        Integer logType;
        @JsonProperty("trackables")
        GCWebLogTrackable[] trackables;
        @JsonProperty("usedFavoritePoint")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean usedFavoritePoint;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GCWebLogResponse extends GCWebLogRequest {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GCWebTrackableLogRequest extends GCWebLogBase {

        //only used for Logs of type RETRIEVED. In this case, field is mandatory
        @JsonProperty("geocacheReferenceCode")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String geocacheReferenceCode;

        @JsonProperty("logType")
        Integer logType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GCWebTrackableLogResponse extends GCWebLogBase {
        @JsonProperty("logType")
        GCWebLogTrackableResponseLogType logType;
    }

    //Contains common fields in JSONs related to Log request and response for both Caches and Trackables
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GCWebLogBase extends HttpResponse {

        // --- common request fields ---

        @JsonProperty("images")
        String[] images; //image GUIDs
        @JsonProperty("logDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtils.JSON_LOCAL_TIMESTAMP_PATTERN)
        Date logDate;
        @JsonProperty("logText")
        String logText;

        @JsonProperty("trackingCode")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String trackingCode; //used only for Trackable Logs

        // --- common response fields ---

        @JsonProperty("guid")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String guid;
        @JsonProperty("logReferenceCode")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String logReferenceCode;
        @JsonProperty("dateTimeCreatedUtc")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtils.JSON_LOCAL_TIMESTAMP_PATTERN)
        Date dateTimeCreatedUtc;
        @JsonProperty("dateTimeLastUpdatedUtc")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtils.JSON_LOCAL_TIMESTAMP_PATTERN)
        Date dateTimeLastUpdatedUtc;
        @JsonProperty("cannotDelete")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean cannotDelete;
        @JsonProperty("isArchived")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean isArchived;

    }

    // Response for GC Log Image create/update request:
    // {
    //    "guid": "c7xyz-xyz-xyz-xyz-xyz",
    //    "url": "https://img.geocaching.com/c7xyz-xyz-xyz-xyz-xyz.jpg",
    //    "thumbnailUrl": "https://img.geocaching.com/large/c7xyz-xyz-xyz-xyz-xyz.jpg",
    //    "success": true
    //}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GCWebLogImageResponse extends HttpResponse {
        @JsonProperty("guid")
        String guid;
        @JsonProperty("url")
        String url;
        @JsonProperty("thumbnailUrl")
        String thumbnailUrl;
        @JsonProperty("success")
        Boolean success;

    }

    //Helper JSOn subtypes

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GCWebLogTrackableResponseLogType {
        @JsonProperty("id")
        Integer id;
    }

    //matches a JSOn snippet like: {"id":123,"referenceCode":"GCxyz","name":"somename"}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GCGeocacheReference {
        @JsonProperty("id")
        Integer id;
        @JsonProperty("referenceCode")
        String referenceCode;
        @JsonProperty("name")
        String name;
    }


    //New Log API
    @NonNull
    @WorkerThread
    public static ImmutablePair<StatusCode, String> postLogInternal(final Geocache geocache,
                                                                    final LogType logType, final Date date,
                                                                    final String log, @NonNull final List<cgeo.geocaching.log.TrackableLog> trackables,
                                                                    final boolean addToFavorites) {

        final String geocode = geocache.getGeocode();
        //1.) Call log page and get a valid CSRF Token
        final String csrfToken = getCsrfTokenFromUrl(WEBSITE_URL + "/live/geocache/" + geocode + "/log");
        if (csrfToken == null) {
            //try old log flow
            Log.w("Log Post: unable to extract CSRF Token in new Log Flow Page");
            return postLog(geocache, logType, date, log, trackables, addToFavorites);
        }

        //2,) Fill Log Entry object and post it
        final GCWebLogRequest logEntry = new GCWebLogRequest();
        logEntry.images = new String[0];
        logEntry.logDate = date;
        logEntry.logType = logType.id;
        logEntry.logText = log;
        logEntry.trackables = CollectionStream.of(trackables).map(t -> {
            final GCWebLogTrackable tLog = new GCWebLogTrackable();
            tLog.trackableCode = t.geocode;
            tLog.trackableLogTypeId = t.action.gcApiId;
            return tLog;
        }).toArray(GCWebLogTrackable.class);
        logEntry.usedFavoritePoint = addToFavorites; //not used by web page, but seems to work

        final GCWebLogResponse response = websiteReq().uri("/api/live/v1/logs/" + geocode + "/geocacheLog")
                .method(HttpRequest.Method.POST)
                .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
                .bodyJson(logEntry)
                .requestJson(GCWebLogResponse.class).blockingGet();

        if (response.logReferenceCode == null) {
            return generateLogError(false, "Problem pasting log, response is: " + response);
        }

        return new ImmutablePair<>(StatusCode.NO_ERROR, response.logReferenceCode);
    }

    @WorkerThread
    @NonNull
    static ImmutablePair<StatusCode, String> postLogImage(final String geocode, final String logId, final Image image) {
        //1) Get CSRF Token from "Edit Log" page. URL is https://www.geocaching.com/live/log/GLxyz
        final String csrfToken = getCsrfTokenFromUrl(WEBSITE_URL + "/live/log/" + logId);
        if (csrfToken == null) {
            Log.w("Log Image Post: unable to extract CSRF Token in new Log Flow Page");
            return generateLogError(true, "No CSRFToken found");
        }

        //2) Create a new "image" attached to the log, uploading only image data
        //   (Do not yet upload name + description, for some reason this results in a server timeout)
        // via POST to https://www.geocaching.com/api/live/v1/logs/GLxyz/images with image payload
        final GCWebLogImageResponse imgResponse = websiteReq().uri("/api/live/v1/logs/" + logId + "/images")
                .method(HttpRequest.Method.POST)
                .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
                .bodyForm(null, "image", "image/jpeg", image.getFile())
                .requestJson(GCWebLogImageResponse.class).blockingGet();
        if (imgResponse.guid == null || imgResponse.url == null) {
            return generateLogError(true, "Problem posting log, response is: " + imgResponse);
        }

        //3) Post the image name + description via PUT
        // URL like: https://www.geocaching.com/api/live/v1/images/GLxyz/c7xyz-xyz-xyz-xyz-xyz/replace (PUT)
        final Parameters params = new Parameters();
        if (!StringUtils.isBlank(image.getTitle())) {
            params.put("name", image.getTitle());
        }
        if (!StringUtils.isBlank(image.getDescription())) {
            params.put("description", image.getDescription());
        }

        if (!params.isEmpty()) {
            //We can reuse same CSRF-Token in this second request
            final GCWebLogImageResponse putImgResponse = websiteReq().uri("/api/live/v1/images/" + logId + "/" + imgResponse.guid + "/replace")
                    .method(HttpRequest.Method.PUT)
                    .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
                    //.bodyForm(params, "image", "image/jpeg", image.getFile())
                    .bodyForm(params, null, null, null)
                    .requestJson(GCWebLogImageResponse.class).blockingGet();
            if (putImgResponse.url == null) {
                return generateLogError(true, "Problem putting image: " + putImgResponse);
            }
        }

        return new ImmutablePair<>(StatusCode.NO_ERROR, imgResponse.url);
    }

    public static ImmutablePair<StatusCode, String> postLogTrackable(final cgeo.geocaching.log.TrackableLog trackableLog, final Date date, final String log) {
        final String tbCode = trackableLog.geocode;

        //1) Get CSRF Token from Trackable "Edit Log" page. URL is https://www.geocaching.com/live/trackable/TBxyz/log
        final ImmutablePair<String, String> htmlAndCsrfToken = getHtmlAndCsrfTokenFromUrl(WEBSITE_URL + "/live/trackable/" + tbCode + "/log");
        final String csrfToken = htmlAndCsrfToken == null ? null : htmlAndCsrfToken.right;
        if (csrfToken == null) {
            //try old log flow
            Log.w("Log Trackable Post: unable to extract CSRF Token in new Log Flow Page");
            return null; // this will trigger trying old log flow
        }

        //1.5) see if we find a geocache reference in the HTML
        final String geocacheReferenceJson = TextUtils.getMatch(htmlAndCsrfToken.left, GCConstants.PATTERN_TB_CURRENT_GEOCACHE_JSON, null);
        String geocacheReferenceCode = null;
        if (geocacheReferenceJson != null) {
            final GCGeocacheReference gcRef = JsonUtils.readValueFailSilently("{" + geocacheReferenceJson + "}", GCGeocacheReference.class, null);
            if (gcRef != null) {
                geocacheReferenceCode = gcRef.referenceCode;
            }
        }


        //2,) Fill Trackable Log Entry object and post it
        //  Exemplary JSOn to send: {"images":[],"logDate":"2023-09-08T23:13:36.414Z","logText":"Write a note for a trackable","logType":4,"trackingCode":null}
        final GCWebTrackableLogRequest logEntry = new GCWebTrackableLogRequest();
        logEntry.images = new String[0];
        logEntry.logDate = date;
        logEntry.logType = trackableLog.action.gcApiId;
        logEntry.logText = log;
        logEntry.trackingCode = trackableLog.trackCode;

        //special case: if type is RETRIEVED, we need to fill reference code
        if (trackableLog.action == LogTypeTrackable.RETRIEVED_IT) {
            logEntry.geocacheReferenceCode = geocacheReferenceCode;
        }

        //URL: https://www.geocaching.com/api/live/v1/logs/TBxyz/trackableLog
        final GCWebTrackableLogResponse response = websiteReq().uri("/api/live/v1/logs/" + tbCode + "/trackableLog")
                .method(HttpRequest.Method.POST)
                .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
                .bodyJson(logEntry)
                .requestJson(GCWebTrackableLogResponse.class).blockingGet();

        if (response.logReferenceCode == null) {
            return generateLogError(false, "Problem pasting trackable log, response is: " + response);
        }

        return new ImmutablePair<>(StatusCode.NO_ERROR, response.logReferenceCode);

    }

    private static ImmutablePair<StatusCode, String> generateLogError(final boolean image, final String errorMsg) {
        Log.w((image ? "LOG IMAGE ERROR:" : "LOG ERROR:") + errorMsg);
        return new ImmutablePair<>(image ? StatusCode.LOGIMAGE_POST_ERROR : StatusCode.LOG_POST_ERROR, "");
    }

    private static String getCsrfTokenFromUrl(final String url) {
        final ImmutablePair<String, String> htmlAndUrl = getHtmlAndCsrfTokenFromUrl(url);
        return htmlAndUrl == null ? null : htmlAndUrl.right;
    }

    private static ImmutablePair<String, String> getHtmlAndCsrfTokenFromUrl(final String url) {

    final HttpResponse htmlResp =
                httpReq().uri(url).request().blockingGet();
        final String html = htmlResp.getBodyString();
        final String csrfToken = TextUtils.getMatch(html, GCConstants.PATTERN_CSRF_TOKEN, null);
        if (!htmlResp.isSuccessful() || csrfToken == null) {
            Log.w("Log Post: unable to find a CSRF Token in Log Page '" + url + "':" + htmlResp);
            return null;
        }
        return new ImmutablePair<>(html, csrfToken);
    }

    private static HttpRequest httpReq() {
        return new HttpRequest().requestPreparer(reqBuilder -> getCachedAuthorization().map(a -> {
            reqBuilder.addHeader("Authorization", a.getAuthorizationField());
            return reqBuilder;
        }));
    }

    private static HttpRequest websiteReq() {
        return httpReq().uriBase(WEBSITE_URL);
    }


    private static HttpRequest apiReq() {
        return httpReq().uriBase(API_URL);
    }
}
