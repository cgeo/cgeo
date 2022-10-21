package cgeo.geocaching.filters.core;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

final class UserDisplayableStringUtils {

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
            sb.append(">").append(minValue);
        } else {
            // maxValueSet
            sb.append("<").append(maxValue);
        }
        return sb.toString();
    }
}
