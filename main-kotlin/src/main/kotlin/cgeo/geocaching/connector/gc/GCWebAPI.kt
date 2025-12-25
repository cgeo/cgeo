// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.gc

import cgeo.geocaching.SearchCacheData
import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.enumerations.CacheAttribute
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Units
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.network.HttpRequest
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.sorting.GeocacheSort
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.EnumValueMapper
import cgeo.geocaching.utils.Log
import cgeo.geocaching.connector.gc.GCAuthAPI.apiProxyReq

import androidx.annotation.NonNull
import androidx.annotation.WorkerThread

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.Date
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Set

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.reactivex.rxjava3.core.Single
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.tuple.ImmutablePair

/**
 * Handles geocaching.com web-api requests.
 * <br>
 * These are all HTTP endpoints with prefix {@link GCAuthAPI#API_PROXY_URL}.
 * This is not the official GC Live API.
 */
class GCWebAPI {


    /**
     * maximum number of elements to retrieve with one call
     */
    private static val MAX_TAKE: Int = 1000

    private GCWebAPI() {
        // Utility class, do not instantiate
    }

    /**
     * This class encapsulates, explains and mimics the search against gc.com WebApi at <a href="https://www.geocaching.com/api/proxy/web/search/v2">...</a>
     */
    public static class WebApiSearch {

        enum class class SortType {
            NAME("geocacheName", GeocacheSort.SortType.NAME),
            DISTANCE("distance", GeocacheSort.SortType.DISTANCE),
            FAVORITEPOINT("favoritePoint", GeocacheSort.SortType.FAVORITES, GeocacheSort.SortType.FAVORITES_RATIO),
            SIZE("containerSize", GeocacheSort.SortType.SIZE),
            DIFFICULTY("difficulty", GeocacheSort.SortType.DIFFICULTY),
            TERRAIN("terrain", GeocacheSort.SortType.TERRAIN),
            TRACKABLECOUNT("trackableCount", GeocacheSort.SortType.INVENTORY),
            HIDDENDATE("placeDate", GeocacheSort.SortType.HIDDEN_DATE),
            LASTFOUND("foundDate", GeocacheSort.SortType.LAST_FOUND)

            public final String keyword
            public final GeocacheSort.SortType[] cgeoSortTypes

            private static val CGEO_TO_GC_SORTTYPE: EnumValueMapper<GeocacheSort.SortType, SortType> = EnumValueMapper<>()

            static {
                for (SortType type : values()) {
                    CGEO_TO_GC_SORTTYPE.add(type, type.cgeoSortTypes)
                }
            }

            SortType(final String keyword, final GeocacheSort.SortType ... cgeoSortTypes) {
                this.keyword = keyword
                this.cgeoSortTypes = cgeoSortTypes
            }

            public static SortType getByCGeoSortType(final GeocacheSort.SortType cgeoSortType) {
                return CGEO_TO_GC_SORTTYPE.get(cgeoSortType, SortType.DISTANCE)
            }
        }

        private static val PARAM_DATE_FORMATTER: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private static val ONE_DAY_MILLISECONDS: Long = 24 * 60 * 60 * 1000

        private Viewport box
        private Geopoint origin

        private var statusOwn: Boolean = null
        private var statusFound: Boolean = null
        private var statusMembership: Boolean = null
        private var statusEnabled: Boolean = null
        private var statusCorrectedCoordinates: Boolean = null
        private var showArchived: Boolean = true

        private val cacheTypes: Set<CacheType> = HashSet<>()
        private val cacheSizes: Set<CacheSize> = HashSet<>()
        private val cacheAttributes: Set<CacheAttribute> = HashSet<>()

        private var hiddenBy: String = null
        private val notFoundBy: List<String> = ArrayList<>()
        private val foundBy: List<String> = ArrayList<>()
        private var difficulty: String = null
        private var terrain: String = null
        private var difficultyTerrainCombis: String = null
        private String placedFrom
        private String placedTo
        private String keywords
        private var minFavoritePoints: Int = -1

        private var deliverLastFoundDateOfFoundBy: Boolean = true

        private var sort: SortType = SortType.DISTANCE
        private var sortAsc: Boolean = true

        private var skip: Int = 0
        private var take: Int = 500

