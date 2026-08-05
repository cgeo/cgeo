package cgeo.geocaching.filters.core;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public final class UserDisplayableStringUtils {

    public static final char[] LESS_THAN_OR_EQUAL_TO = Character.toChars(0x2264);
    public static final char[] GREATER_THAN_OR_EQUAL_TO = Character.toChars(0x2265);

    private UserDisplayableStringUtils() {
        // Do not instantiate
    }

    static String getUserDisplayableConfig(final @Nullable String minValue, final @Nullable String maxValue) {
        final StringBuilder sb = new StringBuilder();
        if (StringUtils.isEmpty(minValue) && StringUtils.isEmpty(maxValue)) {
            sb.append("*");
        } else if (StringUtils.isNotEmpty(minValue) && StringUtils.isNotEmpty(maxValue)) {
            if (minValue.equals(maxValue)) {
                sb.append(maxValue);
            } else {
                sb.append(minValue).append("-").append(maxValue);
            }
        } else if (StringUtils.isNotEmpty(minValue)) {
            sb.append(GREATER_THAN_OR_EQUAL_TO).append(minValue);
        } else {
            // maxValueSet
            sb.append(LESS_THAN_OR_EQUAL_TO).append(maxValue);
        }
        return sb.toString();
    }
}
