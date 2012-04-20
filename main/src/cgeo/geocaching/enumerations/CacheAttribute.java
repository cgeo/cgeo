package cgeo.geocaching.enumerations;

import cgeo.geocaching.cgeoapplication;

import android.content.res.Resources;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public enum CacheAttribute {
    UNKNOWN(0, ""),
    DOGS(1, "dogs"),
    FEE(2, "fee"),
    RAPPELLING(3, "rappelling"),
    BOAT(4, "boat"),
    SCUBA(5, "scuba"),
    KIDS(6, "kids"),
    ONEHOUR(7, "onehour"),
    SCENIC(8, "scenic"),
    HIKING(9, "hiking"),
    CLIMBING(10, "climbing"),
    WADING(11, "wading"),
    SWIMMING(12, "swimming"),
    AVAILABLE(13, "available"),
    NIGHT(14, "night"),
    WINTER(15, "winter"),
    POISONOAK(17, "poisonoak"),
    DANGEROUSANIMALS(18, "dangerousanimals"),
    TICKS(19, "ticks"),
    MINE(29, "mine"),
    CLIFF(21, "cliff"),
    HUNTING(22, "hunting"),
    DANGER(23, "danger"),
    WHEELCHAIR(24, "wheelchair"),
    PARKING(25, "parking"),
    PUBLIC(26, "public"),
    WATER(27, "water"),
    RESTROOMS(28, "restrooms"),
    PHONE(29, "phone"),
    PICNIC(30, "picnic"),
    CAMPING(31, "camping"),
    BICYCLES(32, "bicycles"),
    MOTORCYCLES(33, "motorcycles"),
    QUADS(34, "quads"),
    JEEPS(35, "jeeps"),
    SNOWMOBILES(36, "snowmobiles"),
    HORSES(37, "horses"),
    CAMPFIRES(38, "campfires"),
    THORN(39, "thorn"),
    STEALTH(40, "stealth"),
    STROLLER(41, "stroller"),
    FIRSTAID(42, "firstaid"),
    COW(43, "cow"),
    FLASHLIGHT(44, "flashlight"),
    LANDF(45, "landf"),
    RV(46, "rv"),
    FIELD_PUZZLE(47, "field_puzzle"),
    UV(48, "uv"),
    SNOWSHOES(49, "snowshoes"),
    SKIIS(50, "skiis"),
    SPECIAL_TOOLS(51, "s_tool"),
    NIGHTCACHE(52, "nightcache"),
    PARKNGRAB(53, "parkngrab"),
    ABANDONED_BUILDING(54, "abandonedbuilding"),
    HIKE_SHORT(55, "hike_short"),
    HIKE_MED(56, "hike_med"),
    HIKE_LONG(57, "hike_long"),
    FUEL(58, "fuel"),
    FOOD(59, "food"),
    WIRELESS_BEACON(60, "wirelessbeacon"),
    PARTNERSHIP(61, "partnership"),
    SEASONAL(62, "seasonal"),
    TOURIST_OK(63, "touristok"),
    TREECLIMBING(64, "treeclimbing"),
    FRONTYARD(65, "frontyard"),
    TEAMWORK(66, "teamwork");

    public static final String INTERNAL_PRE = "attribute_";
    public static final String INTERNAL_YES = "_yes";
    public static final String INTERNAL_NO = "_no";

    public final int id;
    public final String gcRawName;

    private CacheAttribute(final int id, final String gcRawName) {
        this.id = id;
        this.gcRawName = gcRawName;
    }

    public String getL10n(final boolean enabled) {
        final String attributeDescriptor = INTERNAL_PRE + gcRawName + (enabled ? INTERNAL_YES : INTERNAL_NO);

        cgeoapplication instance = cgeoapplication.getInstance();
        if (instance != null) {
            Resources res = instance.getResources();
            int id = res.getIdentifier(attributeDescriptor, "string", instance.getBaseContext().getPackageName());

            return (id > 0) ? res.getString(id) : attributeDescriptor;
        } else {
            return attributeDescriptor;
        }
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
