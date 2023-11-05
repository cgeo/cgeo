package cgeo.geocaching.connector.bettercacher;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointParser;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.bettercacher.Category;
import cgeo.geocaching.models.bettercacher.Tier;
import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/** Encapsulates the raw APi calls to bettercacher.org */
public final class BetterCacherAPI {

    // API format for search by geocode. Returns "stump data": only gccode, tier and categories.
    // - request example: https://api.bettercacher.org/cgeo/data?gccodes=XYZ123,ABC987,GC5HP25
    // - reply example:
    //   [
    //    {
    //        "gccode": "XYZ123",
    //        "error": true,
    //        "reason": "No data found for the provided gccode."
    //    },
    //    {
    //        "gccode": "ABC987",
    //        "error": true,
    //        "reason": "No data found for the provided gccode."
    //    },
    //    {
    //        "gccode": "GC5HP25",
    //        "tier": "GOLD",
    //        "attributes": [
    // "mystery",
    // "location",
    // "recommendation"
    //        ]
    //    }
    //]
    //
    //API format to search by coordinate. Returns "stump data": only gccode, tier and categories.
    // - request example: https://api.bettercacher.org/cgeo/location?coords=52.151421,9.922062,250
    //   (parameter coords is lat-center,lon-center,distance-in-km
    // - reply: same as for geocodes
    //
    //API format to search by geocode for "full data". Same URL as for "stump data" with additional flag
    // - request example: https://api.bettercacher.org/cgeo/data?gccodes=XYZ123,ABC987,GC5HP25
    // - reply example:
    // [
    //    {
    //        "gccode": "XYZ123",
    //        "error": true,
    //        "reason": "No data found for the provided gccode."
    //    },
    //    {
    //        "gccode": "ABC987",
    //        "error": true,
    //        "reason": "No data found for the provided gccode."
    //    },
    //    {
    //        "gccode": "GC5HP25",
    //        "name": "Vaters letzter Wille",
    //        "lat": "48.132984",
    //        "lng": "10.241067",
    //        "difficulty": "4.5",
    //        "terrain": "1.5",
    //        "type": "mystery",
    //        "size": "large",
    //        "country": "Germany",
    //        "state": "Bayern",
    //        "acceptedOn": "2015-07-12 17:22:45",
    //        "tier": "GOLD",
    //        "attributes": [
    //            "mystery",
    //            "location",
    //            "recommendation"
    //        ]
    //    }
    //]

    private static final String API_URL_STUMPS_FOR_GEOCODES = "https://api.bettercacher.org/cgeo/data?gccodes=";
    private static final String API_URL_STUMPS_FOR_COORDS = "https://api.bettercacher.org/cgeo/location?coords=";
    private static final String API_URL_FULLDATA_FOR_GEOCODES = "https://api.bettercacher.org/cgeo/data?fulldata=true&gccodes=";

    private static final Map<String, CacheType> CACHE_TYPE_MAP = new HashMap<>();
    private static final Map<String, CacheSize> CACHE_SIZE_MAP = new HashMap<>();
    private static final Map<String, Tier> CACHE_TIER_MAP = new HashMap<>();
    private static final Map<String, Category> CACHE_CATEGORY_MAP = new HashMap<>();

