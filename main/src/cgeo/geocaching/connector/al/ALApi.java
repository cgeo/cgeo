package cgeo.geocaching.connector.al;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;
import static cgeo.geocaching.enumerations.CacheType.ADVLAB;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

final class ALApi {

    @NonNull
    private static final String API_HOST = "https://labs-api.geocaching.com/Api/Adventures/";
    private static final String CONSUMER_HEADER = "X-Consumer-Key";
    private static final String CONSUMER_KEY = LocalizationUtils.getString(R.string.alc_consumer_key);

    private static final String LOCATION = "/Location";
    private static final String LONGITUDE = "Longitude";
    private static final String LATITUDE = "Latitude";
    private static final String TITLE = "Title";
    private static final String MULTICHOICEOPTIONS = "MultiChoiceOptions";

    private static final int DEFAULT_RADIUS = 10 * 1000; // 10km

    private ALApi() {
        // utility class with static methods
    }

    @Nullable
    @WorkerThread
    protected static Geocache searchByGeocode(final String geocode) {
        if (!Settings.isGCPremiumMember() || CONSUMER_KEY.isEmpty()) {
            return null;
        }
        final Parameters headers = new Parameters(CONSUMER_HEADER, CONSUMER_KEY);
        try {
            final Response response = apiRequest(geocode.substring(2), null, headers).blockingGet();
            return importCacheFromJSON(response);
        } catch (final Exception ignored) {
            return null;
        }
    }

    @NonNull
    protected static Collection<Geocache> searchByBBox(final Viewport viewport) {

        if (!Settings.isGCPremiumMember() || CONSUMER_KEY.isEmpty() || viewport.getLatitudeSpan() == 0 || viewport.getLongitudeSpan() == 0) {
            return Collections.emptyList();
        }

        final double lat1 = viewport.getLatitudeMax();
        final double lat2 = viewport.getLatitudeMin();
        final double lon1 = viewport.getLongitudeMax();
        final double lon2 = viewport.getLongitudeMin();
        final double latcenter = (lat1 + lat2) / 2;
        final double loncenter = (lon1 + lon2) / 2;
        final Geopoint gp1 = new Geopoint(lat1, lon1);
        final Geopoint gp2 = new Geopoint(lat2, lon2);
        final int radius = (int) (gp1.distanceTo(gp2) * 500); // we get diameter in km, need radius in m
        Log.d("_AL Radius: " + radius);

        final Geopoint center = new Geopoint(latcenter, loncenter);
        return searchByCenter(center, radius);
    }

    @NonNull
    protected static Collection<Geocache> searchByCenter(final Geopoint center) {
        return searchByCenter(center, DEFAULT_RADIUS);
    }

    @NonNull
    @WorkerThread
    private static Collection<Geocache> searchByCenter(final Geopoint center, final int distanceInMeters) {
        if (!Settings.isGCPremiumMember() || CONSUMER_KEY.isEmpty()) {
            return Collections.emptyList();
        }
        final Parameters params = new Parameters("skip", "0");
        params.add("take", "100");
        params.add("radiusMeters", "" + distanceInMeters);
        params.add("origin.latitude", String.valueOf(center.getLatitude()));
        params.add("origin.longitude", String.valueOf(center.getLongitude()));
        final Parameters headers = new Parameters(CONSUMER_HEADER, CONSUMER_KEY);
        try {
            final Response response = apiRequest("SearchV3", params, headers).blockingGet();
            return importCachesFromJSON(response);
        } catch (final Exception ignored) {
            return Collections.emptyList();
        }
    }

    @NonNull
    public static Collection<Geocache> searchByFilter(final GeocacheFilter filter, final IConnector connector) {
        //for now we have to assume that ALConnector supports only SINGLE criteria search

        final List<BaseGeocacheFilter> filters = filter.getAndChainIfPossible();
        final OriginGeocacheFilter of = GeocacheFilter.findInChain(filters, OriginGeocacheFilter.class);
        if (of != null && !of.allowsCachesOf(connector)) {
            return new ArrayList<>();
        }
        final DistanceGeocacheFilter df = GeocacheFilter.findInChain(filters, DistanceGeocacheFilter.class);
        if (df != null) {
            return searchByCenter(df.getEffectiveCoordinate(), df.getMaxRangeValue() == null ? DEFAULT_RADIUS : df.getMaxRangeValue().intValue() * 1000);
        }
        final TypeGeocacheFilter tf = GeocacheFilter.findInChain(filters, TypeGeocacheFilter.class);
        if (tf != null && tf.isFiltering() && !tf.getRawValues().contains(ADVLAB)) {
            return new ArrayList<>();
        }

        //by default, search around current position
        return searchByCenter(Sensors.getInstance().currentGeo().getCoords());
    }

