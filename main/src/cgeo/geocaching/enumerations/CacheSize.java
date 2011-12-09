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
    UNKNOWN("unknown", 0, R.string.err_unknown); // CacheSize not init. yet

    public final String id;
    public final int comparable;
    private final int stringId;
    private String l10n; // not final because the locale can be changed

    private CacheSize(String id, int comparable, int stringId) {
        this.id = id;
        this.comparable = comparable;
        this.stringId = stringId;
        setL10n();
    }

    final private static Map<String, CacheSize> FIND_BY_ID;
    static {
        final HashMap<String, CacheSize> mapping = new HashMap<String, CacheSize>();
        for (CacheSize cs : values()) {
            mapping.put(cs.id, cs);
        }
        FIND_BY_ID = Collections.unmodifiableMap(mapping);
    }

    public final static CacheSize getById(final String id) {
        final CacheSize result = id != null ? CacheSize.FIND_BY_ID.get(id.toLowerCase().trim()) : null;
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

