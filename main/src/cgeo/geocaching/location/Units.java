package cgeo.geocaching.location;

import cgeo.geocaching.settings.Settings;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Locale;

public class Units {

    public static ImmutablePair<Double, String> scaleDistance(final double distanceKilometers) {
        double distance;
        final String units;
        if (Settings.useImperialUnits()) {
            distance = distanceKilometers / IConversion.MILES_TO_KILOMETER;
            if (distance >= 0.1) {
                units = "mi";
            } else {
                distance *= 5280;
                units = "ft";
            }
        } else {
            if (distanceKilometers >= 1) {
                distance = distanceKilometers;
                units = "km";
            } else {
                distance = distanceKilometers * 1000;
                units = "m";
            }
        }
        return new ImmutablePair<>(distance, units);
    }

    public static String getDistanceFromKilometers(final Float distanceKilometers) {
        if (distanceKilometers == null) {
            return "?";
        }

        final ImmutablePair<Double, String> scaled = scaleDistance(distanceKilometers);
        final String formatString;
        if (scaled.left >= 100) {
            formatString = "%.0f %s";
        } else if (scaled.left >= 10) {
            formatString = "%.1f %s";
        } else {
            formatString = "%.2f %s";
        }

        return String.format(formatString, scaled.left, scaled.right);
    }

    public static String getDistanceFromMeters(final float meters) {
        return getDistanceFromKilometers(meters / 1000f);
    }

    public static String getSpeed(final float kilometersPerHour) {
        if (Settings.useImperialUnits()) {
            return String.format(Locale.US, "%.0f mph", kilometersPerHour / IConversion.MILES_TO_KILOMETER);
        }
        return String.format(Locale.US, "%.0f km/h", kilometersPerHour);
    }
}
