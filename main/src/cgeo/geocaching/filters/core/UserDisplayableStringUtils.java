package cgeo.geocaching.filters.core;

final class UserDisplayableStringUtils {

    private UserDisplayableStringUtils() {
        // Do not instantiate
    }

    static String getUserDisplayableConfig(final boolean minValueSet, final boolean maxValueSet, final String minValue, final String maxValue) {
        final StringBuilder sb = new StringBuilder();
        if (!minValueSet && !maxValueSet) {
            sb.append("*");
        } else if (minValueSet && maxValueSet) {
            if (minValue.equals(maxValue)) {
                sb.append(maxValue);
            } else {
                sb.append(minValue).append("-").append(maxValue);
            }
        } else if (minValueSet) {
            sb.append(">").append(minValue);
        } else {
            // maxValueSet
            sb.append("<").append(maxValue);
        }
        return sb.toString();
    }
}
