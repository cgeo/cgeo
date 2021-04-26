package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Enum listing all cache types
 */
public enum CacheType {

    TRADITIONAL("traditional", "Traditional Cache", "32bc9333-5e52-4957-b0f6-5a2c8fc7b257", R.string.traditional, R.drawable.type_traditional, "2", R.drawable.dot_traditional),
    MULTI("multi", "Multi-cache", "a5f6d0ad-d2f2-4011-8c14-940a9ebf3c74", R.string.multi, R.drawable.type_multi, "3", R.drawable.dot_multi),
    MYSTERY("mystery", "Unknown Cache", "40861821-1835-4e11-b666-8d41064d03fe", R.string.mystery, R.drawable.type_mystery, "8", R.drawable.dot_mystery),
    LETTERBOX("letterbox", "Letterbox hybrid", "4bdd8fb2-d7bc-453f-a9c5-968563b15d24", R.string.letterbox, R.drawable.type_letterbox, "5", R.drawable.dot_mystery),
    EVENT("event", "Event Cache", "69eb8534-b718-4b35-ae3c-a856a55b0874", R.string.event, R.drawable.type_event, "6", R.drawable.dot_event),
    MEGA_EVENT("mega", "Mega-Event Cache", "69eb8535-b718-4b35-ae3c-a856a55b0874", R.string.mega, R.drawable.type_mega, "453", R.drawable.dot_event),
    GIGA_EVENT("giga", "Giga-Event Cache", "51420629-5739-4945-8bdd-ccfd434c0ead", R.string.giga, R.drawable.type_giga, "7005", R.drawable.dot_event),
    EARTH("earth", "Earthcache", "c66f5cf3-9523-4549-b8dd-759cd2f18db8", R.string.earth, R.drawable.type_earth, "137", R.drawable.dot_virtual),
    CITO("cito", "Cache In Trash Out Event", "57150806-bc1a-42d6-9cf0-538d171a2d22", R.string.cito, R.drawable.type_cito, "13", R.drawable.dot_event),
    WEBCAM("webcam", "Webcam Cache", "31d2ae3c-c358-4b5f-8dcd-2185bf472d3d", R.string.webcam, R.drawable.type_webcam, "11", R.drawable.dot_virtual),
    VIRTUAL("virtual", "Virtual Cache", "294d4360-ac86-4c83-84dd-8113ef678d7e", R.string.virtual, R.drawable.type_virtual, "4", R.drawable.dot_virtual),
    WHERIGO("wherigo", "Wherigo Cache", "0544fa55-772d-4e5c-96a9-36a51ebcf5c9", R.string.wherigo, R.drawable.type_wherigo, "1858", R.drawable.dot_mystery),
    COMMUN_CELEBRATION("communceleb", "Community Celebration Event", "3ea6533d-bb52-42fe-b2d2-79a3424d4728", R.string.communceleb, R.drawable.type_event, "3653", R.drawable.dot_event), // icon missing
    PROJECT_APE("ape", "Project Ape Cache", "2555690d-b2bc-4b55-b5ac-0cb704c0b768", R.string.ape, R.drawable.type_ape, "9", R.drawable.dot_traditional),
    GCHQ("gchq", "Geocaching HQ", "416f2494-dc17-4b6a-9bab-1a29dd292d8c", R.string.gchq, R.drawable.type_hq, "3773", R.drawable.dot_traditional),
    GCHQ_CELEBRATION("gchqceleb", "Geocaching HQ Celebration", "af820035-787a-47af-b52b-becc8b0c0c88", R.string.gchqceleb, R.drawable.type_hq, "3774", R.drawable.dot_event), // icon missing
    GPS_EXHIBIT("gps", "GPS Adventures Exhibit", "72e69af2-7986-4990-afd9-bc16cbbb4ce3", R.string.gps, R.drawable.type_event, "1304", R.drawable.dot_event), // icon missing
    BLOCK_PARTY("block", "Geocaching HQ Block Party", "bc2f3df2-1aab-4601-b2ff-b5091f6c02e3", R.string.block, R.drawable.type_event, "4738", R.drawable.dot_event), // icon missing
    LOCATIONLESS("locationless", "Locationless (Reverse) Cache", "8f6dd7bc-ff39-4997-bd2e-225a0d2adf9d", R.string.locationless, R.drawable.type_virtual, "12", R.drawable.dot_virtual), // icon missing
    ADVLAB("advlab", "Adventure Lab Cache", "", R.string.advlab, R.drawable.type_advlab, "", R.drawable.dot_advlab),

