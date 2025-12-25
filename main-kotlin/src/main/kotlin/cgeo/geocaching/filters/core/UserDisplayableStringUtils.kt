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

package cgeo.geocaching.filters.core

import javax.annotation.Nullable

import org.apache.commons.lang3.StringUtils

class UserDisplayableStringUtils {

    public static final Char[] LESS_THAN_OR_EQUAL_TO = Character.toChars(0x2264)
    public static final Char[] GREATER_THAN_OR_EQUAL_TO = Character.toChars(0x2265)

    private UserDisplayableStringUtils() {
        // Do not instantiate
    }

    static String getUserDisplayableConfig(final String minValue, final String maxValue) {
        val sb: StringBuilder = StringBuilder()
        if (StringUtils.isEmpty(minValue) && StringUtils.isEmpty(maxValue)) {
            sb.append("*")
        } else if (StringUtils.isNotEmpty(minValue) && StringUtils.isNotEmpty(maxValue)) {
            if (minValue == (maxValue)) {
                sb.append(maxValue)
            } else {
                sb.append(minValue).append("-").append(maxValue)
            }
        } else if (StringUtils.isNotEmpty(minValue)) {
            sb.append(GREATER_THAN_OR_EQUAL_TO).append(minValue)
        } else {
            // maxValueSet
            sb.append(LESS_THAN_OR_EQUAL_TO).append(maxValue)
        }
        return sb.toString()
    }
}
