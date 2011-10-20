package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum listing all cache types
 *
 * @author koem
 */
public enum CacheType {
    TRADITIONAL("traditional", "traditional cache", "32bc9333-5e52-4957-b0f6-5a2c8fc7b257", R.string.traditional),
    MULTI("multi", "multi-cache", "a5f6d0ad-d2f2-4011-8c14-940a9ebf3c74", R.string.multi),
    MYSTERY("mystery", "unknown cache", "40861821-1835-4e11-b666-8d41064d03fe", R.string.mystery),
    LETTERBOX("letterbox", "letterbox hybrid", "4bdd8fb2-d7bc-453f-a9c5-968563b15d24", R.string.letterbox),
    EVENT("event", "event cache", "69eb8534-b718-4b35-ae3c-a856a55b0874", R.string.event),
    MEGA_EVENT("mega", "mega-event cache", "69eb8535-b718-4b35-ae3c-a856a55b0874", R.string.mega),
    EARTH("earth", "earthcache", "c66f5cf3-9523-4549-b8dd-759cd2f18db8", R.string.earth),
    CITO("cito", "cache in trash out event", "57150806-bc1a-42d6-9cf0-538d171a2d22", R.string.cito),
    WEBCAM("webcam", "webcam cache", "31d2ae3c-c358-4b5f-8dcd-2185bf472d3d", R.string.webcam),
    VIRTUAL("virtual", "virtual cache", "294d4360-ac86-4c83-84dd-8113ef678d7e", R.string.virtual),
    WHERIGO("wherigo", "wherigo cache", "0544fa55-772d-4e5c-96a9-36a51ebcf5c9", R.string.wherigo),
    LOSTANDFOUND("lostfound", "lost & found", "3ea6533d-bb52-42fe-b2d2-79a3424d4728", R.string.lostfound),
    PROJECT_APE("ape", "project ape cache", "2555690d-b2bc-4b55-b5ac-0cb704c0b768", R.string.ape),
    GCHQ("gchq", "groundspeak hq", "416f2494-dc17-4b6a-9bab-1a29dd292d8c", R.string.gchq),
    GPS_EXHIBIT("gps", "gps cache exhibit", "72e69af2-7986-4990-afd9-bc16cbbb4ce3", R.string.gps),
    UNKNOWN("unknown", "unknown", "", R.string.unknown);

    public final String id;
    public final String pattern;
    public final String guid;
    public final int stringId;

    private CacheType(String id, String pattern, String guid, int stringId) {
        this.id = id;
        this.pattern = pattern;
        this.guid = guid;
        this.stringId = stringId;
    }

    public final static Map<String, CacheType> FIND_BY_ID;
    static {
        final HashMap<String, CacheType> mapping = new HashMap<String, CacheType>();
        for (CacheType ct : values()) {
            mapping.put(ct.id, ct);
        }
        FIND_BY_ID = Collections.unmodifiableMap(mapping);
    }

}