    // insert other official cache types before USER_DEFINED and UNKNOWN
    USER_DEFINED("userdefined", "User defined cache", "", R.string.userdefined, R.drawable.type_cgeo, "", R.drawable.dot_virtual),
    UNKNOWN("unknown", "unknown", "", R.string.unknown, R.drawable.type_unknown, "", R.drawable.dot_unknown),

    /// P4N TYPES
    P4NACC_G("acc_g", "Aire gratuite", "67776548-1537-490a-bb89-e6eb433a3c91", R.string.airegratuite, R.drawable.pins_acc_g, "ACC_G", R.drawable.dot_traditional),
    P4NACC_P( "acc_p","Aire payante","5581c22e-ddc8-42e6-8c00-73e5f7059105",R.string.airepayante,R.drawable.pins_acc_p,"ACC_P",R.drawable.dot_event),
    P4NACC_PR("acc_pr", "Aire privée", "5631cb1c-f2c5-4d15-88d1-9a5c8e6ea286", R.string.aireprivee, R.drawable.pins_acc_pr, "ACC_PR", R.drawable.dot_virtual),
    P4NEP("ep", "Particulier", "39001229-8e4a-487e-bcb6-67ff90afcd38", R.string.accueilparticulier, R.drawable.pins_ep, "EP", R.drawable.dot_virtual),
    P4NASS("ass", "Service", "200ed9fb-93f6-4516-a4d9-7d8eef68af22", R.string.serviceonly, R.drawable.pins_ass, "ASS", R.drawable.dot_event),
    P4NAPN("apn", "Aire piquenique", "aad6c817-c274-49d9-beeb-a85ad124a3a0", R.string.airepiquenique, R.drawable.pins_apn, "APN", R.drawable.dot_traditional),
    P4NAR("ar", "Aire repos", "c0e959f2-1a28-4071-a2fb-47bdc682b300", R.string.airerepos, R.drawable.pins_ar, "AR", R.drawable.dot_virtual),
    P4NC("c", "Camping", "8ff4b3a0-a957-4126-af4d-94363046b277", R.string.camping, R.drawable.pins_c, "C", R.drawable.dot_event),
    P4NF("f", "Ferme", "02bbd9bc-20d4-4e35-8944-624622748aaf", R.string.ferme, R.drawable.pins_f, "F", R.drawable.dot_event),
    P4NPN("pn", "Aire naturelle", "4bc39818-1a47-4c77-9799-727c7a850675", R.string.airenaturelle, R.drawable.pins_pn, "PN", R.drawable.dot_traditional),
    P4NP("p", "Parking", "50ae28b4-be87-489f-be9f-683224af7f73", R.string.parkingbleu, R.drawable.pins_p, "P", R.drawable.dot_mystery),
    P4NPJ("pj", "Parking jour", "37071e17-a909-43a2-9cf4-17c64fed9e9a", R.string.parkingjour, R.drawable.pins_pj, "PJ", R.drawable.dot_multi),
    P4NOR("or", "Off road", "c5fd4aa0-571d-4d54-8a1e-e05b1ac64195", R.string.offroad, R.drawable.pins_or, "OR", R.drawable.dot_multi),
    P4NDS("ds", "multi service", "7a3f9074-4ee6-467d-9e32-3b0f7011af11", R.string.multiservice, R.drawable.pins_ds, "DS", R.drawable.dot_event),

