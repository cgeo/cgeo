package cgeo.geocaching.enumerations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum listing cache sizes
 * 
 * @author koem
 */
public enum CacheSize {
    MICRO("micro", 1),
    SMALL("small", 2),
    REGULAR("regular", 3),
    LARGE("large", 4),
    NOT_CHOSEN("not chosen", 0),
    OTHER("other", 0);

    public final String cgeoId;
    public final int comparable;

    private CacheSize(String cgeoId, int comparable) {
        this.cgeoId = cgeoId;
        this.comparable = comparable;
    }

    final public static Map<String, CacheSize> FIND_BY_CGEOID;
    static {
        final HashMap<String, CacheSize> mapping = new HashMap<String, CacheSize>();
        for (CacheSize cs : values()) {
            mapping.put(cs.cgeoId, cs);
        }
        FIND_BY_CGEOID = Collections.unmodifiableMap(mapping);
    }

}
