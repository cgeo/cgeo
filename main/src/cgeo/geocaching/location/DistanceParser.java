package cgeo.geocaching.location;

import cgeo.geocaching.utils.MatcherWrapper;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public final class DistanceParser {

    private static final Pattern pattern = Pattern.compile("^([0-9.,]+)[ ]*(m|km|ft|yd|mi|)?$", Pattern.CASE_INSENSITIVE);

    /**
     * Parse a distance string composed by a number and an optional suffix
     * (such as "1.2km").
     *
     * @param distanceText
     *            the string to analyze
     * @return the distance in kilometers
     *
     * @throws NumberFormatException
     *             if the given number is invalid
     */
    public static float parseDistance(String distanceText, final boolean metricUnit)
            throws NumberFormatException {
        final MatcherWrapper matcher = new MatcherWrapper(pattern, distanceText);

        if (!matcher.find()) {
            throw new NumberFormatException(distanceText);
        }

        final float value = Float.parseFloat(matcher.group(1).replace(',', '.'));
        final String unit = matcher.group(2).toLowerCase(Locale.US);

        if (unit.equals("m") || (StringUtils.isEmpty(unit) && metricUnit)) {
            return value / 1000;
        }
        if (unit.equals("km")) {
            return value;
        }
        if (unit.equals("yd")) {
            return value * IConversion.YARDS_TO_KILOMETER;
        }
        if (unit.equals("mi")) {
            return value * IConversion.MILES_TO_KILOMETER;
        }
        return value * IConversion.FEET_TO_KILOMETER;
    }

}