        public WebApiSearch setPage(final Int take, final Int skip) {
            this.take = take
            this.skip = skip
            return this
        }

        public WebApiSearch setSort(final SortType sort, final Boolean sortAsc) {
            this.sort = sort
            this.sortAsc = sortAsc
            return this
        }

        public Int getTake() {
            return take
        }

        public Int getSkip() {
            return skip
        }

        public SortType getSort() {
            return this.sort
        }

        public Boolean getSortAsc() {
            return this.sortAsc
        }

        /**
         * filters for given cache types. Works for V1
         */
        public WebApiSearch addCacheTypes(final Collection<CacheType> ct) {
            cacheTypes.addAll(CollectionStream.of(ct).filter(type -> type != CacheType.ALL).toList())
            return this
        }

        /**
         * filters for given cache sizes. Works for V1
         */
        public WebApiSearch addCacheSizes(final Collection<CacheSize> cs) {
            cacheSizes.addAll(cs)
            return this
        }

        /**
         * filters for given cache attriutes. Only positive attributes can be filtered, no exclude possible
         * TODO does not work with V1, only works with V2!
         */
        public WebApiSearch addCacheAttributes(final CacheAttribute... ca) {
            cacheAttributes.addAll(Arrays.asList(ca))
            return this
        }

        public Set<CacheAttribute> getCacheAttributes() {
            return this.cacheAttributes
        }

        /**
         * set to true to show ONLY own caches, false to HIDE own caches, null if both should be shown.
         * Works only for Premium members!
         * Works with V1
         */
        public WebApiSearch setStatusOwn(final Boolean statusOwn) {
            this.statusOwn = statusOwn
            return this
        }

        /**
         * set to true to show ONLY found caches, false to HIDE found caches, null if both should be shown.
         * Works only for Premium members!
         * Works with V1
         */
        public WebApiSearch setStatusFound(final Boolean statusFound) {
            this.statusFound = statusFound
            return this
        }

        /**
         * set to true to show ONLY basic caches, false show ONLY premium caches, null if both should be shown.
         */
        public WebApiSearch setStatusMembership(final Boolean statusMembership) {
            this.statusMembership = statusMembership
            return this
        }

        /**
         * set to true to show ONLY enabled caches, false show ONLY disabled caches, null if both should be shown.
         */
        public WebApiSearch setStatusEnabled(final Boolean statusEnabled) {
            this.statusEnabled = statusEnabled
            return this
        }

        /**
         * set to true to show archived caches. Note that gc.com respects this setting not in all cases
         */
        public WebApiSearch setShowArchived(final Boolean showArchived) {
            this.showArchived = showArchived
            return this
        }

        /**
         * set to true to show ONLY caches with original coordinates, false show ONLY caches with corrected coordinates, null if both should be shown.
         */
        public WebApiSearch setStatusCorrectedCoordinates(final Boolean statusCorrectedCoordinates) {
            this.statusCorrectedCoordinates = statusCorrectedCoordinates
            return this
        }

        /**
         * Works only if 'hiddenBy' is the exact owner name, also case muist match! Withs with V1
         */
        public WebApiSearch setHiddenBy(final String hiddenBy) {
            this.hiddenBy = hiddenBy
            return this
        }

        /**
         * Works only if 'notFoundBy' is the exact name of a geocache user. case does not need to match though. Works with V1
         */
        public WebApiSearch addNotFoundBy(final String notFoundBy) {
            this.notFoundBy.add(notFoundBy)
            return this
        }

        /**
         * Works only if 'notFoundBy' is the exact name of a geocache user. case does not need to match though. Works with V1
         */
        public WebApiSearch addFoundBy(final String foundBy) {
            this.foundBy.add(foundBy)
            return this
        }