    static {
        //possible CacheType values: mystery, multi, traditional, wherigo, letter, earthcache
        CACHE_TYPE_MAP.put("mystery", CacheType.MYSTERY);
        CACHE_TYPE_MAP.put("multi", CacheType.MULTI);
        CACHE_TYPE_MAP.put("traditional", CacheType.TRADITIONAL);
        CACHE_TYPE_MAP.put("wherigo", CacheType.WHERIGO);
        CACHE_TYPE_MAP.put("letter", CacheType.LETTERBOX);
        CACHE_TYPE_MAP.put("earthcache", CacheType.EARTH);
        //possible CacheSize values: micro, small, regular, large, other, v (=virtual), not
        CACHE_SIZE_MAP.put("micro", CacheSize.MICRO);
        CACHE_SIZE_MAP.put("small", CacheSize.SMALL);
        CACHE_SIZE_MAP.put("regular", CacheSize.REGULAR);
        CACHE_SIZE_MAP.put("large", CacheSize.LARGE);
        CACHE_SIZE_MAP.put("other", CacheSize.OTHER);
        CACHE_SIZE_MAP.put("v", CacheSize.VIRTUAL);
        CACHE_SIZE_MAP.put("not", CacheSize.NOT_CHOSEN);
        //possible Tier values: blue, silver, gold
        CACHE_TIER_MAP.put("blue", Tier.BC_BLUE);
        CACHE_TIER_MAP.put("silver", Tier.BC_SILVER);
        CACHE_TIER_MAP.put("gold", Tier.BC_GOLD);
        //possible Category values: mystery, gadget, nature, hiking, location, other, recommendation
        CACHE_CATEGORY_MAP.put("mystery", Category.BC_MYSTERY);
        CACHE_CATEGORY_MAP.put("gadget", Category.BC_GADGET);
        CACHE_CATEGORY_MAP.put("nature", Category.BC_NATURE);
        CACHE_CATEGORY_MAP.put("hiking", Category.BC_HIKING);
        CACHE_CATEGORY_MAP.put("location", Category.BC_LOCATION);
        CACHE_CATEGORY_MAP.put("other", Category.BC_OTHER);
        CACHE_CATEGORY_MAP.put("recommendation", Category.BC_RECOMMENDATION);
    }

    //Return class for a cache from bettercacher.org. This class can hold both "stump data" and "full data"
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class BetterCacherCache {
        @JsonProperty("gccode")
        String gccode;

        // error data
        @JsonProperty("error")
        boolean error;
        @JsonProperty("reason")
        String reason;

        //stump data
        @JsonProperty("tier")
        String tier;

        @JsonProperty("attributes")
        String[] attributes;

        //full data

        // "name": "Vaters letzter Wille",
        @JsonProperty("name")
        String name;
        // "lat": "48.132984",
        @JsonProperty("lat")
        String lat;
        // "lng": "10.241067",
        @JsonProperty("lng")
        String lng;
        // "type": "unknown",
        @JsonProperty("type")
        String type;
        // "difficulty": "4.5",
        @JsonProperty("difficulty")
        String difficulty;
        // "terrain": "1.5",
        @JsonProperty("terrain")
        String terrain;
        // "size": "large",
        @JsonProperty("size")
        String size;
        // "state": "Germany",
        @JsonProperty("state")
        String state;
        // "country": "Bayern",
        @JsonProperty("country")
        String country;
        // "acceptedOn": "2015-07-12 17:22:45",
        @JsonProperty("acceptedOn")
        String acceptedOn;

        private boolean hasFullData;

        public void setFullData(final boolean hasFullData) {
            this.hasFullData = hasFullData;
        }

        public boolean hasFullData() {
            return hasFullData;
        }

        public List<Category> getCategories() {
            final List<Category> cats = new ArrayList<>();
            if (attributes != null) {
                for (String att : attributes) {
                    final Category cat = getFromJson(CACHE_CATEGORY_MAP, att, null);
                    if (Category.isValid(cat)) {
                        cats.add(cat);
                    }
                }
            }
            return cats;
        }

        public Tier getTier() {
            return getFromJson(CACHE_TIER_MAP, tier, Tier.NONE);
        }

        public Geocache createGeocache() {
            if (error || StringUtils.isBlank(gccode)) {
                return null;
            }
            final Geocache cache = new Geocache();
            //set stump data
            cache.setGeocode(gccode);
            cache.setCategories(getCategories());
            cache.setTier(getTier());

            if (!hasFullData()) {
                return cache;
            }

            //set other data
            // "name": "Vaters letzter Wille",
            cache.setName(name);
            // "lat": "48.132984",
            // "lng": "10.241067",
            cache.setCoords(GeopointParser.parse(lat + " " + lng, null));
            // "type": "unknown",
            cache.setType(getFromJson(CACHE_TYPE_MAP, type, CacheType.UNKNOWN));
            // "difficulty": "4.5",
            cache.setDifficulty(getNumberFromJson(difficulty, 0));
            // "terrain": "1.5",
            cache.setTerrain(getNumberFromJson(terrain, 0));
            // "size": "large", possible values are: micro, small, regular, large, other, v
            cache.setSize(getFromJson(CACHE_SIZE_MAP, size, CacheSize.UNKNOWN));
            // "state": "Germany",
            // "country": "Bayern",
            cache.setLocation(getLocation(state, country));

            //Not used:
            // "acceptedOn": "2015-07-12 17:22:45",

            return cache;
        }

        private static <T> T getFromJson(final Map<String, T> map, final String jsonValue, final T defaultValue) {
            if (StringUtils.isBlank(jsonValue)) {
                return defaultValue;
            }
            final T mappedValue = map.get(jsonValue.toLowerCase(Locale.US).trim());
            return mappedValue == null ? defaultValue : mappedValue;
        }

        private static float getNumberFromJson(final String jsonValue, final float defaultValue) {
            if (StringUtils.isBlank(jsonValue)) {
                return defaultValue;
            }
            final String value = jsonValue.trim().replace('_', '.').replace(',', '.');
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException nfe) {
                Log.d("BettercacherAPI: could not parse value as float: '" + jsonValue + "'");
                return defaultValue;
            }
        }

