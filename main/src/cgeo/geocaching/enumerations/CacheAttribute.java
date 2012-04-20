package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public enum CacheAttribute {
    UNKNOWN(0, "", R.drawable.attribute__strikethru, 0, 0),
    DOGS(1, "dogs", R.drawable.attribute_dogs, R.string.attribute_dogs_yes, R.string.attribute_dogs_no),
    FEE(2, "fee", R.drawable.attribute_fee, R.string.attribute_fee_yes, R.string.attribute_fee_no),
    RAPPELLING(3, "rappelling", R.drawable.attribute_rappelling, R.string.attribute_rappelling_yes, R.string.attribute_rappelling_no),
    BOAT(4, "boat", R.drawable.attribute_boat, R.string.attribute_boat_yes, R.string.attribute_boat_no),
    SCUBA(5, "scuba", R.drawable.attribute_scuba, R.string.attribute_scuba_yes, R.string.attribute_scuba_no),
    KIDS(6, "kids", R.drawable.attribute_kids, R.string.attribute_kids_yes, R.string.attribute_kids_no),
    ONEHOUR(7, "onehour", R.drawable.attribute_onehour, R.string.attribute_onehour_yes, R.string.attribute_onehour_no),
    SCENIC(8, "scenic", R.drawable.attribute_scenic, R.string.attribute_scenic_yes, R.string.attribute_scenic_no),
    HIKING(9, "hiking", R.drawable.attribute_hiking, R.string.attribute_hiking_yes, R.string.attribute_hiking_no),
    CLIMBING(10, "climbing", R.drawable.attribute_climbing, R.string.attribute_climbing_yes, R.string.attribute_climbing_no),
    WADING(11, "wading", R.drawable.attribute_wading, R.string.attribute_wading_yes, R.string.attribute_wading_no),
    SWIMMING(12, "swimming", R.drawable.attribute_swimming, R.string.attribute_swimming_yes, R.string.attribute_swimming_no),
    AVAILABLE(13, "available", R.drawable.attribute_available, R.string.attribute_available_yes, R.string.attribute_available_no),
    NIGHT(14, "night", R.drawable.attribute_night, R.string.attribute_night_yes, R.string.attribute_night_no),
    WINTER(15, "winter", R.drawable.attribute_winter, R.string.attribute_winter_yes, R.string.attribute_winter_no),
    POISONOAK(17, "poisonoak", R.drawable.attribute_poisonoak, R.string.attribute_poisonoak_yes, R.string.attribute_poisonoak_no),
    DANGEROUSANIMALS(18, "dangerousanimals", R.drawable.attribute_dangerousanimals, R.string.attribute_dangerousanimals_yes, R.string.attribute_dangerousanimals_no),
    TICKS(19, "ticks", R.drawable.attribute_ticks, R.string.attribute_ticks_yes, R.string.attribute_ticks_no),
    MINE(29, "mine", R.drawable.attribute_mine, R.string.attribute_mine_yes, R.string.attribute_mine_no),
    CLIFF(21, "cliff", R.drawable.attribute_cliff, R.string.attribute_cliff_yes, R.string.attribute_cliff_no),
    HUNTING(22, "hunting", R.drawable.attribute_hunting, R.string.attribute_hunting_yes, R.string.attribute_hunting_no),
    DANGER(23, "danger", R.drawable.attribute_danger, R.string.attribute_danger_yes, R.string.attribute_danger_no),
    WHEELCHAIR(24, "wheelchair", R.drawable.attribute_wheelchair, R.string.attribute_wheelchair_yes, R.string.attribute_wheelchair_no),
    PARKING(25, "parking", R.drawable.attribute_parking, R.string.attribute_parking_yes, R.string.attribute_parking_no),
    PUBLIC(26, "public", R.drawable.attribute_public, R.string.attribute_public_yes, R.string.attribute_public_no),
    WATER(27, "water", R.drawable.attribute_water, R.string.attribute_water_yes, R.string.attribute_water_no),
    RESTROOMS(28, "restrooms", R.drawable.attribute_restrooms, R.string.attribute_restrooms_yes, R.string.attribute_restrooms_no),
    PHONE(29, "phone", R.drawable.attribute_phone, R.string.attribute_phone_yes, R.string.attribute_phone_no),
    PICNIC(30, "picnic", R.drawable.attribute_picnic, R.string.attribute_picnic_yes, R.string.attribute_picnic_no),
    CAMPING(31, "camping", R.drawable.attribute_camping, R.string.attribute_camping_yes, R.string.attribute_camping_no),
    BICYCLES(32, "bicycles", R.drawable.attribute_bicycles, R.string.attribute_bicycles_yes, R.string.attribute_bicycles_no),
    MOTORCYCLES(33, "motorcycles", R.drawable.attribute_motorcycles, R.string.attribute_motorcycles_yes, R.string.attribute_motorcycles_no),
    QUADS(34, "quads", R.drawable.attribute_quads, R.string.attribute_quads_yes, R.string.attribute_quads_no),
    JEEPS(35, "jeeps", R.drawable.attribute_jeeps, R.string.attribute_jeeps_yes, R.string.attribute_jeeps_no),
    SNOWMOBILES(36, "snowmobiles", R.drawable.attribute_snowmobiles, R.string.attribute_snowmobiles_yes, R.string.attribute_snowmobiles_no),
    HORSES(37, "horses", R.drawable.attribute_horses, R.string.attribute_horses_yes, R.string.attribute_horses_no),
    CAMPFIRES(38, "campfires", R.drawable.attribute_campfires, R.string.attribute_campfires_yes, R.string.attribute_campfires_no),
    THORN(39, "thorn", R.drawable.attribute_thorn, R.string.attribute_thorn_yes, R.string.attribute_thorn_no),
    STEALTH(40, "stealth", R.drawable.attribute_stealth, R.string.attribute_stealth_yes, R.string.attribute_stealth_no),
    STROLLER(41, "stroller", R.drawable.attribute_stroller, R.string.attribute_stroller_yes, R.string.attribute_stroller_no),
    FIRSTAID(42, "firstaid", R.drawable.attribute_firstaid, R.string.attribute_firstaid_yes, R.string.attribute_firstaid_no),
    COW(43, "cow", R.drawable.attribute_cow, R.string.attribute_cow_yes, R.string.attribute_cow_no),
    FLASHLIGHT(44, "flashlight", R.drawable.attribute_flashlight, R.string.attribute_flashlight_yes, R.string.attribute_flashlight_no),
    LANDF(45, "landf", R.drawable.attribute_landf, R.string.attribute_landf_yes, R.string.attribute_landf_no),
    RV(46, "rv", R.drawable.attribute_rv, R.string.attribute_rv_yes, R.string.attribute_rv_no),
    FIELD_PUZZLE(47, "field_puzzle", R.drawable.attribute_field_puzzle, R.string.attribute_field_puzzle_yes, R.string.attribute_field_puzzle_no),
    UV(48, "uv", R.drawable.attribute_uv, R.string.attribute_uv_yes, R.string.attribute_uv_no),
    SNOWSHOES(49, "snowshoes", R.drawable.attribute_snowshoes, R.string.attribute_snowshoes_yes, R.string.attribute_snowshoes_no),
    SKIIS(50, "skiis", R.drawable.attribute_skiis, R.string.attribute_skiis_yes, R.string.attribute_skiis_no),
    SPECIAL_TOOLS(51, "s_tool", R.drawable.attribute_s_tool, R.string.attribute_s_tool_yes, R.string.attribute_s_tool_no),
    NIGHTCACHE(52, "nightcache", R.drawable.attribute_nightcache, R.string.attribute_nightcache_yes, R.string.attribute_nightcache_no),
    PARKNGRAB(53, "parkngrab", R.drawable.attribute_parkngrab, R.string.attribute_parkngrab_yes, R.string.attribute_parkngrab_no),
    ABANDONED_BUILDING(54, "abandonedbuilding", R.drawable.attribute_abandonedbuilding, R.string.attribute_abandonedbuilding_yes, R.string.attribute_abandonedbuilding_no),
    HIKE_SHORT(55, "hike_short", R.drawable.attribute_hike_short, R.string.attribute_hike_short_yes, R.string.attribute_hike_short_no),
    HIKE_MED(56, "hike_med", R.drawable.attribute_hike_med, R.string.attribute_hike_med_yes, R.string.attribute_hike_med_no),
    HIKE_LONG(57, "hike_long", R.drawable.attribute_hike_long, R.string.attribute_hike_long_yes, R.string.attribute_hike_long_no),
    FUEL(58, "fuel", R.drawable.attribute_fuel, R.string.attribute_fuel_yes, R.string.attribute_fuel_no),
    FOOD(59, "food", R.drawable.attribute_food, R.string.attribute_food_yes, R.string.attribute_food_no),
    WIRELESS_BEACON(60, "wirelessbeacon", R.drawable.attribute_wirelessbeacon, R.string.attribute_wirelessbeacon_yes, R.string.attribute_wirelessbeacon_no),
    PARTNERSHIP(61, "partnership", R.drawable.attribute_partnership, R.string.attribute_partnership_yes, R.string.attribute_partnership_no),
    SEASONAL(62, "seasonal", R.drawable.attribute_seasonal, R.string.attribute_seasonal_yes, R.string.attribute_seasonal_no),
    TOURIST_OK(63, "touristok", R.drawable.attribute_touristok, R.string.attribute_touristok_yes, R.string.attribute_touristok_no),
    TREECLIMBING(64, "treeclimbing", R.drawable.attribute_treeclimbing, R.string.attribute_treeclimbing_yes, R.string.attribute_treeclimbing_no),
    FRONTYARD(65, "frontyard", R.drawable.attribute_frontyard, R.string.attribute_frontyard_yes, R.string.attribute_frontyard_no),
    TEAMWORK(66, "teamwork", R.drawable.attribute_teamwork, R.string.attribute_teamwork_yes, R.string.attribute_teamwork_no);

    public static final String INTERNAL_PRE = "attribute_";
    public static final String INTERNAL_YES = "_yes";
    public static final String INTERNAL_NO = "_no";

    public final int id;
    public final String gcRawName;
    public final int drawableId;
    public final int stringIdYes;
    public final int stringIdNo;

    private CacheAttribute(final int id, final String gcRawName, final int drawableId, final int stringIdYes, final int stringIdNo) {
        this.id = id;
        this.gcRawName = gcRawName;
        this.drawableId = drawableId;
        this.stringIdYes = stringIdYes;
        this.stringIdNo = stringIdNo;
    }

    public String getL10n(final boolean enabled) {
        return cgeoapplication.getInstance().getResources().getString(enabled ? stringIdYes : stringIdNo);
    }

    private final static Map<String, CacheAttribute> FIND_BY_GCRAWNAME;

    static {
        final HashMap<String, CacheAttribute> mapGcRawNames = new HashMap<String, CacheAttribute>();
        for (CacheAttribute attr : values()) {
            mapGcRawNames.put(attr.gcRawName, attr);
        }
        FIND_BY_GCRAWNAME = Collections.unmodifiableMap(mapGcRawNames);
    }

    public static CacheAttribute getById(final int id) {
        for (CacheAttribute attr : values()) {
            if (attr.id == id) {
                return attr;
            }
        }
        return UNKNOWN;
    }

    public static CacheAttribute getByGcRawName(final String gcRawName) {
        final CacheAttribute result = gcRawName != null ? FIND_BY_GCRAWNAME.get(gcRawName) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    public static String trimAttributeName(String attributeName) {
        if (null == attributeName) {
            return "";
        }
        return attributeName.replace(INTERNAL_PRE, "").replace(INTERNAL_YES, "").replace(INTERNAL_NO, "").trim();
    }
}
