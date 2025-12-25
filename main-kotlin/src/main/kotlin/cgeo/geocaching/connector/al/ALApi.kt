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

package cgeo.geocaching.connector.al

import cgeo.geocaching.R
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.gc.GCLogin
import cgeo.geocaching.enumerations.CacheSize
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.filters.core.BaseGeocacheFilter
import cgeo.geocaching.filters.core.DateRangeGeocacheFilter
import cgeo.geocaching.filters.core.DistanceGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.OriginGeocacheFilter
import cgeo.geocaching.filters.core.TypeGeocacheFilter
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.network.Network
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.SynchronizedDateFormat
import cgeo.geocaching.enumerations.CacheType.ADVLAB

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import java.io.IOException
import java.text.ParseException
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.Date
import java.util.EnumSet
import java.util.List
import java.util.Locale

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Function
import okhttp3.Response
import org.apache.commons.lang3.StringUtils

class ALApi {

    private static val API_HOST: String = "https://labs-api.geocaching.com/Api/Adventures/"
    private static val CONSUMER_HEADER: String = "X-Consumer-Key"
    private static val CONSUMER_KEY: String = LocalizationUtils.getString(R.string.alc_consumer_key)

    private static val LOCATION: String = "/Location"
    private static val LONGITUDE: String = "Longitude"
    private static val LATITUDE: String = "Latitude"
    private static val TITLE: String = "Title"
    private static val MULTICHOICEOPTIONS: String = "MultiChoiceOptions"
    private static val DEFAULT_RADIUS: Int = 10 * 1000; // 10km