        private static String getLocation(final String state, final String country) {
            final boolean stateIsBlank = StringUtils.isBlank(state);
            final boolean countryIsBlank = StringUtils.isBlank(country);
            if (stateIsBlank) {
                return countryIsBlank ? "" : country.trim();
            }
            return countryIsBlank ? state.trim() : state.trim() + ", " + country.trim();
        }
    }

    private BetterCacherAPI() {
        //no instance
    }

    public static Map<String, BetterCacherCache> getCacheStumpsForViewport(@NonNull final Viewport viewport) {
        final Geopoint corner = new Geopoint(viewport.getLatitudeMax(), viewport.getLongitudeMax());
        final Geopoint center = viewport.getCenter();
        final int radiusInKm = (int) Math.ceil(viewport.getCenter().distanceTo(corner));
        final String coordRequest = String.format(Locale.US, "%.6f,%.6f,%d", center.getLatitude(), center.getLongitude(), radiusInKm);

        return callAPI(API_URL_STUMPS_FOR_COORDS + coordRequest, false);
    }

    public static <T> Map<String, BetterCacherCache> getCaches(final Collection<T> cacheData, final Func1<T, String> geocodeGetter, final boolean fullData) {
        //search for caches
        final Map<String, BetterCacherCache>[] bcCaches = new Map[1];
        CommonUtils.executeOnPartitions(cacheData, 400, sublist -> {

            final String geocodes = CollectionStream.of(sublist).map(c -> geocodeGetter.call(c).trim()).toJoinedString(",");
            final Map<String, BetterCacherCache> submap = fullData ?
                    callAPI(API_URL_FULLDATA_FOR_GEOCODES + geocodes, true) :
                    callAPI(API_URL_STUMPS_FOR_GEOCODES + geocodes, false);
            if (bcCaches[0] == null) {
                bcCaches[0] = submap;
            } else {
                bcCaches[0].putAll(submap);
            }
            return true;
        });
        return bcCaches[0] == null ? Collections.emptyMap() : bcCaches[0];
    }

    private static Map<String, BetterCacherCache> callAPI(final String uri, final boolean fullData) {
        try (ContextLogger cLog = new ContextLogger(Log.LogLevel.DEBUG, "BetterCacherAPI.callAPI, uri=%1$s, fullData=%2$b", uri, fullData)) {
            final BetterCacherCache[] caches = new HttpRequest().uri(uri)
                    .callTimeout(10000)
                    .requestJson(BetterCacherCache[].class).blockingGet();
            if (caches == null || caches.length == 0) {
                cLog.add("empty");
                return Collections.emptyMap();
            }
            cLog.add(caches.length + " results");
            final Map<String, BetterCacherCache> bcCaches = new HashMap<>();
            for (BetterCacherCache bcCache : caches) {
                if (!bcCache.error && bcCache.gccode != null) {
                    bcCache.setFullData(fullData);
                    bcCaches.put(bcCache.gccode, bcCache);
                }
            }
            cLog.add(bcCaches.size() + " caches");
            return bcCaches;
        } catch (Exception e) {
            logException(uri, e);
        }
        return Collections.emptyMap();
    }

    private static void logException(final String uri, final Throwable exception) {
        Log.w("Problems accessing BetterCacher.org uri " + uri, exception);
    }
}
