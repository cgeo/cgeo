package cgeo.geocaching.connector.ec;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.files.GPX10Parser;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import ch.boye.httpclientandroidlib.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

final class ECApi {

    private static final String API_HOST = "https://extremcaching.com/exports/";

    private static final ECLogin ecLogin = ECLogin.getInstance();
    private static final SynchronizedDateFormat LOG_DATE_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", TimeZone.getTimeZone("UTC"), Locale.US);

    private ECApi() {
        // utility class with static methods
    }

    static String getIdFromGeocode(final String geocode) {
        return StringUtils.removeStartIgnoreCase(geocode, "EC");
    }

    static Geocache searchByGeoCode(final String geocode) {
        final Parameters params = new Parameters("id", getIdFromGeocode(geocode));
        final HttpResponse response = apiRequest("gpx.php", params);

        final Collection<Geocache> caches = importCachesFromGPXResponse(response);
        if (CollectionUtils.isNotEmpty(caches)) {
            return caches.iterator().next();
        }
        return null;
    }

    static Collection<Geocache> searchByBBox(final Viewport viewport) {

        if (viewport.getLatitudeSpan() == 0 || viewport.getLongitudeSpan() == 0) {
            return Collections.emptyList();
        }

        final Parameters params = new Parameters("fnc", "bbox");
        params.add("lat1", String.valueOf(viewport.getLatitudeMin()));
        params.add("lat2", String.valueOf(viewport.getLatitudeMax()));
        params.add("lon1", String.valueOf(viewport.getLongitudeMin()));
        params.add("lon2", String.valueOf(viewport.getLongitudeMax()));
        final HttpResponse response = apiRequest(params);

        return importCachesFromJSON(response);
    }


    static Collection<Geocache> searchByCenter(final Geopoint center) {

        final Parameters params = new Parameters("fnc", "center");
        params.add("distance", "20");
        params.add("lat", String.valueOf(center.getLatitude()));
        params.add("lon", String.valueOf(center.getLongitude()));
        final HttpResponse response = apiRequest(params);

        return importCachesFromJSON(response);
    }

    static LogResult postLog(final Geocache cache, final LogType logType, final Calendar date, final String log) {
        return postLog(cache, logType, date, log, false);
    }

    private static LogResult postLog(final Geocache cache, final LogType logType, final Calendar date, final String log, final boolean isRetry) {
        final Parameters params = new Parameters("cache_id", cache.getGeocode());
        params.add("type", logType.type);
        params.add("log", log);
        params.add("date", LOG_DATE_FORMAT.format(date.getTime()));
        params.add("sid", ecLogin.getSessionId());

        final String uri = API_HOST + "log.php";
        final HttpResponse response = Network.postRequest(uri, params);

        if (response == null) {
            return new LogResult(StatusCode.LOG_POST_ERROR_EC, "");
        }
        if (!isRetry && response.getStatusLine().getStatusCode() == 403) {
            if (ecLogin.login() == StatusCode.NO_ERROR) {
                apiRequest(uri, params, true);
            }
        }
        if (response.getStatusLine().getStatusCode() != 200) {
            return new LogResult(StatusCode.LOG_POST_ERROR_EC, "");
        }

        final String data = Network.getResponseDataAlways(response);
        if (!StringUtils.isBlank(data) && StringUtils.contains(data, "success")) {
            if (logType == LogType.FOUND_IT || logType == LogType.ATTENDED) {
                ecLogin.setActualCachesFound(ecLogin.getActualCachesFound() + 1);
            }
            final String uid = StringUtils.remove(data, "success:");
            return new LogResult(StatusCode.NO_ERROR, uid);
        }

        return new LogResult(StatusCode.LOG_POST_ERROR_EC, "");
    }


    private static HttpResponse apiRequest(final Parameters params) {
        return apiRequest("api.php", params);
    }

    private static HttpResponse apiRequest(final String uri, final Parameters params) {
        return apiRequest(uri, params, false);
    }

    private static HttpResponse apiRequest(final String uri, final Parameters params, final boolean isRetry) {
        // add session and cgeo marker on every request
        if (!isRetry) {
            params.add("cgeo", "1");
            params.add("sid", ecLogin.getSessionId());
        }

        final HttpResponse response = Network.getRequest(API_HOST + uri, params);
        if (response == null) {
            return null;
        }

        // retry at most one time
        if (!isRetry && response.getStatusLine().getStatusCode() == 403) {
            if (ecLogin.login() == StatusCode.NO_ERROR) {
                return apiRequest(uri, params, true);
            }
        }
        if (response.getStatusLine().getStatusCode() != 200) {
            return null;
        }
        return response;
    }

    private static Collection<Geocache> importCachesFromGPXResponse(final HttpResponse response) {
        if (response == null) {
            return Collections.emptyList();
        }

        try {
            return new GPX10Parser(StoredList.TEMPORARY_LIST.id).parse(response.getEntity().getContent(), null);
        } catch (final Exception e) {
            Log.e("Error importing gpx from extremcaching.com", e);
            return Collections.emptyList();
        }
    }

    private static List<Geocache> importCachesFromJSON(final HttpResponse response) {
        if (response != null) {
            try {
                final JsonNode json = JsonUtils.reader.readTree(Network.getResponseDataAlways(response));
                if (!json.isArray()) {
                    return Collections.emptyList();
                }
                final List<Geocache> caches = new ArrayList<>(json.size());
                for (final JsonNode node: json) {
                    final Geocache cache = parseCache(node);
                    if (cache != null) {
                        caches.add(cache);
                    }
                }
                return caches;
            } catch (IOException | ClassCastException e) {
                Log.w("importCachesFromJSON", e);
            }
        }

        return Collections.emptyList();
    }

    private static Geocache parseCache(final JsonNode response) {
        try {
            final Geocache cache = new Geocache();
            cache.setReliableLatLon(true);
            cache.setGeocode("EC" + response.get("cache_id").asText());
            cache.setName(response.get("title").asText());
            cache.setCoords(new Geopoint(response.get("lat").asText(), response.get("lon").asText()));
            cache.setType(getCacheType(response.get("type").asText()));
            cache.setDifficulty((float) response.get("difficulty").asDouble());
            cache.setTerrain((float) response.get("terrain").asDouble());
            cache.setSize(CacheSize.getById(response.get("size").asText()));
            cache.setFound(response.get("found").asInt() == 1);
            DataStore.saveCache(cache, EnumSet.of(SaveFlag.CACHE));
            return cache;
        } catch (final NullPointerException e) {
            Log.e("ECApi.parseCache", e);
            return null;
        }
    }

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
        return CacheType.UNKNOWN;
    }
}
