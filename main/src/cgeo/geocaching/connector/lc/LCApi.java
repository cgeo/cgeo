package cgeo.geocaching.connector.lc;

import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;

import okhttp3.Response;

final class LCApi {

    @NonNull
    private static final String API_HOST = "https://labs-api.geocaching.com/Api/Adventures/";

    @NonNull
    private static final LCLogin lcLogin = LCLogin.getInstance();

    @NonNull
    private static final SynchronizedDateFormat LOG_DATE_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", TimeZone.getTimeZone("UTC"), Locale.US);

    private LCApi() {
        // utility class with static methods
    }

    static String getIdFromGeocode(final String geocode) {
        return StringUtils.removeStartIgnoreCase(geocode, "LC");
    }

    @Nullable
    static Geocache searchByGUID(final String guid) {
        Log.e(guid);
        final Parameters params = new Parameters("id", guid);
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
    static LogResult postLog(@NonNull final Geocache cache, @NonNull final LogType logType, @NonNull final Calendar date, @NonNull final String log) {
        final Parameters params = new Parameters("cache_id", cache.getGeocode());
        params.add("type", logType.type);
        params.add("log", log);
        params.add("date", LOG_DATE_FORMAT.format(date.getTime()));

        final String uri = API_HOST + "log.php";
        try {
            final Response response = Network.postRequest(uri, params).blockingGet();

            if (response.code() == 403 && lcLogin.login() == StatusCode.NO_ERROR) {
                apiRequest(uri, params, true);
            }
            if (response.code() != 200) {
                return new LogResult(StatusCode.LOG_POST_ERROR, "");
            }

            final String data = Network.getResponseData(response, false);
            if (!StringUtils.isBlank(data) && StringUtils.contains(data, "success")) {
                if (logType.isFoundLog()) {
                    lcLogin.increaseActualCachesFound();
                }
                final String uid = StringUtils.remove(data, "success:");
                return new LogResult(StatusCode.NO_ERROR, uid);
            }
        } catch (final Exception ignored) {
            // Response is already logged
        }

        return new LogResult(StatusCode.LOG_POST_ERROR, "");
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
            cache.setGeocode("LC" + id);
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