        /**
         * set to a value > 0 to trigger search. Works with V1
         */
        public WebApiSearch setMinFavoritepoints(final Int minFavoritePoints) {
            this.minFavoritePoints = minFavoritePoints
            return this
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
                placedFrom = null
                placedTo = null
            } else if (from == null) {
                // -> before "to", set "placedTo" to one day AFTER
                placedFrom = null
                placedTo = PARAM_DATE_FORMATTER.format(Date(to.getTime() + ONE_DAY_MILLISECONDS))
            } else if (to == null) {
                // -> after "from", set "placedFrom" to one day BEFORE
                placedFrom = PARAM_DATE_FORMATTER.format(Date(from.getTime() - ONE_DAY_MILLISECONDS))
                placedTo = null
            } else {
                val fromBeforeTo: Boolean = from.before(to)
                placedFrom = PARAM_DATE_FORMATTER.format(fromBeforeTo ? from : to)
                placedTo = PARAM_DATE_FORMATTER.format(fromBeforeTo ? to : from)
            }
            return this
        }

        /**
         * Searches for keywords in cache name only. Search uses "contains" logic.
         * Must be whole word(s), e.g. "amburg" won't find caches with "Hamburg" in them.
         * In case multiple words are given they must occur in this order. E.g. "Hamburger Hafen" will not find "Hafen in Hamburg"
         * Is case insensitive
         */
        public WebApiSearch setKeywords(final String keywords) {
            this.keywords = keywords
            return this
        }

        /**
         * Sets the area to search in. Woirks with V1
         */
        public WebApiSearch setBox(final Viewport box) {
            this.box = box
            return this
        }

        /**
         * Sets the starting point of the search and the reference point for sort by distance. Does not restrict/filter the result. Works with V1
         */
        public WebApiSearch setOrigin(final Geopoint origin) {
            this.origin = origin
            return this
        }

        public Geopoint getOrigin() {
            return this.origin
        }

        /**
         * Works with V1
         */
        public WebApiSearch setDifficulty(final Float pFrom, final Float pTo) {
            this.difficulty = getRangeString(pFrom, pTo)
            return this
        }

        /**
         * Works with V1
         */
        public WebApiSearch setTerrain(final Float pFrom, final Float pTo) {
            this.terrain = getRangeString(pFrom, pTo)
            return this
        }

        // Example: m=1-4.5%2C2.5-4.5%2C3-5%2C4.5-5%2C5-3.5%2C5-4%2C5-4.5
        public WebApiSearch setDifficultyTerrainCombis(final Collection<ImmutablePair<Float, Float>> combis) {
            val sb: StringBuilder = StringBuilder()
            Boolean first = true
            for (ImmutablePair<Float, Float> combi : combis) {
                if (!first) {
                    sb.append(",")
                }
                first = false
                sb.append(getCombiString(combi.left, combi.right))
            }
            this.difficultyTerrainCombis = sb.toString()
            return this
        }

        /** If set to true and foundBy contains EXACTLY ONE element, then in the lastFound-field the date of the last found date of foundBy is returned instead of for the cache */
        public WebApiSearch setDeliverLastFoundDateOfFoundBy(final Boolean deliverLastFoundDateOfFoundBy) {
            this.deliverLastFoundDateOfFoundBy = deliverLastFoundDateOfFoundBy
            return this
        }

        /**
         * Returns a string specifying a range from 1-5 (in 0.5-steps) as used for parameters difficulty and terrain
         */
        private String getRangeString(final Float pFrom, final Float pTo) {
            if (pFrom == null && pTo == null) {
                return null
            }

            val from: Float = pFrom == null ? 1f : Math.round(Math.max(1, Math.min(5, pFrom)) * 2f) / 2f
            val to: Float = pTo == null ? 5f : Math.round(Math.max(1, Math.min(5, pTo)) * 2f) / 2f
            if (from > to) {
                return to + "-" + from
            }
            return from + "-" + to
        }

        private String getCombiString(final Float diff, final Float terrain) {
           return diff + "-" + terrain
        }

