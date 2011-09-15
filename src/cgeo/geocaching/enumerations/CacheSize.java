package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum listing cache sizes
 *
 * @author koem
 */
public enum CacheSize {
    MICRO("micro", 1, R.string.caches_filter_size_micro),
    SMALL("small", 2, R.string.caches_filter_size_small),
    REGULAR("regular", 3, R.string.caches_filter_size_regular),
    LARGE("large", 4, R.string.caches_filter_size_large),
    VIRTUAL("virtual", 0, R.string.caches_filter_size_virtual),
    NOT_CHOSEN("not chosen", 0, R.string.caches_filter_size_notchosen),
    OTHER("other", 0, R.string.caches_filter_size_other);

    public final String id;
    public final int comparable;
    public final int stringId;

    private CacheSize(String id, int comparable, int stringId) {
        this.id = id;
        this.comparable = comparable;
        this.stringId = stringId;
    }

    final public static Map<String, CacheSize> FIND_BY_ID;
    static {
        final HashMap<String, CacheSize> mapping = new HashMap<String, CacheSize>();
        for (CacheSize cs : values()) {
            mapping.put(cs.id, cs);
        }
        FIND_BY_ID = Collections.unmodifiableMap(mapping);
    }

}
