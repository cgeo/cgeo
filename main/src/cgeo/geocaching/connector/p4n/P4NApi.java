package cgeo.geocaching.connector.p4n;

import android.icu.util.MeasureUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.android.gms.common.util.Strings;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import okhttp3.Response;

public class P4NApi {


    @NonNull
    private static final String API_HOSTV3 = "https://www.park4night.com/services/V3/";
    private static final String API_HOSTV4 = "https://www.park4night.com/services/V4/";

    static float getHMax()
    {

        float hMax = (float)Settings.getP4NHeightThreshold() / (float)100.0;

        return hMax;
    }


    @NonNull
    static Collection<Geocache> searchByBBox(final Viewport viewport) {

        if (viewport.getLatitudeSpan() == 0 || viewport.getLongitudeSpan() == 0) {
            return Collections.emptyList();
        }

        final double centerlat =  viewport.getLatitudeMin() + (viewport.getLatitudeMax()-viewport.getLatitudeMin())/2;
        final double centerlon =  viewport.getLongitudeMin() + (viewport.getLongitudeMax()-viewport.getLongitudeMin())/2;

        final Geopoint centerPoint = new Geopoint(centerlat,centerlon);

        try {
            return searchByCenter(centerPoint);
            //return importCachesFromJSON(response);
        } catch (final Exception ignored) {
            return Collections.emptyList();
        }
    }

    static Collection<Geocache> searchByCenter(final Geopoint center) {

        final Parameters params = new Parameters();
        params.add("hauteur_limite",String.valueOf(getHMax()));
        params.add("latitude", String.valueOf(center.getLatitude()));
        params.add("longitude", String.valueOf(center.getLongitude()));
        try {
            final Response response = apiRequestList(params).blockingGet();
            return importCachesFromJSON(response);
        } catch (final Exception ignored) {
            return Collections.emptyList();
        }
    }

    static Collection<Geocache> searchByCenterLite(final Geopoint center,float distance) {
        final Parameters params = new Parameters();
        float hMax = getHMax();

        params.add("latitude", String.valueOf(center.getLatitude()));
        params.add("longitude", String.valueOf(center.getLongitude()));
        params.add("distance",String.valueOf(distance));
        try {
            final Response response = apiRequestListLite(params).blockingGet();
            return importCachesFromJSON(response);
        } catch (final Exception ignored) {
            return Collections.emptyList();
        }
    }

    static Geocache searchById(final String id)
    {
        //https://park4night.com/services/V4/lieuGetOneLieux.php?id=3765&appli=park4night
        final Parameters params = new Parameters();

        params.add("id", String.valueOf(Integer.parseInt(id,31)));
        params.add("appli", "park4night");

        try {
            final Response response = apiRequestOne(params).blockingGet();
            return importCacheFromJSON(response);
        } catch (final Exception ignored) {
            return null;
        }

    }

