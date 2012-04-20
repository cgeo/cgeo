package cgeo.geocaching.geopoint;

import cgeo.geocaching.Settings;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class HumanDistance {

    public static ImmutablePair<Double, String> scaleUnit(final double distanceKilometers) {
        double distance;
        String units;
        if (Settings.isUseMetricUnits()) {
            if (distanceKilometers >= 1) {
                distance = distanceKilometers;
                units = "km";
            } else {
                distance = distanceKilometers * 1000;
                units = "m";
            }
        } else {
            distance = distanceKilometers / IConversion.MILES_TO_KILOMETER;
            if (distance >= 0.1) {
                units = "mi";
            } else {
                distance *= 5280;
                units = "ft";
            }
        }
        return new ImmutablePair<Double, String>(distance, units);
    }

    public static String getHumanDistance(final Float distanceKilometers) {
        if (distanceKilometers == null) {
            return "?";
        }

        final ImmutablePair<Double, String> scaled = scaleUnit(distanceKilometers);
        String formatString;
        if (scaled.left >= 100) {
            formatString = "%.0f";
        } else if (scaled.left >= 10) {
            formatString = "%.1f";
        } else {
            formatString = "%.2f";
        }

        return String.format(formatString + " %s", scaled.left, scaled.right);
    }
}
