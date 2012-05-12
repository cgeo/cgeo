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
    MICRO("Micro", 1, R.string.cache_size_micro),
    SMALL("Small", 2, R.string.cache_size_small),
    REGULAR("Regular", 3, R.string.cache_size_regular),
    LARGE("Large", 4, R.string.cache_size_large),
    VIRTUAL("Virtual", 0, R.string.cache_size_virtual),
    NOT_CHOSEN("Not chosen", 0, R.string.cache_size_notchosen),
    OTHER("Other", 0, R.string.cache_size_other),
    UNKNOWN("Unknown", 0, R.string.cache_size_unknown); // CacheSize not init. yet

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
            mapping.put(cs.id.toLowerCase(), cs);
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

