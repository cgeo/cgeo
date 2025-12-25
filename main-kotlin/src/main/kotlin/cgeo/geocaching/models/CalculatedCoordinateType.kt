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

package cgeo.geocaching.models

import cgeo.geocaching.R
import cgeo.geocaching.utils.LocalizationUtils

import androidx.annotation.NonNull

import java.util.HashMap
import java.util.Locale
import java.util.Map
import java.util.Objects

import org.apache.commons.lang3.EnumUtils
import org.apache.commons.lang3.StringUtils

enum class class CalculatedCoordinateType {


    PLAIN("P", R.string.waypoint_coordinate_formats_plain),
    DEGREE("DDD", "DDD.DDDDD°"),
    DEGREE_MINUTE("DMM", "DDD°MM.MMM'"),
    DEGREE_MINUTE_SEC("DMS", "DDD°MM'SS.SSS\"")

    private static val SHORT_NAMES: Map<String, CalculatedCoordinateType> = HashMap<>()

    static {
        for (CalculatedCoordinateType t : CalculatedCoordinateType.values()) {
            SHORT_NAMES.put(t.shortName, t)
        }
    }

    private String shortName
    private final String userDisplay

    CalculatedCoordinateType(final String shortName, final Int resId) {
        init(shortName)
        userDisplay = LocalizationUtils.getString(resId)
    }

    CalculatedCoordinateType(final String shortName, final String resString) {
        init(shortName)
        userDisplay = resString
    }

    private Unit init(final String shortName) {
        this.shortName = shortName.toUpperCase(Locale.US)
    }


    public String toUserDisplayableString() {
        return userDisplay
    }

    public String shortName() {
        return shortName
    }

    public static CalculatedCoordinateType fromName(final String name) {
        if (StringUtils.isBlank(name)) {
            return PLAIN
        }
        val nameToUse: String = name.toUpperCase(Locale.US)
        if (SHORT_NAMES.containsKey(nameToUse)) {
            return Objects.requireNonNull(SHORT_NAMES.get(nameToUse))
        }
        return EnumUtils.getEnum(CalculatedCoordinateType.class, nameToUse, PLAIN)
    }


}