    @NonNull
    private static Single<Response> apiRequest(final String uri) {
        return Network.getRequest(API_HOST + uri);
    }

    @NonNull
    private static Single<Response> apiRequest(final String uri, @Nullable final Parameters params, final Parameters headers) {
        return apiRequest(uri, params, headers, false);
    }

    @NonNull
    private static Single<Response> apiRequest(final String uri, @Nullable final Parameters params, final Parameters headers, final boolean isRetry) {

        final Single<Response> response = Network.getRequest(API_HOST + uri, params, headers);

        // retry at most one time
        return response.flatMap((Function<Response, Single<Response>>) response1 -> {
            if (!isRetry && response1.code() == 403) {
                return apiRequest(uri, params, headers, true);
            }
            return Single.just(response1);
        });
    }

    @Nullable
    private static Geocache importCacheFromJSON(final Response response) {
        try {
            final String jsonString = Network.getResponseData(response);
            if (jsonString == null) {
                Log.d("_AL importCacheFromJson: null response from network");
                return null;
            }
            final JsonNode json = JsonUtils.reader.readTree(jsonString);
            Log.d("_AL importCacheFromJson: " + json.toPrettyString());
            return parseCacheDetail(json);
        } catch (final Exception e) {
            Log.w("_AL importCacheFromJSON", e);
            return null;
        }
    }

    @NonNull
    private static List<Geocache> importCachesFromJSON(final Response response) {
        try {
            final String jsonString = Network.getResponseData(response);
            if (jsonString == null) {
                Log.d("_AL importCachesFromJson: null response from network");
                return Collections.emptyList();
            }
            final JsonNode json = JsonUtils.reader.readTree(jsonString);
            Log.d("_AL importCachesFromJson: " + json.toPrettyString());
            final JsonNode items = json.at("/Items");
            if (!items.isArray()) {
                return Collections.emptyList();
            }
            final List<Geocache> caches = new ArrayList<>(items.size());
            for (final JsonNode node : items) {
                final Geocache cache = parseCache(node);
                if (cache != null) {
                    caches.add(cache);
                }
            }
            return caches;
        } catch (final Exception e) {
            Log.w("_AL importCachesFromJSON", e);
            return Collections.emptyList();
        }
    }

