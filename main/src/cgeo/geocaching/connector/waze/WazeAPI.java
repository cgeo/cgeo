package cgeo.geocaching.connector.waze;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import okhttp3.Response;

final public class WazeAPI {

    //https://www.waze.com/row-rtserver/web/TGeoRSS?bottom=47.25928721616973&left=-1.5677833557128906&ma=200&mj=100&mu=20&right=-1.4071083068847656&top=47.38537987959898
    @NonNull
    private static final String API_HOST = "https://www.waze.com/row-rtserver/web/";

    @NonNull
    static Collection<Geocache> searchByBBox(final Viewport viewport) {

        if (viewport.getLatitudeSpan() == 0 || viewport.getLongitudeSpan() == 0) {
            return Collections.emptyList();
        }

        final double centerlat =  viewport.getLatitudeMin() + (viewport.getLatitudeMax()-viewport.getLatitudeMin())/2;
        final double centerlon =  viewport.getLongitudeMin() + (viewport.getLongitudeMax()-viewport.getLongitudeMin())/2;

        final Geopoint centerPoint = new Geopoint(centerlat,centerlon);

        try {
            final Parameters params = new Parameters();

            //TGeoRSS?
            // bottom=47.25928721616973&
            // left=-1.5677833557128906&
            // ma=200&
            // mj=100&
            // mu=20&
            // right=-1.4071083068847656&
            // top=47.38537987959898

            params.add("bottom", String.valueOf( viewport.getLatitudeMin()));
            params.add("left", String.valueOf( viewport.getLongitudeMin()));
            params.add("ma","200");
            params.add("mj","100");
            params.add("mu","20");
            params.add("right", String.valueOf(viewport.getLongitudeMax()));
            params.add("top", String.valueOf(viewport.getLatitudeMax()));

                final Response response = apiRequest(params).blockingGet();
                return importCachesFromJSON(response);

            } catch (final Exception ignored) {
            return Collections.emptyList();
        }
    }

    static Collection<Geocache> searchByCenter(final Geopoint center) {

        Viewport vp = new Viewport(center,0.05,0.05);

        try {
            return searchByBBox(vp);
        } catch (final Exception ignored) {
            return Collections.emptyList();
        }
    }