    /// WAZE TYPES
    WAZEROAD_CLOSED("road_closed", "Road closed", "43aa8d1c-8fb8-4880-a84e-95f616eac9e7", R.string.road_closed, R.drawable.waz_road_closed, "ROAD_CLOSED", R.drawable.dot_event),
    WAZEACCIDENT("accident", "Accident", "f42226b7-6c12-429f-8528-7fb98aca24a4", R.string.accident, R.drawable.waz_accident, "ACCIDENT", R.drawable.dot_event),
    WAZEACCIDENT_MAJOR("accidentmajor", "Accident major", "ce0939f6-5414-4803-a23a-a4cacb99dc92", R.string.waz_accident_major, R.drawable.waz_accident_major, "ACCIDENT_MAJOR", R.drawable.dot_event),
    WAZEACCIDENT_MINOR("accidentminor", "Accident minor", "8308502b-efaf-44a5-9945-ffb1785c5cf6", R.string.waz_accident_minor, R.drawable.waz_accident_minor, "ACCIDENT_MINOR", R.drawable.dot_event),
    WAZEJAM("jam", "Jam", "f1cce2f2-81a7-4a33-92da-5067e142a4dc", R.string.jam, R.drawable.waz_jam, "JAM", R.drawable.dot_event),
    WAZEJAM_MODERATE("jammoderate", "Jam moderate", "bded9b7a-f14e-43ad-af7c-a9bdbcaa9dee", R.string.jam_moderate, R.drawable.waz_jam_moderate_traffic, "JAM_MODERATE_TRAFFIC", R.drawable.dot_event),
    WAZEJAM_HEAVY("jamheavy", "Jam heavy", "26f744e0-5049-4c2a-978b-43834ff70fb1", R.string.jam_heavy, R.drawable.waz_jam_heavy_traffic, "JAM_HEAVY_TRAFFIC", R.drawable.dot_event),
    WAZEJAM_STANDSTILL("jamstandstill", "Jam standstill", "ab230478-728c-4e74-a069-00b037e48181", R.string.jam_standstill, R.drawable.waz_jam_standstill_traffic, "JAM_STAND_STILL_TRAFFIC", R.drawable.dot_event),
    WAZEPOLICE("police", "Police", "01af5a1c-d836-426e-aebe-bf07e7259039", R.string.police, R.drawable.waz_police, "POLICE", R.drawable.dot_event),
    WAZEPOLICE_HIDING("policehiding", "Police hiding", "2fcf61e2-9488-4d9b-9820-2ea8fd956f6d", R.string.police_hiding, R.drawable.waz_police_hidden, "POLICE_HIDING", R.drawable.dot_event),
    WAZEHAZARD("hazard", "Hazard", "6cea7263-e144-4bd4-b2df-3ba46373b012", R.string.hazard, R.drawable.waz_hazard, "HAZARD", R.drawable.dot_event),
    WAZEHAZARD_WEATHER_FLOOD("hazardweatherflood", "Hazard weather flood", "eae3b928-2851-4180-9a57-3d20a9efb52c", R.string.hazard_weather_flood, R.drawable.waz_hazard_weather_flood, "HAZARD_WEATHER_FLOOD", R.drawable.dot_event),
    WAZEHAZARD_ON_ROAD("hazardonroad", "Hazard on road", "916b6f42-a013-415d-a3cd-5281f5d8f5c5", R.string.hazard_on_road, R.drawable.waz_hazard_on_road, "HAZARD_ON_ROAD", R.drawable.dot_event),
    WAZEHAZARD_ON_SHOULDER("hazardonshoulder", "Hazard on shoulder", "d759de0d-bd37-4552-955f-acf66959fdb1", R.string.hazard_on_shoulder, R.drawable.waz_hazard_on_shoulder, "HAZARD_ON_SHOULDER", R.drawable.dot_event),
    WAZEHAZARD_WEATHER("hazardweather", "Hazard weather", "4cf06fb5-531a-4446-b483-3c217f2becab", R.string.hazard_weather, R.drawable.waz_hazard_weather, "HAZARD_WEATHER", R.drawable.dot_event),
    WAZEHAZARD_ON_ROAD_OBJECT("hazardonroadobject", "Hazard on road object", "0dc4ac55-de87-4865-8a5c-29ca749faf4b", R.string.hazard_on_road_object, R.drawable.waz_hazard_object, "HAZARD_ON_ROAD_OBJECT", R.drawable.dot_event),
    WAZEHAZARD_ON_ROAD_POT_HOLE("hazardonroadpothole", "Hazard on road pot hole", "538fee54-163b-4ddd-85bc-1a9a59f38608", R.string.hazard_on_road_pot_hole, R.drawable.waz_hazard_pothole, "HAZARD_ON_ROAD_POT_HOLE", R.drawable.dot_event),
    WAZEHAZARD_ON_ROAD_ROAD_KILL("hazardonroadkill", "Hazard on road kill", "b65529f4-ddfb-43b6-b7bd-89907b11b75a", R.string.hazard_on_road_kill, R.drawable.waz_hazard_roadkill, "HAZARD_ON_ROAD_ROAD_KILL", R.drawable.dot_event),
    WAZEHAZARD_ON_ROAD_CAR_STOPPED("hazardonroadcarstopped", "Hazard on road car stopped", "313ef2eb-3a1b-466f-83e8-2732d3400a14", R.string.hazard_on_road_car_stopped, R.drawable.waz_hazard_stopped, "HAZARD_ON_ROAD_CAR_STOPPED", R.drawable.dot_event),
    WAZEHAZARD_ON_SHOULDER_ANIMALS("hazardonshoulderanimals", "Hazard on shoulder animals", "d8a16bb6-dcf6-4516-9929-5496fa763969", R.string.hazard_on_shoulder_animals, R.drawable.waz_hazard_animals, "HAZARD_ON_SHOULDER_ANIMALS", R.drawable.dot_event),
    WAZEHAZARD_WEATHER_FOG("hazardweatherfog", "Hazard weather fog", "b0aca713-c27e-4061-8621-ba765f46d27f", R.string.waz_hazard_weather_fog, R.drawable.waz_hazard_weather_fog, "HAZARD_WEATHER_FOG", R.drawable.dot_event),
    WAZEHAZARD_WEATHER_HAIL("hazardweatherhail", "Hazard weather hail", "d084e165-dc19-458e-b7d6-1ac9ee909bda", R.string.waz_hazard_weather_hail, R.drawable.waz_hazard_weather_hail, "HAZARD_WEATHER_HAIL", R.drawable.dot_event),
    WAZEHAZARD_ON_ROAD_ICE("hazardonroadice", "Hazard on road ice", "eaa78957-36c9-4707-b5d4-4f14cef8335c", R.string.waz_hazard_on_road_ice, R.drawable.waz_hazard_weather_ice, "HAZARD_ON_ROAD_ICE", R.drawable.dot_event),
    WAZEHAZARD_ON_ROAD_CONSTRUCTION("hazardonroadconstruction", "Hazard on road construction", "a2e46cd3-f7bd-4c20-8069-88d6a9aef7cd", R.string.hazard_on_road_construction, R.drawable.waz_hazard_construction, "HAZARD_ON_ROAD_CONSTRUCTION", R.drawable.dot_event),
    WAZEHAZARD_ON_ROAD_OIL("hazardonroadoil", "Hazard on road oil", "7d57da31-9443-4014-b728-f4e27dc6000e", R.string.hazard_on_road_oil, R.drawable.waz_hazard_oilspill, "HAZARD_ON_ROAD_OIL", R.drawable.dot_event),
    WAZECHIT_CHAT("chit_chat", "Chit chat", "e1e8f216-2c8c-45ea-8248-621f3216280d", R.string.chit_chat, R.drawable.waz_chit_chat, "CHIT_CHAT", R.drawable.dot_event),


