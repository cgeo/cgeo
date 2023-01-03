package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Enum listing cache sizes
 */
// static maps need to be initialized later in the class
@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
public enum CacheSize {
    NANO("Nano", 0, R.string.cache_size_nano, "nano", null, "XXS"), // used by OC only
    MICRO("Micro", 1, R.string.cache_size_micro, "micro", new int[]{2}, "XS"),
    SMALL("Small", 2, R.string.cache_size_small, "small", new int[]{8}, "S"),
    REGULAR("Regular", 3, R.string.cache_size_regular, "regular", new int[]{3}, "M"),
    LARGE("Large", 4, R.string.cache_size_large, "large", new int[]{4}, "L"),
    VERY_LARGE("Very large", 5, R.string.cache_size_very_large, "xlarge", null, "XL"), // used by OC only
    NOT_CHOSEN("Not chosen", 6, R.string.cache_size_notchosen, "", new int[]{1}, "-"), // e.g. EC
    VIRTUAL("Virtual", 7, R.string.cache_size_virtual, "none", new int[]{5}, "V"), //gc:
    OTHER("Other", 8, R.string.cache_size_other, "other", new int[]{6}, "O"),
    UNKNOWN("Unknown", -1, R.string.cache_size_unknown, "", null, "?"); // CacheSize not init. yet

    @NonNull
    public final String id;
    public final int comparable;
    private final int stringId;
    private final int[] gcIds;
    private final String shortName;
    /**
     * lookup for OC JSON requests (the numeric size is deprecated for OC)
     */
    private final String ocSize2;

    CacheSize(@NonNull final String id, final int comparable, final int stringId, final String ocSize2, final int[] gcIds, final String shortName) {
        this.id = id;
        this.comparable = comparable;
        this.stringId = stringId;
        this.ocSize2 = ocSize2;
        this.gcIds = gcIds;
        this.shortName = shortName;
    }

    @NonNull
    private static final Map<String, CacheSize> FIND_BY_ID = new HashMap<>();
    private static final Map<Integer, CacheSize> FIND_BY_GC_ID = new HashMap<>();

    static {
        for (final CacheSize cs : values()) {
            FIND_BY_ID.put(cs.id.toLowerCase(Locale.US), cs);
            if (StringUtils.isNotBlank(cs.ocSize2)) {
                FIND_BY_ID.put(cs.ocSize2.toLowerCase(Locale.US), cs);
            }
            // also add the size icon names of the website
            final String imageName = StringUtils.replace(StringUtils.lowerCase(cs.id), " ", "_");
            FIND_BY_ID.put(imageName, cs);
        }
        // add medium as additional string for Regular
        FIND_BY_ID.put("medium", REGULAR);

        // additional OC size names, https://github.com/opencaching/gpx-extension-v1/blob/master/schema.xsd
        FIND_BY_ID.put("No Container".toLowerCase(Locale.US), VIRTUAL);

        for (final CacheSize cs : values()) {
            if (cs.gcIds != null) {
                for (int gcId : cs.gcIds) {
                    FIND_BY_GC_ID.put(gcId, cs);
                }
            }
        }
    }

    @NonNull
    public static CacheSize getByGcId(final int gcId) {
        final CacheSize result = FIND_BY_GC_ID.get(gcId);
        return result == null ? CacheSize.UNKNOWN : result;
    }

    public static int[] getGcIdsForSize(final CacheSize size) {
        if (size.gcIds != null) {
            return size.gcIds;
        }
        //special cases:
        switch (size) {
            case NANO:
                return MICRO.gcIds;
            case VERY_LARGE:
                return LARGE.gcIds;
            case NOT_CHOSEN:
                return OTHER.gcIds;
            default:
                return new int[0];
        }
    }

    public String getOcSize2() {
        return ocSize2;
    }

    @NonNull
    public static CacheSize getById(@Nullable final String id) {
        if (id == null) {
            return UNKNOWN;
        }
        // avoid String operations for performance reasons
        final CacheSize result = FIND_BY_ID.get(id);
        if (result != null) {
            return result;
        }
        // only if String was not found, normalize it
        final CacheSize resultNormalized = FIND_BY_ID.get(id.toLowerCase(Locale.US).trim());
        if (resultNormalized != null) {
            return resultNormalized;
        }
        return getByNumber(id);
    }

    /**
     * Bad GPX files can contain the container size encoded as number.
     */
    @NonNull
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
        } catch (final NumberFormatException ignored) {
            // ignore, as this might be a number or not
        }
        return UNKNOWN;
    }

    @NonNull
    public final String getL10n() {
        return CgeoApplication.getInstance().getBaseContext().getString(stringId);
    }

    @NonNull
    public final String getShortName() {
        return shortName;
    }
}
