package cgeo.geocaching.connector.lc;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

final class LCApi {

    @NonNull
    private static final String API_HOST = "https://labs-api.geocaching.com/Api/Adventures/";

    @NonNull
    private static final LCLogin lcLogin = LCLogin.getInstance();

    private LCApi() {
        // utility class with static methods
    }

    static String getIdFromGeocode(final String geocode) {
        return StringUtils.removeStartIgnoreCase(geocode, "LC");
    }

    @Nullable
    static Geocache searchByGeocode(final String geocode) {
        final Parameters params = new Parameters("id", geocode);
        try {
            final Response response = apiRequest("GetAdventureBasicInfo", params).blockingGet();

            final Collection<Geocache> caches = importCachesFromJSON(response);
            if (CollectionUtils.isNotEmpty(caches)) {
                return caches.iterator().next();
            }
            return null;
        } catch (final Exception ignored) {
            return null;
        }
    }
    @NonNull
    static Collection<Geocache> searchByBBox(final Viewport viewport) {

        if (viewport.getLatitudeSpan() == 0 || viewport.getLongitudeSpan() == 0) {
            return Collections.emptyList();
        }

        final double latcenter = (viewport.getLatitudeMax()  + viewport.getLatitudeMin())  / 2;
        final double loncenter = (viewport.getLongitudeMax() + viewport.getLongitudeMin()) / 2;
        final String latitude  = Double.toString(latcenter);
        final String longitude = Double.toString(loncenter);

        final Parameters params = new Parameters("skip", "0");
        params.add("take", "100");
        params.add("excludeOwned", "false");
        params.add("excludeCompleted", "false");
        params.add("radiusMeters", "10000");
        params.add("latitude", latitude);
        params.add("longitude", longitude);

        try {
            final Response response = apiRequest("GCSearch", params).blockingGet();
            //Log.e(response.toString());
            return importCachesFromJSON(response);
        } catch (final Exception ignored) {
            return Collections.emptyList();
        }
    }
    @NonNull
    static Collection<Geocache> searchByCenter(final Geopoint center) {
        final Parameters params = new Parameters("skip", "0");
        params.add("take", "100");
        params.add("excludeOwned", "false");
        params.add("excludeCompleted", "false");
        params.add("radiusMeters", "10000");
        params.add("latitude", String.valueOf(center.getLatitude()));
        params.add("longitude", String.valueOf(center.getLongitude()));
        try {
            final Response response = apiRequest("GCSearch", params).blockingGet();
            return importCachesFromJSON(response);
        } catch (final Exception ignored) {
            return Collections.emptyList();
        }
    }

    @NonNull
    private static Single<Response> apiRequest(final String uri, final Parameters params) {
        return apiRequest(uri, params, false);
    }

    @NonNull
    private static Single<Response> apiRequest(final String uri, final Parameters params, final boolean isRetry) {

        final Single<Response> response = Network.getRequest(API_HOST + uri, params);

        // retry at most one time
        return response.flatMap((Function<Response, Single<Response>>) response1 -> {
            if (!isRetry && response1.code() == 403 && lcLogin.login() == StatusCode.NO_ERROR) {
                return apiRequest(uri, params, true);
            }
            return Single.just(response1);
        });
    }

    @NonNull
    private static List<Geocache> importCachesFromJSON(final Response response) {
        try {
            final JsonNode json = JsonUtils.reader.readTree(Network.getResponseData(response));
            if (!json.isArray()) {
                return Collections.emptyList();
            }
            final List<Geocache> caches = new ArrayList<>(json.size());
            for (final JsonNode node : json) {
                //Log.e(node.toPrettyString());
                final Geocache cache = parseCache(node);
                if (cache != null) {
                    caches.add(cache);
                }
            }
            return caches;
        } catch (final Exception e) {
            Log.w("importCachesFromJSON", e);
            return Collections.emptyList();
        }
    }

    @Nullable
    private static Geocache parseCache(final JsonNode response) {
        try {
            final Geocache cache = new Geocache();
            final JsonNode location = response.at("/location");
            final String firebaseDynamicLink = response.get("firebaseDynamicLink").asText();
            final String[] segments = firebaseDynamicLink.split("/");
            final String id = segments[segments.length - 1];
            cache.setReliableLatLon(true);
            cache.setGeocodeCaseSensitive("LC" + id);
            cache.setGuid(response.get("id").asText());
            cache.setName(response.get("title").asText());
            cache.setCoords(new Geopoint(location.get("latitude").asText(), location.get("longitude").asText()));
            cache.setType(getCacheType("Lab"));
            cache.setDifficulty((float) 1);
            cache.setTerrain((float) 1);
            cache.setSize(CacheSize.getById("virtual"));
            cache.setFound(false);
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.CACHE));
            return cache;
        } catch (final NullPointerException e) {
            Log.e("LCApi.parseCache", e);
            return null;
        }
    }

    @NonNull
    private static CacheType getCacheType(final String cacheType) {
        if (cacheType.equalsIgnoreCase("Tradi")) {
            return CacheType.TRADITIONAL;
        }
        if (cacheType.equalsIgnoreCase("Multi")) {
            return CacheType.MULTI;
        }
        if (cacheType.equalsIgnoreCase("Event")) {
            return CacheType.EVENT;
        }
        if (cacheType.equalsIgnoreCase("Mystery")) {
            return CacheType.MYSTERY;
        }
        if (cacheType.equalsIgnoreCase("Lab")) {
            return CacheType.USER_DEFINED;
        }
        return CacheType.UNKNOWN;
    }
}
