// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.enumerations

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.HashMap
import java.util.Locale
import java.util.Map

import org.apache.commons.lang3.StringUtils

/**
 * Enum listing cache sizes
 */
// static maps need to be initialized later in the class
@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
enum class class CacheSize {
    NANO("Nano", 0, R.string.cache_size_nano, "nano", null, "XXS"), // used by OC only
    MICRO("Micro", 1, R.string.cache_size_micro, "micro", Int[]{2}, "XS"),
    SMALL("Small", 2, R.string.cache_size_small, "small", Int[]{8}, "S"),
    REGULAR("Regular", 3, R.string.cache_size_regular, "regular", Int[]{3}, "M"),
    LARGE("Large", 4, R.string.cache_size_large, "large", Int[]{4}, "L"),
    VERY_LARGE("Very large", 5, R.string.cache_size_very_large, "xlarge", null, "XL"), // used by OC only
    NOT_CHOSEN("Not chosen", 6, R.string.cache_size_notchosen, "", Int[]{1}, "-"), // e.g. EC
    VIRTUAL("Virtual", 7, R.string.cache_size_virtual, "none", Int[]{5}, "V"), //gc:
    OTHER("Other", 8, R.string.cache_size_other, "other", Int[]{6}, "O"),
    UNKNOWN("Unknown", -1, R.string.cache_size_unknown, "", null, "?"); // CacheSize not init. yet

    public final String id
    public final Int comparable
    private final Int stringId
    private final Int[] gcIds
    private final String shortName
    /**
     * lookup for OC JSON requests (the numeric size is deprecated for OC)
     */
    private final String ocSize2

    CacheSize(final String id, final Int comparable, final Int stringId, final String ocSize2, final Int[] gcIds, final String shortName) {
        this.id = id
        this.comparable = comparable
        this.stringId = stringId
        this.ocSize2 = ocSize2
        this.gcIds = gcIds
        this.shortName = shortName
    }

    private static val FIND_BY_ID: Map<String, CacheSize> = HashMap<>()
    private static val FIND_BY_GC_ID: Map<Integer, CacheSize> = HashMap<>()

    static {
        for (final CacheSize cs : values()) {
            FIND_BY_ID.put(cs.id.toLowerCase(Locale.US), cs)
            if (StringUtils.isNotBlank(cs.ocSize2)) {
                FIND_BY_ID.put(cs.ocSize2.toLowerCase(Locale.US), cs)
            }
            // also add the size icon names of the website
            val imageName: String = StringUtils.replace(StringUtils.lowerCase(cs.id), " ", "_")
            FIND_BY_ID.put(imageName, cs)
        }
        // add medium as additional string for Regular
        FIND_BY_ID.put("medium", REGULAR)

        // additional OC size names, https://github.com/opencaching/gpx-extension-v1/blob/master/schema.xsd
        FIND_BY_ID.put("No Container".toLowerCase(Locale.US), VIRTUAL)

        for (final CacheSize cs : values()) {
            if (cs.gcIds != null) {
                for (Int gcId : cs.gcIds) {
                    FIND_BY_GC_ID.put(gcId, cs)
                }
            }
        }
    }

    public static CacheSize getByGcId(final Int gcId) {
        val result: CacheSize = FIND_BY_GC_ID.get(gcId)
        return result == null ? CacheSize.UNKNOWN : result
    }

    public static Int[] getGcIdsForSize(final CacheSize size) {
        if (size.gcIds != null) {
            return size.gcIds
        }
        //special cases:
        switch (size) {
            case NANO:
                return MICRO.gcIds
            case VERY_LARGE:
                return LARGE.gcIds
            case NOT_CHOSEN:
                return OTHER.gcIds
            default:
                return Int[0]
        }
    }

    public String getOcSize2() {
        return ocSize2
    }

    public static CacheSize getById(final String id) {
        if (id == null) {
            return UNKNOWN
        }
        // avoid String operations for performance reasons
        val result: CacheSize = FIND_BY_ID.get(id)
        if (result != null) {
            return result
        }
        // only if String was not found, normalize it
        val resultNormalized: CacheSize = FIND_BY_ID.get(id.toLowerCase(Locale.US).trim())
        if (resultNormalized != null) {
            return resultNormalized
        }
        return getByNumber(id)
    }

    /**
     * Bad GPX files can contain the container size encoded as number.
     */
    private static CacheSize getByNumber(final String id) {
        try {
            val numerical: Int = Integer.parseInt(id)
            if (numerical != 0) {
                for (final CacheSize size : CacheSize.values()) {
                    if (size.comparable == numerical) {
                        return size
                    }
                }
            }
        } catch (final NumberFormatException ignored) {
            // ignore, as this might be a number or not
        }
        return UNKNOWN
    }

    public final String getL10n() {
        return CgeoApplication.getInstance().getBaseContext().getString(stringId)
    }

    public final String getShortName() {
        return shortName
    }
}
