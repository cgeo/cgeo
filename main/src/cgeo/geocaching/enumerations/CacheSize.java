package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum listing cache sizes
 *
 * @author koem
 */
public enum CacheSize {
    MICRO("micro", 1, R.string.cache_size_micro),
    SMALL("small", 2, R.string.cache_size_small),
    REGULAR("regular", 3, R.string.cache_size_regular),
    LARGE("large", 4, R.string.cache_size_large),
    VIRTUAL("virtual", 0, R.string.cache_size_virtual),
    NOT_CHOSEN("not chosen", 0, R.string.cache_size_notchosen),
    OTHER("other", 0, R.string.cache_size_other),
    UNKNOWN("unknown", 0, R.string.cache_size_unknown); // CacheSize not init. yet

    public final String id;
    public final int comparable;
    private final int stringId;

    private CacheSize(String id, int comparable, int stringId) {
        this.id = id;
        this.comparable = comparable;
        this.stringId = stringId;
    }

    final private static Map<String, CacheSize> FIND_BY_ID;
    static {
        final HashMap<String, CacheSize> mapping = new HashMap<String, CacheSize>();
        for (CacheSize cs : values()) {
            mapping.put(cs.id, cs);
        }
        FIND_BY_ID = Collections.unmodifiableMap(mapping);
    }

    public static CacheSize getById(final String id) {
        if (id == null) {
            return UNKNOWN;
        }
        // avoid String operations for performance reasons
        final CacheSize result = CacheSize.FIND_BY_ID.get(id);
        if (result != null) {
            return result;
        }
        // only if String was not found, normalize it
        final CacheSize resultNormalized = CacheSize.FIND_BY_ID.get(id.toLowerCase().trim());
        if (resultNormalized != null) {
            return resultNormalized;
        }
        return UNKNOWN;
    }

    public final String getL10n() {
        return cgeoapplication.getInstance().getBaseContext().getResources().getString(stringId);
    }
}