        @WorkerThread
        MapSearchResultSet execute() {
            val params: Parameters = Parameters()

            if (box != null) {
                // on empty viewport silently log stacktrace + return empty searchresult without calling search provider
                if (box.isJustADot()) {
                    try {
                        throw RuntimeException("searching map with empty viewport")
                    } catch (RuntimeException e) {
                        Log.d("searching map with empty viewport: " + ExceptionUtils.getStackTrace(e))
                    }
                    return MapSearchResultSet()
                }
                params.put("box", String.valueOf(this.box.getLatitudeMax()) + ',' + this.box.getLongitudeMin() +
                        ',' + this.box.getLatitudeMin() + ',' + this.box.getLongitudeMax())

                //6.4.24: seems like gc.com adds a strange "rad=16000" parameter to every box search. Always 16000, regardless of zoom level.
                // Don't know why yet...
                params.put("rad", "16000")

                //set origin to middle of viewport (will be overridden if origin is set explicitely later)
                params.put("origin", String.valueOf(this.box.getCenter().getLatitude()) + ',' + this.box.getCenter().getLongitude())
            }

            if (origin != null) {
                params.put("origin", String.valueOf(origin.getLatitude()) + ',' + origin.getLongitude())
            }

            if (!this.cacheTypes.isEmpty()) {
                params.put("ct", CollectionStream.of(this.cacheTypes).map(ct -> ct.wptTypeId).toJoinedString(","))
            }

            if (!this.cacheSizes.isEmpty()) {
                params.put("cs", CollectionStream.of(this.cacheSizes).filter(cs -> CacheSize.getGcIdsForSize(cs).length > 0)
                        .map(cs -> CollectionStream.of(ArrayUtils.toObject(CacheSize.getGcIdsForSize(cs))).toJoinedString(",")).toJoinedString(","))
            }

            if (!this.cacheAttributes.isEmpty()) {
                params.put("att", CollectionStream.of(this.cacheAttributes).map(ct -> ct.gcid).toJoinedString(","))
            }

            //Hide owned/hide found caches, only works for premium members
            if (this.statusOwn != null) {
                params.put("ho", this.statusOwn ? "0" : "1")
            }

            if (this.statusFound != null) {
                params.put("hf", this.statusFound ? "0" : "1")
            }

            if (this.statusMembership != null) {
                params.put("sp", this.statusMembership ? "0" : "1")
            }

            if (this.statusEnabled != null) {
                params.put("sd", this.statusEnabled ? "0" : "1")
            }
            if (this.showArchived) {
                params.put("sa", "1")
            }

            if (this.statusCorrectedCoordinates != null) {
                params.put("cc", this.statusCorrectedCoordinates ? "0" : "1")
            }

            if (this.hiddenBy != null) {
                params.put("hb", this.hiddenBy)
            }

            for (String notFoundBy : this.notFoundBy) {
                params.put("nfb", notFoundBy)
            }

            for (String foundBy : this.foundBy) {
                params.put("fb", foundBy)
            }

            if (this.minFavoritePoints > 0) {
                params.put("fp", "" + this.minFavoritePoints)
            }

            if (this.difficulty != null) {
                params.put("d", this.difficulty)
            }

            if (this.terrain != null) {
                params.put("t", this.terrain)
            }

            if (this.difficultyTerrainCombis != null) {
                params.put("m", this.difficultyTerrainCombis)
            }

            if (this.placedFrom != null || this.placedTo != null) {
                // after: pad
                // between: psd - ped
                // before: pbd
                // on: pod (not used by us)
                if (this.placedFrom == null) {
                    params.put("pbd", this.placedTo)
                } else if (this.placedTo == null) {
                    params.put("pad", this.placedFrom)
                } else {
                    params.put("psd", this.placedFrom)
                    params.put("ped", this.placedTo)
                }
            }

            if (this.keywords != null) {
                params.put("cn", this.keywords)
            }

            //special
            //6.4.24: seems like "properties=callernote" is now ALWAYS set
            //if (deliverLastFoundDateOfFoundBy && foundBy.size() == 1) {
            params.put("properties", "callernote")
            //}

            //paging / result size
            params.put("take", "" + take)
            params.put("skip", "" + skip)

            //sort
            if (sort != null) {
                params.put("sort", sort.keyword)
                if (sort == SortType.DISTANCE && this.box == null) {
                    //to sort by distance we need to set an origin of distance measurement
                    //6.4.24: gc.com seems to avoid dorigin param when box is set. So let's avoid it too...
                    val dOrigin: Geopoint = origin != null ? origin : LocationDataProvider.getInstance().currentGeo().getCoords()
                    params.put("dorigin", String.valueOf(dOrigin.getLatitude()) + ',' + dOrigin.getLongitude())
                }
                params.put("asc", "" + sortAsc)
            }


            //ALWAYS send cgeo as an identifier
            params.put("app", "cgeo"); //identify us towards Groundspeak due to gentlemens agreement

            val request: HttpRequest = apiProxyReq().uri("/web/search/v2").uriParams(params)
            Log.iForce("GCWEBAPI: request: " + request.getRequestUrl())
            return request.requestJson(MapSearchResultSet.class).blockingGet()
        }

