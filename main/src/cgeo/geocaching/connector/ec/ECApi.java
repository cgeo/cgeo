package cgeo.geocaching.connector.ec;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class ECApi {

    private static final String API_URL = "http://extremcaching.com/exports/api.php";

    public static String cleanCode(String geocode) {
        return geocode.replace("EC", "");
    }

    public static Geocache searchByGeoCode(final String geocode) {
        final Parameters params = new Parameters("id", cleanCode(geocode));
        params.add("cgeo", "1");
        final HttpResponse response = apiRequest("http://extremcaching.com/exports/gpx.php", params);

        final Collection<Geocache> caches = importCachesFromGPXResponse(response);
        if (CollectionUtils.isNotEmpty(caches)) {
            return caches.iterator().next();
        }
        return null;
    }

    public static Collection<Geocache> searchByBBox(final Viewport viewport) {

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


    public static Collection<Geocache> searchByCenter(final Geopoint center) {

        final Parameters params = new Parameters("fnc", "center");
        params.add("distance", "20");
        params.add("lat", String.valueOf(center.getLatitude()));
        params.add("lon", String.valueOf(center.getLongitude()));
        final HttpResponse response = apiRequest(params);

        return importCachesFromJSON(response);
    }


    private static HttpResponse apiRequest(final Parameters params) {
        return apiRequest(API_URL, params, false);
    }

    private static HttpResponse apiRequest(final String uri, final Parameters params) {
        return apiRequest(uri, params, false);
    }

    private static HttpResponse apiRequest(final String uri, final Parameters params, final boolean isRetry) {
        final HttpResponse response = Network.getRequest(uri, params);

        if (response == null) {
            return null;
        }
        if (!isRetry && response.getStatusLine().getStatusCode() == 403) {
            apiRequest(uri, params, true);
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
        Collection<Geocache> caches;
        try {
            caches = new ECGPXParser(StoredList.TEMPORARY_LIST_ID).parse(response.getEntity().getContent(), null);
        } catch (Exception e) {
            Log.e("Error importing gpx from extremcaching.com", e);
            return Collections.emptyList();
        }
        return caches;
    }

    private static List<Geocache> importCachesFromJSON(final HttpResponse response) {

        if (response != null) {
            try {
                final String data = Network.getResponseDataAlways(response);
                if (data == null || StringUtils.isBlank(data) || StringUtils.equals(data, "[]")) {
                    return Collections.emptyList();
                }
                final JSONArray json = new JSONArray(data);
                final List<Geocache> caches = new ArrayList<Geocache>(json.length());
                for (int i = 0; i < json.length(); i++) {
                    final Geocache cache = parseCache(json.getJSONObject(i));
                    caches.add(cache);
                }
                return caches;
            } catch (final JSONException e) {
                Log.w("JSONResult", e);
            }
        }

        return Collections.emptyList();

    }

    private static Geocache parseCache(final JSONObject response) {
        final Geocache cache = new Geocache();
        cache.setReliableLatLon(true);
        try {
            cache.setGeocode("EC" + response.getString("cache_id"));
            cache.setName(response.getString("title"));
            cache.setCoords(new Geopoint(response.getString("lat"), response.getString("lon")));
            cache.setType(getCacheType(response.getString("type")));
            cache.setDifficulty((float) response.getDouble("difficulty"));
            cache.setTerrain((float) response.getDouble("terrain"));
            cache.setFound(response.getInt("found") == 1 ? true : false);

            DataStore.saveCache(cache, EnumSet.of(SaveFlag.SAVE_CACHE));
        } catch (final JSONException e) {
            Log.e("ECApi.parseCache", e);
        }
        return cache;
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