    /** No real cache type -> filter */
    ALL("all", "display all caches", "9a79e6ce-3344-409c-bbe9-496530baf758", R.string.all_types, R.drawable.type_unknown, "", R.drawable.dot_unknown);

    /**
     * id field is used for storing caches in the database.
     */
    public final String id;
    /**
     * human readable name of the cache type<br>
     * used in web parsing as well as for gpx import/export.
     */
    public final String pattern;
    public final String guid;
    private final int stringId;
    public final int markerId;
    @NonNull public final String wptTypeId;
    public final int dotMarkerId;

    CacheType(final String id, final String pattern, final String guid, final int stringId, final int markerId, @NonNull final String wptTypeId, final int dotMarkerId) {
        this.id = id;
        this.pattern = pattern;
        this.guid = guid;
        this.stringId = stringId;
        this.markerId = markerId;
        this.wptTypeId = wptTypeId;
        this.dotMarkerId = dotMarkerId;
    }

    @NonNull
    private static final Map<String, CacheType> FIND_BY_ID = new HashMap<>();
    @NonNull
    private static final Map<String, CacheType> FIND_BY_PATTERN = new HashMap<>();
    @NonNull
    private static final Map<String, CacheType> FIND_BY_GUID = new HashMap<>();
    @NonNull private static final Map<String, CacheType> FIND_BY_WPT_TYPE = new HashMap<>();