        public Unit fillSearchCacheData(final SearchCacheData searchCacheData) {
            searchCacheData.addFoundBy(foundBy)
            searchCacheData.addNotFoundBy(notFoundBy)
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackableLog {
        @JsonProperty("logType") final TrackableLogType logType
        @JsonProperty("date") final String date
        @JsonProperty("geocache") final Geocache geocache
        @JsonProperty("referenceCode") final String referenceCode

        TrackableLog(final String logTypeId, final String date, final String geocode, final String referenceCode) {
            this.logType = TrackableLogType(logTypeId)
            this.date = date
            this.geocache = Geocache(geocode)
            this.referenceCode = referenceCode
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class TrackableLogType {
            @JsonProperty("id") final String id

            TrackableLogType(final String id) {
                this.id = id
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        static class Geocache {
            @JsonProperty("gcCode") final String geocode

            Geocache(final String geocode) {
                this.geocode = geocode
            }
        }

    }

    /*
     * [{"referenceCode":"TB....","iconUrl":"http://www.geocaching.com/images/wpttypes/4433.gif","name":"Some-Geocoin","distanceTraveledInMiles":13350.6100050281,"distanceTraveledInKilometers":21485.7241079319425664,"currentGoal":"Goal of the owner.","description":"","dateReleased":"2011-08-31T12:00:00","locationReleased":{"state":"Hessen","country":"Germany","isoCountryCode":"DE"},"allowedToBeCollected":true,"owner":{"avatarUrl":"https://img.geocaching.com/avatar/...jpg","membershipTypeId":3,"code":"PR...","publicGuid":"...","userName":"..."},"holder":{"avatarUrl":"https://img.geocaching.com/avatar/...jpg","membershipTypeId":3,"code":"PR...","publicGuid":"...","userName":"..."},"inHolderCollection":false,"isMissing":false,"isActive":true,"isLocked":false,"journeyStepsCount":1638,"ownerImagesCount":0,"activityImagesCount":0,"activityCount":1688,"trackingNumber":"...","trackingNumberSha512Hash":"...","trackableType":{"id":4433,"name":"...-Geocoin","imageName":"4433.gif"}}]
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackableInventoryEntry {
        @JsonProperty("referenceCode")
        String referenceCode; // The public one, starting with "TB"
        @JsonProperty("name")
        String name
        @JsonProperty("iconUrl")
        String iconUrl
        @JsonProperty("trackingNumber")
        String trackingNumber; // The secret one
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class CacheOwner {
        @JsonProperty("code")
        String code
        @JsonProperty("username")
        String username
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MapSearchResultSet {
        @JsonProperty("results")
        List<MapSearchResult> results
        @JsonProperty("total")
        Int total
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
    static class MapSearchResult {
        @JsonProperty
        Int id
        @JsonProperty("name")
        String name
        @JsonProperty("code")
        String code
        @JsonProperty("premiumOnly")
        Boolean premiumOnly
        @JsonProperty("favoritePoints")
        Int favoritePoints
        @JsonProperty("geocacheType")
        Int geocacheType
        @JsonProperty("containerType")
        Int containerType
        @JsonProperty("difficulty")
        Float difficulty
        @JsonProperty("terrain")
        Float terrain
        @JsonProperty("userFound")
        Boolean userFound
        @JsonProperty("userDidNotFind")
        Boolean userDidNotFind
        @JsonProperty("cacheStatus")
        Int cacheStatus
        @JsonProperty("postedCoordinates")
        PostedCoordinates postedCoordinates
        @JsonProperty("userCorrectedCoordinates")
        PostedCoordinates userCorrectedCoordinates
        @JsonProperty("detailsUrl")
        String detailsUrl
        @JsonProperty("hasGeotour")
        Boolean hasGeotour
        @JsonProperty("hasLogDraft")
        Boolean hasLogDraft
        @JsonProperty("placedDate")
        Date placedDate
        @JsonProperty("owner")
        CacheOwner owner
        @JsonProperty("lastFoundDate")
        Date lastFoundDate
        @JsonProperty("trackableCount")
        Int trackableCount
        @JsonProperty("region")
        String region
        @JsonProperty("country")
        String country
        @JsonProperty("attributes")
        List<Attribute> attributes
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Attribute {
        @JsonProperty("id")
        Int id
        @JsonProperty("name")
        String name
        @JsonProperty("isApplicable")
        Boolean isApplicable
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PostedCoordinates {
        @JsonProperty("latitude")
        Double latitude
        @JsonProperty("longitude")
        Double longitude

        PostedCoordinates(final Double latitude, final Double longitude) {
            this.latitude = latitude
            this.longitude = longitude
        }

        PostedCoordinates() {
        }

        Geopoint toCoords() {
            return Geopoint(latitude, longitude)
        }
    }

    @WorkerThread
    static SearchResult searchCaches(final IConnector con, final WebApiSearch search) {
        val result: SearchResult = SearchResult()

        try {

            val mapSearchResultSet: MapSearchResultSet = search.execute()
            result.setLeftToFetch(con, mapSearchResultSet.total - search.getTake() - search.getSkip())
            result.setPartialResult(con, search.getTake() > 0 && mapSearchResultSet.results.size() == search.getTake())
            val foundCaches: List<Geocache> = ArrayList<>()

            if (mapSearchResultSet.results != null) {
                for (final GCWebAPI.MapSearchResult r : mapSearchResultSet.results) {

                    val cacheCoord: Geopoint = r.postedCoordinates == null ? null : Geopoint(r.postedCoordinates.latitude, r.postedCoordinates.longitude)

                    val c: Geocache = Geocache()
                    c.setDetailed(false)
                    c.setGeocode(r.code)
                    c.setName(r.name)
                    if (r.userCorrectedCoordinates != null) {
                        c.setCoords(Geopoint(r.userCorrectedCoordinates.latitude, r.userCorrectedCoordinates.longitude))
                        c.setUserModifiedCoords(true)
                    } else if (cacheCoord != null) {
                        c.setCoords(cacheCoord)
                        c.setUserModifiedCoords(false)
                    } else {
                        //this can only happen for PREMIUM caches when searched by BASIC members.
                        //Open issue: what to do with those?
                        c.setCoords(null)
                    }
                    c.setType(CacheType.getByWaypointType(Integer.toString(r.geocacheType)))
                    c.setDifficulty(r.difficulty)
                    c.setTerrain(r.terrain)
                    c.setSize(CacheSize.getByGcId(r.containerType))
                    c.setPremiumMembersOnly(r.premiumOnly)
                    c.setHidden(r.placedDate)
                    c.setLastFound(r.lastFoundDate)
                    c.setInventoryItems(r.trackableCount)
                    c.setLocation(r.region + ", " + r.country)

                    //Only set found if the map returns a "found",
                    //the map API will possibly lag behind and break
                    //cache merging if "not found" is set
                    if (r.userFound) {
                        c.setFound(true)
                    } else if (r.userDidNotFind) {
                        c.setDNF(true)
                    }

                    c.setFavoritePoints(r.favoritePoints)
                    c.setDisabled(r.cacheStatus == 1)
                    c.setArchived(r.cacheStatus == 2)
                    if (r.owner != null) {
                        c.setOwnerDisplayName(r.owner.username)
                        c.setOwnerUserId(r.owner.username)
                    }

                    // parse attributes
                    val attributes: List<String> = ArrayList<>()
                    if (r.attributes != null) {
                        for (Attribute attribute : r.attributes) {
                            attributes.add(CacheAttribute.getById(attribute.id).getValue(attribute.isApplicable))
                        }
                    }
                    c.setAttributes(attributes)

                    foundCaches.add(c)
                }

            }

            tryGuessMissingDistances(foundCaches, search)

            result.addAndPutInCache(foundCaches)
        } catch (RuntimeException re) {
            Log.w("GCWebAPI: problem executing search", re)
            result.setError(GCConnector.getInstance(), StatusCode.COMMUNICATION_ERROR)
        }

        return result
    }

    /**
     * For BASIC members, PREMIUM caches don't contain coordinates. This helper methods guesses distances for those caches
     */
    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity"})
    private static Unit tryGuessMissingDistances(final List<Geocache> caches, final WebApiSearch search) {
        if (caches == null || caches.isEmpty()) {
            return
        }
        //This heuristic only works if origin is given and sort is of type DISTANCE
        if (search.getOrigin() == null || search.getSort() != WebApiSearch.SortType.DISTANCE) {
            return
        }

        //inverse the list in case of inverse sort
        List<Geocache> loopCaches = caches
        if (!search.getSortAsc()) {
            loopCaches = ArrayList<>(caches)
            Collections.reverse(loopCaches)
        }

        //This heuristic will assign each cache without coordinates the middle of the distance of two surrounding caches with known coordinates
        //    to given pos
        //All caches AFTER the last cache with know coords will get assigend its distance to pos plus 1
        //If ALL caches have no coordinates, they get assigned a distance of 1
        Float lastDistance = 0
        val emptyCoordCaches: List<Geocache> = ArrayList<>()

        for (Geocache c : loopCaches) {
            if (c.getCoords() != null) {
                val newDistance: Float = search.getOrigin().distanceTo(c.getCoords())
                for (Geocache emptyC : emptyCoordCaches) {
                    emptyC.setDistance(Units.generateSmartRoundedAverageDistance(newDistance, lastDistance))
                }
                emptyCoordCaches.clear()
                lastDistance = newDistance
            } else {
                emptyCoordCaches.add(c)
            }
        }

        if (!emptyCoordCaches.isEmpty()) {
            for (Geocache emptyC : emptyCoordCaches) {
                emptyC.setDistance(lastDistance == 0 ? 1 : lastDistance + 1)
            }
        }
    }

    /*
     * https://www.geocaching.com/api/proxy/trackables?inCollection=false&skip=0&take=50
     */
    @WorkerThread
    static List<TrackableInventoryEntry> getTrackableInventory(final Int totalTrackables) {
        val trackableInventoryEntries: List<TrackableInventoryEntry> = ArrayList<>()
        Int skip = 0
        TrackableInventoryEntry[] entries
        do {
            entries = apiProxyReq().uri("/trackables?inCollection=false&inInventory=true&take=" + MAX_TAKE + "&skip=" + skip).requestJson(TrackableInventoryEntry[].class).blockingGet()
            //entries = getAPI("/trackables?inCollection=false&take=" + MAX_TAKE + "&skip=" + skip, TrackableInventoryEntry[].class).blockingGet()
            trackableInventoryEntries.addAll(Arrays.asList(entries))
            skip += MAX_TAKE
        } while (trackableInventoryEntries.size() < totalTrackables)
        return trackableInventoryEntries
    }

    /*
     * https://www.geocaching.com/api/proxy/web/v1/users/PR.../availablefavoritepoints
     */
    static Single<Integer> getAvailableFavoritePoints(final String profile) {
        return apiProxyReq().uri("/web/v1/users/" + profile + "/availablefavoritepoints").requestJson(Integer.class)
        //return getAPI("/web/v1/users/" + profile + "/availablefavoritepoints", Integer.class)
    }

    public static Collection<ImmutablePair<Float, Float>> getNeededDifficultyTerrainCombisFor81Matrix() {
        // Request URI: see code below
        // Answer is a json string array, something like: ["1-4.5","2.5-4.5","3-5","4.5-5","5-3.5","5-4","5-4.5"]

        final String[] rawCombis = apiProxyReq().uri("/web/v1/statistics/difficultyterrainmatrix/needed")
                .requestJson(String[].class).blockingGet()
        if (rawCombis == null || rawCombis.length == 0) {
            return Collections.emptyList()
        }
        final List<ImmutablePair<Float, Float>> result = ArrayList<>(rawCombis.length)
        try {
            for (String rawCombi : rawCombis) {
                final String[] parts = rawCombi.split("-")
                val difficulty: Float = Float.parseFloat(parts[0])
                val terrain: Float = Float.parseFloat((parts[1]))
                result.add(ImmutablePair<>(difficulty, terrain))
            }
        } catch (Exception ex) {
            Log.w("Problems parsing as list of dt-combis: " + Arrays.asList(rawCombis))
        }
        return result
    }
}
