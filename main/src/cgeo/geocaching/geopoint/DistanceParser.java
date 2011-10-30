package cgeo.geocaching.geopoint;

import cgeo.geocaching.cgBase;

import java.util.regex.Matcher;
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
    public static float parseDistance(String distanceText, final boolean metricUnit) {
        final Matcher matcher = pattern.matcher(distanceText);

        if (!matcher.find()) {
            throw new NumberFormatException(distanceText);
        }

        final float value = Float.parseFloat(matcher.group(1).replace(',', '.'));
        final String unit = matcher.group(2).toLowerCase();

        if (unit.equals("m") || (unit.length() == 0 && metricUnit)) {
            return value / 1000;
        }
        if (unit.equals("km")) {
            return value;
        }
        if (unit.equals("yd")) {
            return value * cgBase.yards2km;
        }
        if (unit.equals("mi")) {
            return value * cgBase.miles2km;
        }
        return value * cgBase.feet2km;
    }

}
