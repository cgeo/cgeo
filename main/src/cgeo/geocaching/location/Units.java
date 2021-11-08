package cgeo.geocaching.location;

import cgeo.geocaching.settings.Settings;

import java.util.Locale;

import org.apache.commons.lang3.tuple.ImmutablePair;

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

    public static float generateSmartRoundedAverageDistance(final float newDistance, final float lastDistance) {
        final float scaleFactor;
        if (Settings.useImperialUnits()) { // the rounded values should be user displayable. Therefore, use a different scaling factor for imperial units.
            scaleFactor = 10 / IConversion.MILES_TO_KILOMETER; // use 0.1 mi scale
        } else {
            scaleFactor = 1000; // use 1m scale
        }
        final float originalDelta = scaleFactor * (newDistance - lastDistance);
        float delta = originalDelta;
        while (delta >= 10) {
            delta /= 10;
        }
        final float roundingFactor = originalDelta / delta; // depending on the delta, generate the best suitable rounding factor

        final float average = scaleFactor * (newDistance + lastDistance) / 2;
        final float roundedValue = (Math.round(average / roundingFactor)) * roundingFactor;

        return roundedValue / scaleFactor;
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
