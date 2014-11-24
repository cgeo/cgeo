package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import org.apache.commons.lang3.StringUtils;

import android.util.SparseArray;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum CacheAttribute {
    // THIS LIST IS GENERATED: don't change anything here but read
    // project/attributes/readme.txt
    DOGS(1, -1, "dogs", R.drawable.attribute_dogs, R.string.attribute_dogs_yes, R.string.attribute_dogs_no),
    BICYCLES(32, 27, "bicycles", R.drawable.attribute_bicycles, R.string.attribute_bicycles_yes, R.string.attribute_bicycles_no),
    MOTORCYCLES(33, -1, "motorcycles", R.drawable.attribute_motorcycles, R.string.attribute_motorcycles_yes, R.string.attribute_motorcycles_no),
    QUADS(34, -1, "quads", R.drawable.attribute_quads, R.string.attribute_quads_yes, R.string.attribute_quads_no),
    JEEPS(35, -1, "jeeps", R.drawable.attribute_jeeps, R.string.attribute_jeeps_yes, R.string.attribute_jeeps_no),
    SNOWMOBILES(36, -1, "snowmobiles", R.drawable.attribute_snowmobiles, R.string.attribute_snowmobiles_yes, R.string.attribute_snowmobiles_no),
    HORSES(37, -1, "horses", R.drawable.attribute_horses, R.string.attribute_horses_yes, R.string.attribute_horses_no),
    CAMPFIRES(38, -1, "campfires", R.drawable.attribute_campfires, R.string.attribute_campfires_yes, R.string.attribute_campfires_no),
    RV(46, -1, "rv", R.drawable.attribute_rv, R.string.attribute_rv_yes, R.string.attribute_rv_no),
    KIDS(6, 71, "kids", R.drawable.attribute_kids, R.string.attribute_kids_yes, R.string.attribute_kids_no),
    ONEHOUR(7, -1, "onehour", R.drawable.attribute_onehour, R.string.attribute_onehour_yes, R.string.attribute_onehour_no),
    SCENIC(8, -1, "scenic", R.drawable.attribute_scenic, R.string.attribute_scenic_yes, R.string.attribute_scenic_no),
    HIKING(9, 21, "hiking", R.drawable.attribute_hiking, R.string.attribute_hiking_yes, R.string.attribute_hiking_no),
    CLIMBING(10, -1, "climbing", R.drawable.attribute_climbing, R.string.attribute_climbing_yes, R.string.attribute_climbing_no),
    WADING(11, -1, "wading", R.drawable.attribute_wading, R.string.attribute_wading_yes, R.string.attribute_wading_no),
    SWIMMING(12, 25, "swimming", R.drawable.attribute_swimming, R.string.attribute_swimming_yes, R.string.attribute_swimming_no),
    AVAILABLE(13, 39, "available", R.drawable.attribute_available, R.string.attribute_available_yes, R.string.attribute_available_no),
    NIGHT(14, 42, "night", R.drawable.attribute_night, R.string.attribute_night_yes, R.string.attribute_night_no),
    WINTER(15, -1, "winter", R.drawable.attribute_winter, R.string.attribute_winter_yes, R.string.attribute_winter_no),
    STEALTH(40, -1, "stealth", R.drawable.attribute_stealth, R.string.attribute_stealth_yes, R.string.attribute_stealth_no),
    FIRSTAID(42, -1, "firstaid", R.drawable.attribute_firstaid, R.string.attribute_firstaid_yes, R.string.attribute_firstaid_no),
    COW(43, -1, "cow", R.drawable.attribute_cow, R.string.attribute_cow_yes, R.string.attribute_cow_no),
    FIELD_PUZZLE(47, -1, "field_puzzle", R.drawable.attribute_field_puzzle, R.string.attribute_field_puzzle_yes, R.string.attribute_field_puzzle_no),
    NIGHTCACHE(52, 43, "nightcache", R.drawable.attribute_nightcache, R.string.attribute_nightcache_yes, R.string.attribute_nightcache_no),
    PARKNGRAB(53, 19, "parkngrab", R.drawable.attribute_parkngrab, R.string.attribute_parkngrab_yes, R.string.attribute_parkngrab_no),
    ABANDONEDBUILDING(54, -1, "abandonedbuilding", R.drawable.attribute_abandonedbuilding, R.string.attribute_abandonedbuilding_yes, R.string.attribute_abandonedbuilding_no),
    HIKE_SHORT(55, -1, "hike_short", R.drawable.attribute_hike_short, R.string.attribute_hike_short_yes, R.string.attribute_hike_short_no),
    HIKE_MED(56, -1, "hike_med", R.drawable.attribute_hike_med, R.string.attribute_hike_med_yes, R.string.attribute_hike_med_no),
    HIKE_LONG(57, -1, "hike_long", R.drawable.attribute_hike_long, R.string.attribute_hike_long_yes, R.string.attribute_hike_long_no),
    SEASONAL(62, 45, "seasonal", R.drawable.attribute_seasonal, R.string.attribute_seasonal_yes, R.string.attribute_seasonal_no),
    TOURISTOK(63, -1, "touristok", R.drawable.attribute_touristok, R.string.attribute_touristok_yes, R.string.attribute_touristok_no),
    FRONTYARD(65, -1, "frontyard", R.drawable.attribute_frontyard, R.string.attribute_frontyard_yes, R.string.attribute_frontyard_no),
    TEAMWORK(66, -1, "teamwork", R.drawable.attribute_teamwork, R.string.attribute_teamwork_yes, R.string.attribute_teamwork_no),
    LANDF(45, -1, "landf", R.drawable.attribute_landf, R.string.attribute_landf_yes, R.string.attribute_landf_no),
    PARTNERSHIP(61, -1, "partnership", R.drawable.attribute_partnership, R.string.attribute_partnership_yes, R.string.attribute_partnership_no),
    FEE(2, 26, "fee", R.drawable.attribute_fee, R.string.attribute_fee_yes, R.string.attribute_fee_no),
    RAPPELLING(3, 53, "rappelling", R.drawable.attribute_rappelling, R.string.attribute_rappelling_yes, R.string.attribute_rappelling_no),
    BOAT(4, 57, "boat", R.drawable.attribute_boat, R.string.attribute_boat_yes, R.string.attribute_boat_no),
    SCUBA(5, 55, "scuba", R.drawable.attribute_scuba, R.string.attribute_scuba_yes, R.string.attribute_scuba_no),
    FLASHLIGHT(44, 52, "flashlight", R.drawable.attribute_flashlight, R.string.attribute_flashlight_yes, R.string.attribute_flashlight_no),
    UV(48, -1, "uv", R.drawable.attribute_uv, R.string.attribute_uv_yes, R.string.attribute_uv_no),
    SNOWSHOES(49, -1, "snowshoes", R.drawable.attribute_snowshoes, R.string.attribute_snowshoes_yes, R.string.attribute_snowshoes_no),
    SKIIS(50, -1, "skiis", R.drawable.attribute_skiis, R.string.attribute_skiis_yes, R.string.attribute_skiis_no),
    S_TOOL(51, 56, "s_tool", R.drawable.attribute_s_tool, R.string.attribute_s_tool_yes, R.string.attribute_s_tool_no),
    WIRELESSBEACON(60, 9, "wirelessbeacon", R.drawable.attribute_wirelessbeacon, R.string.attribute_wirelessbeacon_yes, R.string.attribute_wirelessbeacon_no),
    TREECLIMBING(64, -1, "treeclimbing", R.drawable.attribute_treeclimbing, R.string.attribute_treeclimbing_yes, R.string.attribute_treeclimbing_no),
    POISONOAK(17, 66, "poisonoak", R.drawable.attribute_poisonoak, R.string.attribute_poisonoak_yes, R.string.attribute_poisonoak_no),
    DANGEROUSANIMALS(18, 67, "dangerousanimals", R.drawable.attribute_dangerousanimals, R.string.attribute_dangerousanimals_yes, R.string.attribute_dangerousanimals_no),
    TICKS(19, 64, "ticks", R.drawable.attribute_ticks, R.string.attribute_ticks_yes, R.string.attribute_ticks_no),
    MINE(20, 65, "mine", R.drawable.attribute_mine, R.string.attribute_mine_yes, R.string.attribute_mine_no),
    CLIFF(21, 61, "cliff", R.drawable.attribute_cliff, R.string.attribute_cliff_yes, R.string.attribute_cliff_no),
    HUNTING(22, 62, "hunting", R.drawable.attribute_hunting, R.string.attribute_hunting_yes, R.string.attribute_hunting_no),
    DANGER(23, 59, "danger", R.drawable.attribute_danger, R.string.attribute_danger_yes, R.string.attribute_danger_no),
    THORN(39, 63, "thorn", R.drawable.attribute_thorn, R.string.attribute_thorn_yes, R.string.attribute_thorn_no),
    WHEELCHAIR(24, 18, "wheelchair", R.drawable.attribute_wheelchair, R.string.attribute_wheelchair_yes, R.string.attribute_wheelchair_no),
    PARKING(25, 33, "parking", R.drawable.attribute_parking, R.string.attribute_parking_yes, R.string.attribute_parking_no),
    PUBLIC(26, 34, "public", R.drawable.attribute_public, R.string.attribute_public_yes, R.string.attribute_public_no),
    WATER(27, 35, "water", R.drawable.attribute_water, R.string.attribute_water_yes, R.string.attribute_water_no),
    RESTROOMS(28, 36, "restrooms", R.drawable.attribute_restrooms, R.string.attribute_restrooms_yes, R.string.attribute_restrooms_no),
    PHONE(29, 37, "phone", R.drawable.attribute_phone, R.string.attribute_phone_yes, R.string.attribute_phone_no),
    PICNIC(30, -1, "picnic", R.drawable.attribute_picnic, R.string.attribute_picnic_yes, R.string.attribute_picnic_no),
    CAMPING(31, -1, "camping", R.drawable.attribute_camping, R.string.attribute_camping_yes, R.string.attribute_camping_no),
    STROLLER(41, -1, "stroller", R.drawable.attribute_stroller, R.string.attribute_stroller_yes, R.string.attribute_stroller_no),
    FUEL(58, -1, "fuel", R.drawable.attribute_fuel, R.string.attribute_fuel_yes, R.string.attribute_fuel_no),
    FOOD(59, -1, "food", R.drawable.attribute_food, R.string.attribute_food_yes, R.string.attribute_food_no),
    OC_ONLY(-1, 1, "oc_only", R.drawable.attribute_oc_only, R.string.attribute_oc_only_yes, R.string.attribute_oc_only_no),
    LINK_ONLY(-1, -1, "link_only", R.drawable.attribute_link_only, R.string.attribute_link_only_yes, R.string.attribute_link_only_no),
    LETTERBOX(-1, 4, "letterbox", R.drawable.attribute_letterbox, R.string.attribute_letterbox_yes, R.string.attribute_letterbox_no),
    RAILWAY(-1, 60, "railway", R.drawable.attribute_railway, R.string.attribute_railway_yes, R.string.attribute_railway_no),
    SYRINGE(-1, 38, "syringe", R.drawable.attribute_syringe, R.string.attribute_syringe_yes, R.string.attribute_syringe_no),
    SWAMP(-1, 22, "swamp", R.drawable.attribute_swamp, R.string.attribute_swamp_yes, R.string.attribute_swamp_no),
    HILLS(-1, 23, "hills", R.drawable.attribute_hills, R.string.attribute_hills_yes, R.string.attribute_hills_no),
    EASY_CLIMBING(-1, 24, "easy_climbing", R.drawable.attribute_easy_climbing, R.string.attribute_easy_climbing_yes, R.string.attribute_easy_climbing_no),
    POI(-1, 30, "poi", R.drawable.attribute_poi, R.string.attribute_poi_yes, R.string.attribute_poi_no),
    MOVING_TARGET(-1, 11, "moving_target", R.drawable.attribute_moving_target, R.string.attribute_moving_target_yes, R.string.attribute_moving_target_no),
    WEBCAM(-1, 12, "webcam", R.drawable.attribute_webcam, R.string.attribute_webcam_yes, R.string.attribute_webcam_no),
    INSIDE(-1, 31, "inside", R.drawable.attribute_inside, R.string.attribute_inside_yes, R.string.attribute_inside_no),
    IN_WATER(-1, 32, "in_water", R.drawable.attribute_in_water, R.string.attribute_in_water_yes, R.string.attribute_in_water_no),
    NO_GPS(-1, 58, "no_gps", R.drawable.attribute_no_gps, R.string.attribute_no_gps_yes, R.string.attribute_no_gps_no),
    OVERNIGHT(-1, 69, "overnight", R.drawable.attribute_overnight, R.string.attribute_overnight_yes, R.string.attribute_overnight_no),
    SPECIFIC_TIMES(-1, 40, "specific_times", R.drawable.attribute_specific_times, R.string.attribute_specific_times_yes, R.string.attribute_specific_times_no),
    DAY(-1, 41, "day", R.drawable.attribute_day, R.string.attribute_day_yes, R.string.attribute_day_no),
    TIDE(-1, 48, "tide", R.drawable.attribute_tide, R.string.attribute_tide_yes, R.string.attribute_tide_no),
    ALL_SEASONS(-1, 44, "all_seasons", R.drawable.attribute_all_seasons, R.string.attribute_all_seasons_yes, R.string.attribute_all_seasons_no),
    BREEDING(-1, 46, "breeding", R.drawable.attribute_breeding, R.string.attribute_breeding_yes, R.string.attribute_breeding_no),
    SNOW_PROOF(-1, 47, "snow_proof", R.drawable.attribute_snow_proof, R.string.attribute_snow_proof_yes, R.string.attribute_snow_proof_no),
    COMPASS(-1, 49, "compass", R.drawable.attribute_compass, R.string.attribute_compass_yes, R.string.attribute_compass_no),
    CAVE(-1, 54, "cave", R.drawable.attribute_cave, R.string.attribute_cave_yes, R.string.attribute_cave_no),
    AIRCRAFT(-1, -1, "aircraft", R.drawable.attribute_aircraft, R.string.attribute_aircraft_yes, R.string.attribute_aircraft_no),
    INVESTIGATION(-1, 14, "investigation", R.drawable.attribute_investigation, R.string.attribute_investigation_yes, R.string.attribute_investigation_no),
    PUZZLE(-1, 15, "puzzle", R.drawable.attribute_puzzle, R.string.attribute_puzzle_yes, R.string.attribute_puzzle_no),
    ARITHMETIC(-1, 16, "arithmetic", R.drawable.attribute_arithmetic, R.string.attribute_arithmetic_yes, R.string.attribute_arithmetic_no),
    OTHER_CACHE(-1, 13, "other_cache", R.drawable.attribute_other_cache, R.string.attribute_other_cache_yes, R.string.attribute_other_cache_no),
    ASK_OWNER(-1, 17, "ask_owner", R.drawable.attribute_ask_owner, R.string.attribute_ask_owner_yes, R.string.attribute_ask_owner_no),
    UNKNOWN(-1, -1, "unknown", R.drawable.attribute_unknown, R.string.attribute_unknown_yes, R.string.attribute_unknown_no),
    GEOTOUR(67, -1, "geotour", R.drawable.attribute_geotour, R.string.attribute_geotour_yes, R.string.attribute_geotour_no),
    KIDS_2(-1, 70, "kids_2", R.drawable.attribute_kids_2, R.string.attribute_kids_2_yes, R.string.attribute_kids_2_no),
    HISTORIC_SITE(-1, 29, "historic_site", R.drawable.attribute_historic_site, R.string.attribute_historic_site_yes, R.string.attribute_historic_site_no),
    MAGNETIC(-1, 6, "magnetic", R.drawable.attribute_magnetic, R.string.attribute_magnetic_yes, R.string.attribute_magnetic_no),
    USB_CACHE(-1, 10, "usb_cache", R.drawable.attribute_usb_cache, R.string.attribute_usb_cache_yes, R.string.attribute_usb_cache_no),
    SHOVEL(-1, 51, "shovel", R.drawable.attribute_shovel, R.string.attribute_shovel_yes, R.string.attribute_shovel_no),
    SPECIFIC_ACCESS(-1, 73, "specific_access", R.drawable.attribute_specific_access, R.string.attribute_specific_access_yes, R.string.attribute_specific_access_no),
    PEDESTRIAN_ONLY(-1, 20, "pedestrian_only", R.drawable.attribute_pedestrian_only, R.string.attribute_pedestrian_only_yes, R.string.attribute_pedestrian_only_no),
    NATURE_CACHE(-1, 28, "nature_cache", R.drawable.attribute_nature_cache, R.string.attribute_nature_cache_yes, R.string.attribute_nature_cache_no),
    BYOP(-1, 50, "byop", R.drawable.attribute_byop, R.string.attribute_byop_yes, R.string.attribute_byop_no),
    SAFARI_CACHE(-1, 72, "safari_cache", R.drawable.attribute_safari_cache, R.string.attribute_safari_cache_yes, R.string.attribute_safari_cache_no),
    QUICK_CACHE(-1, 68, "quick_cache", R.drawable.attribute_quick_cache, R.string.attribute_quick_cache_yes, R.string.attribute_quick_cache_no),
    WHERIGO(-1, 3, "wherigo", R.drawable.attribute_wherigo, R.string.attribute_wherigo_yes, R.string.attribute_wherigo_no),
    AUDIO_CACHE(-1, 7, "audio_cache", R.drawable.attribute_audio_cache, R.string.attribute_audio_cache_yes, R.string.attribute_audio_cache_no),
    GEOHOTEL(-1, 5, "geohotel", R.drawable.attribute_geohotel, R.string.attribute_geohotel_yes, R.string.attribute_geohotel_no),
    SURVEY_MARKER(-1, 2, "survey_marker", R.drawable.attribute_survey_marker, R.string.attribute_survey_marker_yes, R.string.attribute_survey_marker_no),
    OFFSET_CACHE(-1, 8, "offset_cache", R.drawable.attribute_offset_cache, R.string.attribute_offset_cache_yes, R.string.attribute_offset_cache_no);
    // THIS LIST IS GENERATED: don't change anything here but read
    // project/attributes/readme.txt

    private static final String INTERNAL_YES = "_yes";
    private static final String INTERNAL_NO = "_no";

    public static final int NO_ID = -1;

    public final int gcid;
    public final int ocacode;
    public final String rawName;
    public final int drawableId;
    public final int stringIdYes;
    public final int stringIdNo;

    CacheAttribute(final int gcid, final int ocacode, final String rawName,
            final int drawableId, final int stringIdYes, final int stringIdNo) {
        this.gcid = gcid;
        this.ocacode = ocacode;
        this.rawName = rawName;
        this.drawableId = drawableId;
        this.stringIdYes = stringIdYes;
        this.stringIdNo = stringIdNo;
    }

    /**
     * get localized text
     *
     * @param enabled
     *            true: for positive text, false: for negative text
     * @return the localized text
     */
    public String getL10n(final boolean enabled) {
        return CgeoApplication.getInstance().getResources().getString(
                enabled ? stringIdYes : stringIdNo);
    }

    private final static Map<String, CacheAttribute> FIND_BY_GCRAWNAME;
    private final static SparseArray<CacheAttribute> FIND_BY_OCACODE = new SparseArray<>();
    static {
        final HashMap<String, CacheAttribute> mapGcRawNames = new HashMap<>();
        for (final CacheAttribute attr : values()) {
            mapGcRawNames.put(attr.rawName, attr);
            if (attr.ocacode != NO_ID) {
                FIND_BY_OCACODE.put(attr.ocacode, attr);
            }
        }
        FIND_BY_GCRAWNAME = Collections.unmodifiableMap(mapGcRawNames);
    }

    public static CacheAttribute getByRawName(final String rawName) {
        return rawName != null ? FIND_BY_GCRAWNAME.get(rawName) : null;
    }

    public static CacheAttribute getByOcACode(final int ocAcode) {
        return FIND_BY_OCACODE.get(ocAcode);
    }

    public static String trimAttributeName(final String attributeName) {
        if (null == attributeName) {
            return "";
        }
        return attributeName.replace(INTERNAL_YES, "").replace(INTERNAL_NO, "").trim();
    }

    public static boolean isEnabled(final String attributeName) {
        return !StringUtils.endsWithIgnoreCase(attributeName, INTERNAL_NO);
    }

}