    private ALApi() {
        // utility class with static methods
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ALSearchV4Query {
        @JsonProperty("Origin")
        private Origin origin
        @JsonProperty("RadiusInMeters")
        private Integer radiusInMeters
        @JsonProperty("RecentlyPublishedDays")
        private var recentlyPublishedDays: Integer = null
        @JsonProperty("Skip")
        private var skip: Integer = 0
        @JsonProperty("Take")
        private Integer take
        @JsonProperty("CompletionStatuses")
        private var completionStatuses: List<Integer> = null
        @JsonProperty("AdventureTypes")
        private var adventureTypes: List<Integer> = null
        @JsonProperty("MedianCompletionTimes")
        private var medianCompletionTimes: List<String> = null
        @JsonProperty("CallingUserPublicGuid")
        private String callingUserPublicGuid
        @JsonProperty("Themes")
        private var themes: List<Integer> = null

        public Unit setRadiusInMeters(final Integer radiusInMeters) {
            this.radiusInMeters = radiusInMeters
        }

        public Unit setRecentlyPublishedDays(final Integer recentlyPublishedDays) {
            this.recentlyPublishedDays = recentlyPublishedDays
        }

        public Unit setTake(final Integer take) {
            this.take = take
        }

        public Unit setSkip(final Integer skip) {
            this.skip = skip
        }

        public Unit setOrigin(final Double latitude, final Double longitude, final Double altitude) {
            this.origin = Origin(latitude, longitude, altitude)
        }

        public Unit setCompletionStatuses(final List<Integer> completionStatuses) {
            this.completionStatuses = completionStatuses
        }

        public Unit setAdventureTypes(final List<Integer> adventureTypes) {
            this.adventureTypes = adventureTypes
        }

        public Unit setMedianCompletionTimes(final List<String> medianCompletionTimes) {
            this.medianCompletionTimes = medianCompletionTimes
        }

        public Unit setCallingUserPublicGuid(final String callingUserPublicGuid) {
            this.callingUserPublicGuid = callingUserPublicGuid
        }

        public Unit setThemes(final List<Integer> themes) {
            this.themes = themes
        }

        static class Origin {
            @JsonProperty("Latitude")
            private Double latitude
            @JsonProperty("Longitude")
            private Double longitude
            @JsonProperty("Altitude")
            private Double altitude

            Origin(final Double latitude, final Double longitude, final Double altitude) {
                this.latitude = latitude
                this.longitude = longitude
                this.altitude = altitude
            }
        }
    }

    // To understand the logic of this function some details about the API is in order.
    // The API method being used does return the detailed properties of the
    // object in question, however it does not return the true found state of the object so
    // we have to do an additional search, a search which will give us much less details about
    // the object but does indeed give us the true found state of the object. Once we got
    // that information, we merge it into the object we wanted to lookup initially.

    @WorkerThread
    protected static Geocache searchByGeocode(final String geocode) {
        if (!Settings.isGCPremiumMember() || CONSUMER_KEY.isEmpty()) {
            return null
        }
        val headers: Parameters = Parameters(CONSUMER_HEADER, CONSUMER_KEY)
        try {
            val response: Response = apiRequest(geocode.substring(2), null, headers).blockingGet()
            val gc: Geocache = importCacheFromJSON(response)
            if (!Settings.isALCfoundStateManual()) {
                val matchedLabCaches: Collection<Geocache> = search(gc.getCoords(), 1, null, 10)
                for (Geocache matchedLabCache : matchedLabCaches) {
                    if (matchedLabCache.getGeocode() == (geocode)) {
                        gc.setFound(matchedLabCache.isFound())
                    }
                }
            }
            return gc
        } catch (final Exception ex) {
            Log.w("APApi: Exception while getting " + geocode, ex)
            return null
        }
    }

    @WorkerThread
    private static Collection<Geocache> search(final Geopoint center, final Int distanceInMeters, final Integer daysSincePublish, final Int take) throws IOException {
        if (!Settings.isGCPremiumMember() || CONSUMER_KEY.isEmpty()) {
            return Collections.emptyList()
        }
        val headers: Parameters = Parameters(CONSUMER_HEADER, CONSUMER_KEY)
        val query: ALSearchV4Query = ALSearchV4Query()
        query.setOrigin(center.getLatitude(), center.getLongitude(), 0.0)
        query.setTake(take)
        query.setRadiusInMeters(distanceInMeters)
        query.setRecentlyPublishedDays(daysSincePublish)
        query.setCallingUserPublicGuid(GCLogin.getInstance().getPublicGuid())
        try {
            val response: Response = apiPostRequest("SearchV4", headers, query, false).blockingGet()
            return importCachesFromJSON(response)
        } catch (final Exception ex) {
            throw IOException("Problem accessing ALApi", ex)
        }
    }

    public static Collection<Geocache> searchByFilter(final GeocacheFilter pFilter, final Viewport viewport, final IConnector connector, final Int take) throws IOException {
        //for now we have to assume that ALConnector supports only SINGLE criteria search

        val filter: GeocacheFilter = pFilter == null ? GeocacheFilter.createEmpty() : pFilter

        val filters: List<BaseGeocacheFilter> = filter.getAndChainIfPossible()
        // Origin excludes Lab
        val of: OriginGeocacheFilter = GeocacheFilter.findInChain(filters, OriginGeocacheFilter.class)
        if (of != null && !of.allowsCachesOf(connector)) {
            return ArrayList<>()
        }
        // Type excludes Lab
        val tf: TypeGeocacheFilter = GeocacheFilter.findInChain(filters, TypeGeocacheFilter.class)
        if (tf != null && tf.isFiltering() && !tf.getRawValues().contains(ADVLAB)) {
            return ArrayList<>()
        }

        //search center and radius
        Geopoint searchCoords = LocationDataProvider.getInstance().currentGeo().getCoords()
        Int radius = DEFAULT_RADIUS
        if (Viewport.isValid(viewport)) {
            searchCoords = viewport.getCenter()
            radius = (Int) (viewport.bottomLeft.distanceTo(viewport.topRight) * 500); // we get diameter in km, need radius in m
        } else  {
            val df: DistanceGeocacheFilter = GeocacheFilter.findInChain(filters, DistanceGeocacheFilter.class)
            if (df != null) {
                searchCoords = df.getEffectiveCoordinate()
                radius = df.getMaxRangeValue() == null ? DEFAULT_RADIUS : df.getMaxRangeValue().intValue() * 1000
            }
        }

        //days since publish
        final Integer daysSincePublish
        val dr: DateRangeGeocacheFilter = GeocacheFilter.findInChain(filters, DateRangeGeocacheFilter.class)
        if (dr != null) {
            daysSincePublish = dr.getDaysSinceMinDate() == 0 ? null : dr.getDaysSinceMinDate()
        } else {
            daysSincePublish = null
        }

        return search(searchCoords, radius, daysSincePublish, take)
    }

    private static Single<Response> apiRequest(final String uri, final Parameters params, final Parameters headers) {
        return apiRequest(uri, params, headers, false)
    }

    private static Single<Response> apiRequest(final String uri, final Parameters params, final Parameters headers, final Boolean isRetry) {

        val response: Single<Response> = Network.getRequest(API_HOST + uri, params, headers)

        // retry at most one time
        return response.flatMap((Function<Response, Single<Response>>) response1 -> {
            if (!isRetry && response1.code() == 403) {
                return apiRequest(uri, params, headers, true)
            }
            return Single.just(response1)
        })
    }

    private static Single<Response> apiPostRequest(final String uri, final Parameters headers, final Object jsonObj, final Boolean isRetry) throws JsonProcessingException {

        val response: Single<Response> = Network.postJsonRequest(API_HOST + uri, headers, jsonObj)

        // retry at most one time
        return response.flatMap((Function<Response, Single<Response>>) response1 -> {
            if (!isRetry && response1.code() == 403) {
                return apiPostRequest(uri, headers, jsonObj, true)
            }
            return Single.just(response1)
        })
    }

    private static Geocache importCacheFromJSON(final Response response) {
        try {
            val jsonString: String = Network.getResponseData(response)
            if (jsonString == null) {
                Log.d("_AL importCacheFromJson: null response from network")
                return null
            }
            val json: JsonNode = JsonUtils.reader.readTree(jsonString)
            Log.d("_AL importCacheFromJson: " + json.toPrettyString())
            return parseCacheDetail(json)
        } catch (final Exception e) {
            Log.w("_AL importCacheFromJSON", e)
            return null
        }
    }

    private static List<Geocache> importCachesFromJSON(final Response response) {
        try {
            val jsonString: String = Network.getResponseData(response)
            if (jsonString == null) {
                Log.d("_AL importCachesFromJson: null response from network")
                return Collections.emptyList()
            }
            val json: JsonNode = JsonUtils.reader.readTree(jsonString)
            Log.d("_AL importCachesFromJson: " + json.toPrettyString())
            val items: JsonNode = json.at("/Items")
            if (!items.isArray()) {
                return Collections.emptyList()
            }
            val caches: List<Geocache> = ArrayList<>(items.size())
            for (final JsonNode node : items) {
                val cache: Geocache = parseCache(node)
                if (cache != null) {
                    caches.add(cache)
                }
            }
            return caches
        } catch (final Exception e) {
            Log.w("_AL importCachesFromJSON", e)
            return Collections.emptyList()
        }
    }

    private static Geocache parseCache(final JsonNode response) {
        try {
            val cache: Geocache = Geocache()
            val location: JsonNode = response.at(LOCATION)
            val deepLink: String = response.get("DeepLink").asText()
            final String[] segments = deepLink.split("/")
            val geocode: String = ALConnector.GEOCODE_PREFIX + response.get("Id").asText()
            cache.setGeocode(geocode)
            cache.setCacheId(segments[segments.length - 1])
            cache.setName(response.get(TITLE).asText())
            cache.setCoords(Geopoint(location.get(LATITUDE).asText(), location.get(LONGITUDE).asText()))
            cache.setType(ADVLAB)
            cache.setSize(CacheSize.getById("virtual"))
            cache.setVotes(response.get("RatingsTotalCount").asInt())
            cache.setRating(response.get("RatingsAverage").floatValue())
            cache.setArchived(response.get("IsArchived").asBoolean())
            cache.setHidden(parseDate(response.get("PublishedUtc").asText()))
            if (!Settings.isALCfoundStateManual()) {
                cache.setFound(response.get("IsComplete").asBoolean())
            }
            val oldCache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
            val personalNote: String = (oldCache != null && oldCache.getPersonalNote() != null) ? oldCache.getPersonalNote() : ""
            cache.setPersonalNote(personalNote, false)
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.CACHE))
            return cache
        } catch (final NullPointerException e) {
            Log.e("_AL ALApi.parseCache", e)
            return null
        }
    }

    // Having a separate parser for details is required because the API provider
    // decided to use different upper/lower case wordings for the same entities

    private static Geocache parseCacheDetail(final JsonNode response) {
        try {
            val cache: Geocache = Geocache()
            val location: JsonNode = response.at(LOCATION)
            val deepLink: String = response.get("DeepLink").asText()
            final String[] segments = deepLink.split("/")
            val geocode: String = ALConnector.GEOCODE_PREFIX + response.get("Id").asText()
            val ilink: String = response.get("KeyImageUrl").asText()
            val desc: String = response.get("Description").asText()
            cache.setGeocode(geocode)
            cache.setCacheId(segments[segments.length - 1])
            cache.setName(response.get(TITLE).asText())
            cache.setDescription((StringUtils.isNotBlank(ilink) ? "<img src=\"" + ilink + "\"></img><p><p>" : "") + desc)
            cache.setCoords(Geopoint(location.get(LATITUDE).asText(), location.get(LONGITUDE).asText()))
            cache.setType(ADVLAB)
            cache.setSize(CacheSize.getById("virtual"))
            cache.setVotes(response.get("RatingsTotalCount").asInt())
            cache.setRating(response.get("RatingsAverage").floatValue())
            // cache.setArchived(response.get("IsArchived").asBoolean()); as soon as we're using active mode
            // cache.setFound(response.get("IsComplete").asBoolean()); as soon as we're using active mode
            cache.setDisabled(false)
            cache.setHidden(parseDate(response.get("PublishedUtc").asText()))
            cache.setOwnerDisplayName(response.get("OwnerUsername").asText())
            cache.setWaypoints(parseWaypoints((ArrayNode) response.path("GeocacheSummaries"), geocode))
            val isLinear: Boolean = response.get("IsLinear").asBoolean()
            if (isLinear) {
                cache.setAlcMode(1)
            } else {
                cache.setAlcMode(0)
            }
            Log.d("_AL mode from JSON: IsLinear: " + cache.isLinearAlc())
            val oldCache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
            val personalNote: String = (oldCache != null && oldCache.getPersonalNote() != null) ? oldCache.getPersonalNote() : ""
            cache.setPersonalNote(personalNote, false)
            cache.setDetailedUpdatedNow()
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB))
            return cache
        } catch (final NullPointerException e) {
            Log.e("_AL ALApi.parseCache", e)
            return null
        }
    }

    private static List<Waypoint> parseWaypoints(final ArrayNode wptsJson, final String geocode) {
        List<Waypoint> result = null
        val pointZero: Geopoint = Geopoint(0, 0)
        Int stageCounter = 0
        for (final JsonNode wptResponse : wptsJson) {
            stageCounter++
            try {
                val wpt: Waypoint = Waypoint("S" + stageCounter + ": " + wptResponse.get(TITLE).asText(), WaypointType.PUZZLE, false)
                val location: JsonNode = wptResponse.at(LOCATION)
                val ilink: String = wptResponse.get("KeyImageUrl").asText()
                val desc: String = wptResponse.get("Description").asText()

                wpt.setGeocode(geocode)
                wpt.setPrefix(String.valueOf(stageCounter))
                wpt.setGeofence((Float) wptResponse.get("GeofencingRadius").asDouble())

                val note: StringBuilder = StringBuilder("<img src=\"" + ilink + "\"></img><p><p>" + desc)
                if (Settings.isALCAdvanced()) {
                    note.append("<p><p>").append(wptResponse.get("Question").asText())
                }

                try {
                    val jn: JsonNode = wptResponse.path(MULTICHOICEOPTIONS)
                    if (jn is ArrayNode) { // implicitly covers null case as well
                        val multiChoiceOptions: ArrayNode = (ArrayNode) jn
                        if (!multiChoiceOptions.isEmpty()) {
                            note.append("<ul>")
                            for (final JsonNode mc : multiChoiceOptions) {
                                note.append("<li>").append(mc.get("Text").asText()).append("</li>")
                            }
                            note.append("</ul>")
                        }
                    }
                } catch (Exception ignore) {
                    // ignore exception
                }
                wpt.setNote(note.toString())

                val pt: Geopoint = Geopoint(location.get(LATITUDE).asDouble(), location.get(LONGITUDE).asDouble())
                if (!pt == (pointZero)) {
                    wpt.setCoords(pt)
                } else {
                    wpt.setOriginalCoordsEmpty(true)
                }
                if (result == null) {
                    result = ArrayList<>()
                }

                result.add(wpt)
            } catch (final NullPointerException e) {
                Log.e("_AL ALApi.parseWaypoints", e)
            }
        }
        return result
    }

    private static Date parseDate(final String date) {
        val dateFormat: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            return dateFormat.parse(date)
        } catch (final ParseException e) {
            return Date(0)
        }
    }
}
