package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Enum listing cache sizes
 */
public enum CacheSize {
    NANO("Nano", 0, R.string.cache_size_nano, "nano"), // used by OC only
    MICRO("Micro", 1, R.string.cache_size_micro, "micro"),
    SMALL("Small", 2, R.string.cache_size_small, "small"),
    REGULAR("Regular", 3, R.string.cache_size_regular, "regular"),
    LARGE("Large", 4, R.string.cache_size_large, "large"),
    VERY_LARGE("Very large", 5, R.string.cache_size_very_large, "xlarge"), // used by OC only
    NOT_CHOSEN("Not chosen", 6, R.string.cache_size_notchosen, ""),
    VIRTUAL("Virtual", 7, R.string.cache_size_virtual, "none"),
    OTHER("Other", 8, R.string.cache_size_other, "other"),
    UNKNOWN("Unknown", -1, R.string.cache_size_unknown, ""); // CacheSize not init. yet

    public final String id;
    public final int comparable;
    private final int stringId;
    /**
     * lookup for OC JSON requests (the numeric size is deprecated for OC)
     */
    private final String ocSize2;

    CacheSize(final String id, final int comparable, final int stringId, final String ocSize2) {
        this.id = id;
        this.comparable = comparable;
        this.stringId = stringId;
        this.ocSize2 = ocSize2;
    }

    final private static Map<String, CacheSize> FIND_BY_ID;
    static {
        final HashMap<String, CacheSize> mapping = new HashMap<String, CacheSize>();
        for (final CacheSize cs : values()) {
            mapping.put(cs.id.toLowerCase(Locale.US), cs);
            mapping.put(cs.ocSize2.toLowerCase(Locale.US), cs);
        }
        // add medium as additional string for Regular
        mapping.put("medium", CacheSize.REGULAR);
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
        final CacheSize resultNormalized = CacheSize.FIND_BY_ID.get(id.toLowerCase(Locale.US).trim());
        if (resultNormalized != null) {
            return resultNormalized;
        }
        return getByNumber(id);
    }

    /**
     * Bad GPX files can contain the container size encoded as number.
     *
     * @param id
     * @return
     */
    private static CacheSize getByNumber(final String id) {
        try {
            final int numerical = Integer.parseInt(id);
            if (numerical != 0) {
                for (final CacheSize size : CacheSize.values()) {
                    if (size.comparable == numerical) {
                        return size;
                    }
                }
            }
        } catch (final NumberFormatException e) {
            // ignore, as this might be a number or not
        }
        return UNKNOWN;
    }

    public final String getL10n() {
        return CgeoApplication.getInstance().getBaseContext().getResources().getString(stringId);
    }
}