    @NonNull
    private static List<Geocache> importCachesFromJSON(final Response response) {
        try {
            final JsonNode jsonResp = JsonUtils.reader.readTree(Network.getResponseData(response));

            final JsonNode json = jsonResp.get("alerts");
            if (!json.isArray()) {
                return Collections.emptyList();
            }

            final List<Geocache> caches = new ArrayList<>(json.size());
            for (final JsonNode node : json) {
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
            final String intId = response.get("id").asText();
            final long lngId = Long.parseLong(String.valueOf(intId.subSequence(intId.indexOf('-')+1,intId.indexOf('/'))));
            final Geocache cache = new Geocache();
            cache.setReliableLatLon(true);
            cache.setGeocode("WAZ"+Long.toString(lngId,31));
            cache.setGuid(response.get("uuid").asText());
            cache.setName(response.get("type").asText());
            cache.setCoords(new Geopoint(response.get("location").get("y").asText(), response.get("location").get("x").asText()));
            cache.setType(getCacheType(response.get("type").asText(), response.get("subtype").asText()));
            cache.setSize(CacheSize.NOT_CHOSEN);

            DataStore.saveCache(cache, EnumSet.of(LoadFlags.SaveFlag.DB));
            return cache;
        } catch (final NullPointerException e) {
            Log.e("WazeApi.parseCache", e);
            return null;
        }
    }

    @NonNull
    private static CacheType getCacheType(final String cacheType, final String subtype) {
       final String sub = subtype.toUpperCase();
        switch(cacheType.toUpperCase()) {
            case "ROAD_CLOSED":
               return CacheType.WAZEROAD_CLOSED;
            case "ACCIDENT":
                switch(sub){
                    case "ACCIDENT_MINOR":
                        return CacheType.WAZEACCIDENT_MINOR;

                    case "ACCIDENT_MAJOR":
                        return CacheType.WAZEACCIDENT_MAJOR;

                    default:
                        return CacheType.WAZEACCIDENT;
                }
            case "JAM":
                switch(sub){
                    case "JAM_MODERATE_TRAFFIC":
                        return CacheType.WAZEJAM_MODERATE;

                    case "JAM_HEAVY_TRAFFIC":
                        return CacheType.WAZEJAM_HEAVY;

                    case "JAM_STAND_STILL_TRAFFIC":
                        return CacheType.WAZEJAM_STANDSTILL;

                    default:
                        return CacheType.WAZEJAM;

                }
            case"POLICE":
                if(sub == "POLICE_HIDING")
                    return CacheType.WAZEPOLICE_HIDING;
                return CacheType.WAZEPOLICE;
            case "HAZARD":
                switch (sub) {
                    case "HAZARD_ON_ROAD":
                        return CacheType.WAZEHAZARD_ON_ROAD;

                            case "HAZARD_ON_SHOULDER":
                                return CacheType.WAZEHAZARD_ON_SHOULDER;


                                    case "HAZARD_WEATHER":
                                        return CacheType.WAZEHAZARD_WEATHER;

                    case "HAZARD_ON_ROAD_OBJECT":
                        return CacheType.WAZEHAZARD_ON_ROAD_OBJECT;

                    case "HAZARD_ON_ROAD_POT_HOLE":
                        return CacheType.WAZEHAZARD_ON_ROAD_POT_HOLE;

                    case "HAZARD_ON_ROAD_ROAD_KILL":
                        return CacheType.WAZEHAZARD_ON_ROAD_ROAD_KILL;

                    case "HAZARD_ON_ROAD_CAR_STOPPED":
                        case "HAZARD_ON_SHOULDER_CAR_STOPPED":
                        return CacheType.WAZEHAZARD_ON_ROAD_CAR_STOPPED;

                    case "HAZARD_ON_SHOULDER_ANIMALS":
                        return CacheType.WAZEHAZARD_ON_SHOULDER_ANIMALS;

                    case "HAZARD_WEATHER_FOG":
                        return CacheType.WAZEHAZARD_WEATHER_FOG;

                    case "HAZARD_WEATHER_HAIL":
                        return CacheType.WAZEHAZARD_WEATHER_HAIL;

                    case "HAZARD_WEATHER_FLOOD":
                        return CacheType.WAZEHAZARD_WEATHER_FLOOD;

                    case "HAZARD_ON_ROAD_OIL":
                        return CacheType.WAZEHAZARD_ON_ROAD_OIL;

                    case "HAZARD_ON_ROAD_ICE":
                        return CacheType.WAZEHAZARD_ON_ROAD_ICE;

                    case "HAZARD_ON_ROAD_CONSTRUCTION":
                        return CacheType.WAZEHAZARD_ON_ROAD_CONSTRUCTION;

                    default:
                        return CacheType.WAZEHAZARD;

                }
            case "CHIT_CHAT":
                return CacheType.WAZECHIT_CHAT;
            default:
                return CacheType.WAZEHAZARD;
        }

        /*if (cacheType.equalsIgnoreCase("ROAD_CLOSED")) {
            return CacheType.WAZEROAD_CLOSED;
        }
        if (cacheType.equalsIgnoreCase("ACCIDENT")) {
            return CacheType.WAZEACCIDENT;
        }
        if (cacheType.equalsIgnoreCase("JAM")) {
            return CacheType.WAZEJAM;
        }
        if (cacheType.equalsIgnoreCase("POLICE")) {
            return CacheType.WAZEPOLICE;
        }
        if (cacheType.equalsIgnoreCase("HAZARD")) {
            return CacheType.WAZEHAZARD;
        }
        if (cacheType.equalsIgnoreCase("CHIT_CHAT")) {
            return CacheType.WAZECHIT_CHAT;
        }

        return CacheType.WAZEHAZARD;*/
    }

    @NonNull
    private static Single<Response> apiRequest(final Parameters params) {
        return apiRequest("TGeoRSS", params);
    }

    @NonNull
    private static Single<Response> apiRequest(final String uri, final Parameters params) {
        return apiRequest(uri, params, false);
    }

    @NonNull
    private static Single<Response> apiRequest(final String uri, final Parameters params, final boolean isRetry) {


        Parameters headers = new Parameters();
        headers.add("referer","https://www.waze.com/live-map/");

        final Single<Response> response = Network.getRequest(API_HOST + uri, params, headers);

        // retry at most one time
        return response.flatMap((Function<Response, Single<Response>>) response1 -> {
            if (!isRetry && response1.code() == 403) {
                return apiRequest(uri, params, true);
            }
            return Single.just(response1);
        });
    }
}