    @Nullable
    private static Geocache parseCache(final JsonNode response) {
        try {
            final Geocache cache = new Geocache();
            final JsonNode location = response.at(LOCATION);
            final String firebaseDynamicLink = response.get("FirebaseDynamicLink").asText();
            final String[] segments = firebaseDynamicLink.split("/");
            final String geocode = ALConnector.GEOCODE_PREFIX + response.get("Id").asText();
            cache.setGeocode(geocode);
            cache.setCacheId(segments[segments.length - 1]);
            cache.setName(response.get(TITLE).asText());
            cache.setCoords(new Geopoint(location.get(LATITUDE).asText(), location.get(LONGITUDE).asText()));
            cache.setType(ADVLAB);
            cache.setSize(CacheSize.getById("virtual"));
            cache.setArchived(response.get("IsArchived").asBoolean()); // we get that even in passive mode!
            // cache.setFound(response.get("IsComplete").asBoolean()); as soon as we're using active mode
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.CACHE));
            return cache;
        } catch (final NullPointerException e) {
            Log.e("_AL ALApi.parseCache", e);
            return null;
        }
    }

    // Having a separate parser for details is required because the API provider
    // decided to use different upper/lower case wordings for the same entities

    @Nullable
    private static Geocache parseCacheDetail(final JsonNode response) {
        try {
            final Geocache cache = new Geocache();
            final JsonNode location = response.at(LOCATION);
            final String firebaseDynamicLink = response.get("FirebaseDynamicLink").asText();
            final String[] segments = firebaseDynamicLink.split("/");
            final String geocode = ALConnector.GEOCODE_PREFIX + response.get("Id").asText();
            final String ilink = response.get("KeyImageUrl").asText();
            final String desc = response.get("Description").asText();
            cache.setGeocode(geocode);
            cache.setCacheId(segments[segments.length - 1]);
            cache.setName(response.get(TITLE).asText());
            cache.setDescription((StringUtils.isNotBlank(ilink) ? "<img src=\"" + ilink + "\" </img><p><p>" : "") + desc);
            cache.setCoords(new Geopoint(location.get(LATITUDE).asText(), location.get(LONGITUDE).asText()));
            cache.setType(ADVLAB);
            cache.setSize(CacheSize.getById("virtual"));
            // cache.setArchived(response.get("IsArchived").asBoolean()); as soon as we're using active mode
            // cache.setFound(response.get("IsComplete").asBoolean()); as soon as we're using active mode
            cache.setDisabled(false);
            cache.setHidden(parseDate(response.get("PublishedUtc").asText()));
            cache.setOwnerDisplayName(response.get("OwnerUsername").asText());
            cache.setWaypoints(parseWaypoints((ArrayNode) response.path("GeocacheSummaries")), true);
            cache.setDetailedUpdatedNow();
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
            return cache;
        } catch (final NullPointerException e) {
            Log.e("_AL ALApi.parseCache", e);
            return null;
        }
    }

    @Nullable
    private static List<Waypoint> parseWaypoints(final ArrayNode wptsJson) {
        if (!Settings.isALCAdvanced()) {
            return null;
        }
        List<Waypoint> result = null;
        final Geopoint pointZero = new Geopoint(0, 0);
        int stageCounter = 0;
        for (final JsonNode wptResponse : wptsJson) {
            stageCounter++;
            try {
                final Waypoint wpt = new Waypoint(wptResponse.get(TITLE).asText(), WaypointType.PUZZLE, false);
                final JsonNode location = wptResponse.at(LOCATION);
                final String ilink = wptResponse.get("KeyImageUrl").asText();
                final String desc = wptResponse.get("Description").asText();

                // For ALCs, waypoints don't have a geocode, of course they have an id (a uuid) though.
                // We artificially create a geocode and a prefix as at least the prefix is used when
                // showing waypoints on the map. It seems that the geocode from the parent is used but
                // prefixed with what we set here. Not clear where the geocode of a waypoint comes into play
                // but we will eventually figure that out.

                wpt.setGeocode(String.valueOf(stageCounter));
                wpt.setPrefix(String.valueOf(stageCounter));

                String note = "<img style=\"width: 100%;\" src=\"" + ilink + "\"</img><p><p>" + desc + "<p><p>" + wptResponse.get("Question").asText();

                try {
                    final JsonNode jn = wptResponse.path(MULTICHOICEOPTIONS);
                    if (jn instanceof ArrayNode) { // implicitly covers null case as well
                        final ArrayNode multiChoiceOptions = (ArrayNode) jn;
                        if (!multiChoiceOptions.isEmpty()) {
                            note += "<ul>";
                            for (final JsonNode mc : multiChoiceOptions) {
                                note += "<li>" + mc.get("Text").asText() + "</li>";
                            }
                            note += "</ul>";
                        }
                    }
                } catch (Exception ignore) {
                    // ignore exception
                }
                wpt.setNote(note);

                final Geopoint pt = new Geopoint(location.get(LATITUDE).asDouble(), location.get(LONGITUDE).asDouble());
                if (!pt.equals(pointZero)) {
                    wpt.setCoords(pt);
                } else {
                    wpt.setOriginalCoordsEmpty(true);
                }
                if (result == null) {
                    result = new ArrayList<>();
                }

                result.add(wpt);
            } catch (final NullPointerException e) {
                Log.e("_AL ALApi.parseWaypoints", e);
            }
        }
        return result;
    }

    @Nullable
    private static Date parseDate(final String date) {
        final SynchronizedDateFormat dateFormat = new SynchronizedDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            return dateFormat.parse(date);
        } catch (final ParseException e) {
            return new Date(0);
        }
    }
}
