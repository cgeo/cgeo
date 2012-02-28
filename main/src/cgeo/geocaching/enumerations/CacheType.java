package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum listing all cache types
 *
 * @author koem
 */
public enum CacheType {

    TRADITIONAL("traditional", "traditional cache", "32bc9333-5e52-4957-b0f6-5a2c8fc7b257", R.string.traditional, R.drawable.type_traditional),
    MULTI("multi", "multi-cache", "a5f6d0ad-d2f2-4011-8c14-940a9ebf3c74", R.string.multi, R.drawable.type_multi),
    MYSTERY("mystery", "unknown cache", "40861821-1835-4e11-b666-8d41064d03fe", R.string.mystery, R.drawable.type_mystery),
    LETTERBOX("letterbox", "letterbox hybrid", "4bdd8fb2-d7bc-453f-a9c5-968563b15d24", R.string.letterbox, R.drawable.type_letterbox),
    EVENT("event", "event cache", "69eb8534-b718-4b35-ae3c-a856a55b0874", R.string.event, R.drawable.type_event),
    MEGA_EVENT("mega", "mega-event cache", "69eb8535-b718-4b35-ae3c-a856a55b0874", R.string.mega, R.drawable.type_mega),
    EARTH("earth", "earthcache", "c66f5cf3-9523-4549-b8dd-759cd2f18db8", R.string.earth, R.drawable.type_earth),
    CITO("cito", "cache in trash out event", "57150806-bc1a-42d6-9cf0-538d171a2d22", R.string.cito, R.drawable.type_cito),
    WEBCAM("webcam", "webcam cache", "31d2ae3c-c358-4b5f-8dcd-2185bf472d3d", R.string.webcam, R.drawable.type_webcam),
    VIRTUAL("virtual", "virtual cache", "294d4360-ac86-4c83-84dd-8113ef678d7e", R.string.virtual, R.drawable.type_virtual),
    WHERIGO("wherigo", "wherigo cache", "0544fa55-772d-4e5c-96a9-36a51ebcf5c9", R.string.wherigo, R.drawable.type_wherigo),
    LOSTANDFOUND("lostfound", "lost & found", "3ea6533d-bb52-42fe-b2d2-79a3424d4728", R.string.lostfound, R.drawable.type_event), // icon missing
    PROJECT_APE("ape", "project ape cache", "2555690d-b2bc-4b55-b5ac-0cb704c0b768", R.string.ape, R.drawable.type_ape),
    GCHQ("gchq", "groundspeak hq", "416f2494-dc17-4b6a-9bab-1a29dd292d8c", R.string.gchq, R.drawable.type_hq),
    GPS_EXHIBIT("gps", "gps cache exhibit", "72e69af2-7986-4990-afd9-bc16cbbb4ce3", R.string.gps, R.drawable.type_traditional), // icon missing
    UNKNOWN("unknown", "unknown", "", R.string.unknown, R.drawable.type_unknown),
    /** No real cache type -> filter */
    ALL("all", "display all caches", "9a79e6ce-3344-409c-bbe9-496530baf758", R.string.all_types, R.drawable.type_mystery);

    public final String id;
    public final String pattern;
    public final String guid;
    private final int stringId;
    private String l10n; // not final because the locale can be changed
    public final int markerId;

    private CacheType(String id, String pattern, String guid, int stringId, int markerId) {
        this.id = id;
        this.pattern = pattern;
        this.guid = guid;
        this.stringId = stringId;
        setL10n();
        this.markerId = markerId;
    }

    private final static Map<String, CacheType> FIND_BY_ID;
    private final static Map<String, CacheType> FIND_BY_PATTERN;
    static {
        final HashMap<String, CacheType> mappingId = new HashMap<String, CacheType>();
        final HashMap<String, CacheType> mappingPattern = new HashMap<String, CacheType>();
        for (CacheType ct : values()) {
            mappingId.put(ct.id, ct);
            mappingPattern.put(ct.pattern, ct);
        }
        FIND_BY_ID = Collections.unmodifiableMap(mappingId);
        FIND_BY_PATTERN = Collections.unmodifiableMap(mappingPattern);
    }

    public final static CacheType getById(final String id) {
        final CacheType result = id != null ? CacheType.FIND_BY_ID.get(id.toLowerCase().trim()) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    public final static CacheType getByPattern(final String pattern) {
        final CacheType result = pattern != null ? CacheType.FIND_BY_PATTERN.get(pattern.toLowerCase().trim()) : null;
        if (result == null) {
            return UNKNOWN;
        }
        return result;
    }

    public final String getL10n() {
        return l10n;
    }

    public void setL10n() {
        this.l10n = cgeoapplication.getInstance().getBaseContext().getResources().getString(this.stringId);
    }

}