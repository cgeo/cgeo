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

    // insert other official cache types before USER_DEFINED and UNKNOWN
    USER_DEFINED("userdefined", "User defined cache", "", R.string.userdefined, R.drawable.type_virtual, "", R.drawable.dot_virtual),
    UNKNOWN("unknown", "unknown", "", R.string.unknown, R.drawable.type_unknown, "", R.drawable.dot_unknown),
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
        return this == VIRTUAL || this == WEBCAM || this == EARTH;
    }
}