    static List<LogEntry> GetLogs(final String id)
    {
        final Parameters params = new Parameters();

        params.add("lieu_id", String.valueOf(Integer.parseInt(id,31)));
        params.add("appli", "park4night");

        try {
            final Response response = apiRequestLogs(params).blockingGet();
            return parseLogs(response, id);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static Single<Response> apiRequestOne(final Parameters params) {
        return apiRequest("lieuGetOneLieux.php", params);
    }

    private static Single<Response> apiRequestList(final Parameters params) {
        return apiRequest("lieuxGetFilter.php", params);
    }

    private static Single<Response> apiRequestListLite(final Parameters params) {
        return apiRequestV3("getLieuxAroundMe.php", params);
    }

    private static Single<Response> apiRequestLogs(final Parameters params)
    {
        return apiRequest("commGet.php", params);
    }

    @NonNull
    private static Single<Response> apiRequest(final String uri, final Parameters params) {
        return apiRequest(uri, params, false);
    }

    @NonNull
    private static Single<Response> apiRequest(final String uri, final Parameters params, final boolean isRetry) {

        final Single<Response> response = Network.getRequest(API_HOSTV4 + uri, params);

        // retry at most one time
        return response.flatMap((Function<Response, Single<Response>>) response1 -> {
            return Single.just(response1);
        });
    }

    @NonNull
    private static Single<Response> apiRequestV3(final String uri, final Parameters params) {

        final Single<Response> response = Network.getRequest(API_HOSTV3 + uri, params);

        // retry at most one time
        return response.flatMap((Function<Response, Single<Response>>) response1 -> {
            return Single.just(response1);
        });
    }

    @NonNull
    private static List<Geocache> importCachesFromJSON(final Response response) {
        try {
            final JsonNode jsonResp = JsonUtils.reader.readTree(Network.getResponseData(response));

            final JsonNode json = jsonResp.get("lieux");
            if (!json.isArray()) {
                return Collections.emptyList();
            }
            final List<Geocache> caches = new ArrayList<>(100/*json.size()*/);
            int counter = 0;
            for (final JsonNode node : json) {
                if(counter++ >=100)
                    break;
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
    private static Geocache importCacheFromJSON(final Response response) {
        try {
            final JsonNode jsonResp = JsonUtils.reader.readTree(Network.getResponseData(response));

            final JsonNode json = jsonResp.get("lieux");
            if (!json.isArray()) {
                return null;
            }

                final Geocache cache = parseCache(json.get(0));

            return cache;
        } catch (final Exception e) {
            Log.w("importCacheFromJSON", e);
            return null;
        }
    }

    @Nullable
    private static Geocache parseCache(final JsonNode response) {
        try {

            Integer idInt =  Integer.parseInt(response.get("id").asText());

            String id = Integer.toString(idInt,31);

            final Geocache cache = new Geocache();
            cache.setReliableLatLon(true);
            cache.setGeocode("P4N" + id);
            cache.setName(response.get("titre").asText());
            cache.setCoords(new Geopoint(response.get("latitude").asText(), response.get("longitude").asText()));
            cache.setType(getCacheType(response.get("code").asText()));
            cache.setShortDescription( composeShortDescription(response));
            cache.setDescription(composeLongDescription(response));
            cache.setHidden(parseDate(response.get("date_creation").asText()));
            //cache.setDifficulty((float) response.get("difficulty").asDouble());
            //cache.setTerrain((float) response.get("terrain").asDouble());
            cache.setRating((float)response.get("note_moyenne").asDouble());
            cache.setSize(CacheSize.NOT_CHOSEN);
            //cache.setFound(response.get("found").asInt() == 1);
            cache.setLocation(response.get("route").asText() + " " + response.get("code_postal").asText() +" " +response.get("ville").asText() +" "+ response.get("pays").asText());
            List<String> attributes = parseAttributes(response);
            cache.setAttributes(attributes);

            List<Image> photos = parsePhotos(response);
            for(final Image img:photos)
                cache.addSpoiler(img);

            /* on filtre la hauteur max pour être sûr... */
            float hLimite = (float)response.get("hauteur_limite").asDouble();
            if(hLimite >0 && hLimite< getHMax() )
               return null;

            DataStore.saveCache(cache, EnumSet.of(LoadFlags.SaveFlag.DB));
            return cache;
        } catch (final NullPointerException e) {
            Log.e("P4NApi.parseCache", e);
            return null;
        }
    }

    private static String composeLongDescription(JsonNode response)
    {
        final StringBuilder descriptionBuilder = new StringBuilder();
        String defaultUserlanguage = Settings.getApplicationLocale().getLanguage();
        HashMap<String,String> lDesc = new HashMap<String,String>();
        lDesc.put("description_fr","Détail du lieu :");
        lDesc.put("description_en","Detail of the place :");
        lDesc.put("description_de","Detail eines Platzes :");
        lDesc.put("description_it","Dettaglio di un luogo :");
        lDesc.put("description_nl","Detail van een locatie :");
        lDesc.put("description_es","Detalle de un lugar :");

        try
        {
            String desc = response.get("description_"+defaultUserlanguage.toLowerCase()).asText();
            if(!Strings.isEmptyOrWhitespace(desc)  )
            {
                addBoldText(descriptionBuilder, lDesc.get("description_"+defaultUserlanguage.toLowerCase()));
                descriptionBuilder.append(desc);
                descriptionBuilder.append("<br>");
                lDesc.remove("description_"+defaultUserlanguage.toLowerCase());
            };



            for(final Map.Entry l : lDesc.entrySet() )
            {
                desc = response.get(l.getKey().toString()).asText();
                if(!Strings.isEmptyOrWhitespace(desc)  ) {
                    descriptionBuilder.append("<br>");
                    addBoldText(descriptionBuilder, l.getValue().toString());
                    descriptionBuilder.append(desc);
                    descriptionBuilder.append("<br>");
                }

            }
        }
        catch (final Exception e) {
        Log.e("P4NApi.composeLongDescription", e);

        }
        //description_fr
        //description_en
        //description_de
        //description_it
        //description_nl
        //description_es

       return descriptionBuilder.toString();
    }

    private static String composeShortDescription(JsonNode response)
    {
        final StringBuilder descriptionBuilder = new StringBuilder();

        //"reseaux":"",
        addValue(descriptionBuilder,response,R.string.P4NReseau,"reseaux");

        // "date_fermeture":"no restrictions",
        addValue(descriptionBuilder,response,R.string.P4NCloseDate,"date_fermeture");
        // "borne":"",

        addValue(descriptionBuilder,response,R.string.P4NBorneType,"borne");
        // "prix_stationnement":"free",
        addValue(descriptionBuilder,response,R.string.P4NParkPrice,"prix_stationnement");
        // "prix_services":"none",
        addValue(descriptionBuilder,response,R.string.P4NServicePrice,"prix_services");
        // "nb_places":"6",
        if(response.get("nb_places").asText() != "0" )
            addValue(descriptionBuilder,response,R.string.P4NAvaiablePlaces,"nb_places");
        // "hauteur_limite":"0.00
        if(response.get("hauteur_limite").asText() != "0.00" )
            addValue(descriptionBuilder,response,R.string.P4NMaxHeight,"hauteur_limite");

        addValue(descriptionBuilder,response,R.string.P4NWebsite,"site_internet");

        addValue(descriptionBuilder,response,R.string.P4NTelephone,"tel");



        return descriptionBuilder.toString();
    }



    private static void addValue(final StringBuilder builder,final JsonNode response, final int valueTitle, final String valueName)
    {
        try {
            String value = response.get(valueName).asText();
            if (!Strings.isEmptyOrWhitespace(value) && value !="null") {
                addBoldText(builder, CgeoApplication.getInstance().getBaseContext().getString(valueTitle) + ": ");
                builder.append(value);
                builder.append("<br>");
            }
        }
        catch(final Exception e) {
        Log.w("addValue", e);
        }
    }

    private static void addBoldText(final StringBuilder builder, final String text) {
        builder.append("<strong>");
        builder.append(text);
        builder.append("</strong>");
    }

    @NonNull
    private static CacheType getCacheType(final String cacheType) {
        if (cacheType.equalsIgnoreCase("ACC_G")) {
            return CacheType.P4NACC_G;
        }
        if (cacheType.equalsIgnoreCase("ACC_P")) {
            return CacheType.P4NACC_P;
        }
        if (cacheType.equalsIgnoreCase("ACC_PR")) {
            return CacheType.P4NACC_PR;
        }
        if (cacheType.equalsIgnoreCase("EP")) {
            return CacheType.P4NEP;
        }
        if (cacheType.equalsIgnoreCase("ASS")) {
            return CacheType.P4NASS;
        }
        if (cacheType.equalsIgnoreCase("APN")) {
            return CacheType.P4NAPN;
        }
        if (cacheType.equalsIgnoreCase("AR")) {
            return CacheType.P4NAR;
        }
        if (cacheType.equalsIgnoreCase("C")) {
            return CacheType.P4NC;
        }
        if (cacheType.equalsIgnoreCase("F")) {
            return CacheType.P4NF;
        }
        if (cacheType.equalsIgnoreCase("PN")) {
            return CacheType.P4NPN;
        }
        if (cacheType.equalsIgnoreCase("P")) {
            return CacheType.P4NP;
        }
        if (cacheType.equalsIgnoreCase("PJ")) {
            return CacheType.P4NPJ;
        }
        if (cacheType.equalsIgnoreCase("OR")) {
            return CacheType.P4NOR;
        }
        if (cacheType.equalsIgnoreCase("DS")) {
            return CacheType.P4NDS;
        }
        return CacheType.UNKNOWN;
    }


    @NonNull
    private static List<LogEntry> parseLogs(final Response response, final String geocode) {
        final List<LogEntry> result = new LinkedList<>();
        try {
        final JsonNode jsonResp = JsonUtils.reader.readTree(Network.getResponseData(response));

        final JsonNode logsJSON = jsonResp.get("commentaires");

        for (final JsonNode logResponse: logsJSON) {
            try {
               /* final Date date = parseDate(logResponse.get(LOG_DATE).asText());
                if (date == null) {
                    continue;
                }*/
                final LogEntry log =  new LogEntry.Builder()
                   // .setServiceLogId(logResponse.get(LOG_UUID).asText().trim() + ":" + logResponse.get(LOG_INTERNAL_ID).asText().trim())
                    .setServiceLogId("P4NL"+ logResponse.get("id"))
                    //.setAuthor(parseUser(logResponse.get(LOG_USER)))
                    .setAuthor(logResponse.get("uuid").asText())
                    .setDate(parseDate(logResponse.get("date_creation").asText()).getTime())
                    .setLogType(LogType.NOTE)
                    //.setLogImages(parseLogImages((ArrayNode) logResponse.path(LOG_IMAGES), geocode))
                    .setLog(logResponse.get("commentaire").asText().trim())
                    .setRating((float)logResponse.get("note").asDouble())
                    .build();


                result.add(log);
            } catch (final NullPointerException e) {
                Log.e("P4NApi.parseLogs", e);
            }
        }

        } catch (final Exception e) {
            Log.w("importCacheFromJSON", e);
            return null;
        }
        return result;
    }

    @Nullable
    private static Date parseDate(final String date) {
        try {
            return  new SimpleDateFormat("yyyy-MM-dd").parse((date));
        } catch (final ParseException e) {
            Log.e("OkapiClient.parseDate", e);
        }
        return null;
    }


    private static List<String> parseAttributes(final JsonNode response) {

        final List<String> result = new ArrayList<>();
//"caravaneige":"0","electricite":"0","douche":"0","poubelle":"0","animaux":"0","eau_noire":"0","eau_usee":"0","boulangerie":"0","point_eau":"0","visites":"0","point_de_vue":"1","moto":"0","baignade":"0","piscine":"0","laverie":"0","jeux_enfants":null,"lavage":null,"gpl":null,"gaz":null,"donnees_mobile":"0","windsurf":"0","vtt":"1","rando":"0","wc_public":"0","escalade":"0","eaux_vives":"0","wifi":"0","peche":"0","peche_pied":"0"

        // Ajout des services

        //electricite
        if (response.get("electricite").asInt(0) > 0)
        result.add("P4NService_electricite");
        //eau
        if (response.get("point_eau").asInt(0) > 0)
            result.add("P4NService_point_eau");
        //vidange eaux noires
        if (response.get("eau_noire").asInt(0) > 0)
            result.add("P4NService_eaunoire");
        //vidange eaux grise
        if (response.get("eau_usee").asInt(0) > 0)
            result.add("P4NService_eau_usee");
        //poubelle
        if (response.get("poubelle").asInt(0) > 0)
            result.add("P4NService_poubelle");
        //boulangerie
        if (response.get("boulangerie").asInt(0) > 0)
            result.add("P4NService_boulangerie");
        //wc_public
        if (response.get("wc_public").asInt(0) > 0)
            result.add("P4NService_wc_public");
        //douche
        if (response.get("douche").asInt(0) > 0)
            result.add("P4NService_douche");
        //wifi
        if (response.get("wifi").asInt(0) > 0)
            result.add("P4NService_wifi");
        //caravaneige
        if (response.get("caravaneige").asInt(0) > 0)
            result.add("P4NService_caravaneige");
        //animaux
        if (response.get("animaux").asInt(0) > 0)
            result.add("P4NService_animaux");
        //piscine
        if (response.get("piscine").asInt(0) > 0)
            result.add("P4NService_piscine");
        //machine à laver
        if (response.get("laverie").asInt(0) > 0)
            result.add("P4NService_laverie");
        //gpl
        if (response.get("gpl").asInt(0) > 0)
            result.add("P4NService_gpl");
        //gaz
        if (response.get("gaz").asInt(0) > 0)
            result.add("P4NService_gaz");
        //donnees_mobile
        if (response.get("donnees_mobile").asInt(0) > 0)
            result.add("P4NService_donnees_mobile");
        //lavage
        if (response.get("lavage").asInt(0) > 0)
            result.add("P4NService_lavage");

        // Ajout des activités
        //"jeux_enfants"
        if (response.get("jeux_enfants").asInt(0) > 0)
            result.add("P4NActivite_jeux_enfants");
        //"point_de_vue"
        if (response.get("point_de_vue").asInt(0) > 0)
            result.add("P4NActivite_point_de_vue");
        //"baignade"
        if (response.get("baignade").asInt(0) > 0)
            result.add("P4NActivite_baignade");
        //"escalade"
        if (response.get("escalade").asInt(0) > 0)
            result.add("P4NActivite_escalade");
        //"eaux_vives"
        if (response.get("eaux_vives").asInt(0) > 0)
            result.add("P4NActivite_eaux_vives");
        //"peche"
        if (response.get("peche").asInt(0) > 0)
            result.add("P4NActivite_peche");
        //"peche_pied"
        if (response.get("peche_pied").asInt(0) > 0)
            result.add("P4NActivite_peche_pied");
        //"rando"
        if (response.get("rando").asInt(0) > 0)
            result.add("P4NActivite_rando");
        //"visites"
        if (response.get("visites").asInt(0) > 0)
            result.add("P4NActivite_visites");
        //"vtt"
        if (response.get("vtt").asInt(0) > 0)
            result.add("P4NActivite_vtt");
        //"windsurf"
        if (response.get("windsurf").asInt(0) > 0)
            result.add("P4NActivite_windsurf");
        //"moto"
        if (response.get("moto").asInt(0) > 0)
            result.add("P4NActivite_moto");



        return result;
    }


    private static List<Image> parsePhotos(JsonNode response)
    {
        //"photos":[
        // {
        // "id":"1991",
        // "link_large":"https:\/\/cdn3.park4night.com\/lieu\/1901_2000\/1991_gd.jpg",
        // "link_thumb":"https:\/\/cdn3.park4night.com\/lieu\/1901_2000\/1991_pt.jpg",
        // "numero":"1","p4n_user_id":"1","pn_lieu_id":"3765"},
        // {"id":"5956",
        // "link_large":"https:\/\/cdn3.park4night.com\/lieu\/5901_6000\/5956_gd.jpg",
        // "link_thumb":"https:\/\/cdn3.park4night.com\/lieu\/5901_6000\/5956_pt.jpg",
        // "numero":"2","p4n_user_id":"1","pn_lieu_id":"3765"}]

        List<Image> photos = new ArrayList<>();

        final ArrayNode images = (ArrayNode) response.get("photos");
        if (images != null) {
            for (final JsonNode imageResponse: images) {
                final String title = imageResponse.get("pn_lieu_id").asText()+"_"+imageResponse.get("numero").asText();
                final String url = imageResponse.get("link_large").asText();
                // all images are added as spoiler images, although OKAPI has spoiler and non spoiler images
                //cache.addSpoiler(new Image.Builder().setUrl(url).setTitle(title).build());
                photos.add(new Image.Builder().setUrl(url).setTitle(title).build());
            }
        }

        return photos;

    }
}