    static {
        for (final CacheType ct : values()) {
            FIND_BY_ID.put(ct.id, ct);
            FIND_BY_PATTERN.put(ct.pattern.toLowerCase(Locale.US), ct);
            FIND_BY_GUID.put(ct.guid, ct);
            if (StringUtils.isNotBlank(ct.wptTypeId)) {
                FIND_BY_WPT_TYPE.put(ct.wptTypeId, ct);
            }
        }
        // Add old mystery type for GPX file compatibility.
        FIND_BY_PATTERN.put("Mystery Cache".toLowerCase(Locale.US), MYSTERY);
        // This pattern briefly appeared on gc.com in 2014-08.
        FIND_BY_PATTERN.put("Traditional Geocache".toLowerCase(Locale.US), TRADITIONAL);
        // map lab caches to the virtual type for the time being
        FIND_BY_PATTERN.put("Lab Cache".toLowerCase(Locale.US), VIRTUAL);
        // renaming in 2019
        FIND_BY_PATTERN.put("Groundspeak HQ".toLowerCase(Locale.US), GCHQ);
        FIND_BY_PATTERN.put("Groundspeak Block Party".toLowerCase(Locale.US), BLOCK_PARTY);
        FIND_BY_PATTERN.put("Lost and Found Event Cache".toLowerCase(Locale.US), COMMUN_CELEBRATION);

        // Geocaching.ru
        FIND_BY_PATTERN.put("Multistep Traditional cache".toLowerCase(Locale.US), MULTI);
        FIND_BY_PATTERN.put("Multistep Virtual cache".toLowerCase(Locale.US), MYSTERY);
        FIND_BY_PATTERN.put("Contest".toLowerCase(Locale.US), EVENT);
        FIND_BY_PATTERN.put("Event".toLowerCase(Locale.US), EVENT);

        // OC, https://github.com/opencaching/gpx-extension-v1/blob/master/schema.xsd
        FIND_BY_PATTERN.put("Quiz Cache".toLowerCase(Locale.US), MYSTERY);
        FIND_BY_PATTERN.put("Moving Cache".toLowerCase(Locale.US), MYSTERY);
        FIND_BY_PATTERN.put("Podcast Cache".toLowerCase(Locale.US), MYSTERY);
        FIND_BY_PATTERN.put("Own Cache".toLowerCase(Locale.US), MYSTERY);
    }

    @NonNull
    public static CacheType getById(final String id) {
        final CacheType result = id != null ? FIND_BY_ID.get(id.toLowerCase(Locale.US).trim()) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    @NonNull
    public static CacheType getByPattern(final String pattern) {
        final CacheType result = pattern != null ? FIND_BY_PATTERN.get(pattern.toLowerCase(Locale.US).trim()) : null;
        if (result != null) {
            return result;
        }
        return UNKNOWN;
    }

    @NonNull
    public static CacheType getByGuid(final String guid) {
        final CacheType result = guid != null ? FIND_BY_GUID.get(guid) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    @NonNull
    public static CacheType getByWaypointType(final String typeNumber) {
        final CacheType result = typeNumber != null ? FIND_BY_WPT_TYPE.get(typeNumber) : null;
        if (result == null) {
            // earthcaches don't use their numeric ID on search result pages, but a literal "earthcache". therefore have a fallback
            return getByPattern(typeNumber);
        }
        return result;
    }

    @NonNull
    public final String getL10n() {
        return CgeoApplication.getInstance().getBaseContext().getString(stringId);
    }

    public boolean isEvent() {
        return this == EVENT || this == MEGA_EVENT || this == CITO || this == GIGA_EVENT || this == COMMUN_CELEBRATION ||
            this == BLOCK_PARTY || this == GPS_EXHIBIT  || this == GCHQ_CELEBRATION;
    }

    @Override
    public String toString() {
        return getL10n();
    }

    /**
     * Whether this type contains the given cache.
     *
     * @return true if this is the ALL type or if this type equals the type of the cache.
     */
    public boolean contains(final Geocache cache) {
        if (cache == null) {
            return false;
        }
        if (this == ALL) {
            return true;
        }
        return cache.getType() == this;
    }

    public boolean applyDistanceRule() {
        return this == TRADITIONAL || this == PROJECT_APE || this == GCHQ;
    }

    public boolean isVirtual() {
        return this == VIRTUAL || this == WEBCAM || this == EARTH || this == LOCATIONLESS;
    }
}
