package cgeo.geocaching.location;

import cgeo.geocaching.settings.Settings;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Locale;

public class Units {

    private Units() {
        // utility class
    }

    public static ImmutablePair<Double, String> scaleDistance(final double distanceKilometers) {
        if (Settings.useImperialUnits()) {
            final double distanceMiles = distanceKilometers / IConversion.MILES_TO_KILOMETER;
            if (distanceMiles >= 0.1) {
                return new ImmutablePair<>(distanceMiles, "mi");
            }
            return new ImmutablePair<>(distanceMiles * 5280, "ft");
        } else if (distanceKilometers >= 1) {
            return new ImmutablePair<>(distanceKilometers, "km");
        } else {
            return new ImmutablePair<>(distanceKilometers * 1000, "m");
        }
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
