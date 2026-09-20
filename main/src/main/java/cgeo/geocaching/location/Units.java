package cgeo.geocaching.location;

import cgeo.geocaching.settings.Settings;

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;

public class Units {

    private Units() {
        // utility class
    }

    public static ImmutableTriple<Double, String, Float> scaleDistanceWithFactor(final double distanceKilometers) {
        if (Settings.useImperialUnits()) {
            final double distanceMiles = distanceKilometers / IConversion.MILES_TO_KILOMETER;
            if (Math.abs(distanceMiles) >= 0.1) {
                return new ImmutableTriple<>(distanceMiles, "mi", IConversion.MILES_TO_KILOMETER);
            }
            return new ImmutableTriple<>(distanceMiles * 5280, "ft", IConversion.MILES_TO_KILOMETER / 5280);
        } else if (Math.abs(distanceKilometers) >= 1) {
            return new ImmutableTriple<>(distanceKilometers, "km", 1f);
        } else {
            return new ImmutableTriple<>(distanceKilometers * 1000, "m", 1f / 1000);
        }
    }

    public static ImmutablePair<Double, String> scaleDistance(final double distanceKilometers) {
        final ImmutableTriple<Double, String, Float> value = scaleDistanceWithFactor(distanceKilometers);
        return new ImmutablePair<>(value.left, value.middle);
    }

    /** formats given elevation in meters or feet, no fractions, no kilometers/miles */
    public static String formatElevation(final float meters) {
        final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.getDefault());
        return Float.isNaN(meters) ? "" : Settings.useImperialUnits() ? nf.format(meters * IConversion.METERS_TO_FEET) + " ft" : nf.format(meters) + " m";
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
        if (distanceKilometers == null || Float.isNaN(distanceKilometers) || Float.isInfinite(distanceKilometers)) {
            return "?";
        }

        final ImmutablePair<Double, String> scaled = scaleDistance(distanceKilometers);
        final String formatString;
        if (Math.abs(scaled.left) >= 100) {
            formatString = "%.0f %s";
        } else if (Math.abs(scaled.left) >= 10) {
            formatString = "%.1f %s";
        } else {
            formatString = "%.2f %s";
        }

        return String.format(formatString, scaled.left, scaled.right);
    }

    public static String getDistanceFromMeters(final float meters) {
        return getDistanceFromKilometers(meters / 1000f);
    }

    public static String getDirectionFromBearing(final float bb) {
        final float bearing = (bb + 360) % 360f;
        final boolean north = bearing <= 67.5 || bearing >= 292.5;
        final boolean south = bearing >= 112.5 && bearing <= 247.5;
        final boolean east = bearing >= 22.5 && bearing <= 157.5;
        final boolean west = bearing >= 202.5 && bearing <= 337.5;

        return (north ? "N" : (south ? "S" : "")) + (east ? "E" : (west ? "W" : ""));

    }

    public static String getSpeed(final float kilometersPerHour) {
        if (Settings.useImperialUnits()) {
            return String.format(Locale.US, "%.0f mph", kilometersPerHour / IConversion.MILES_TO_KILOMETER);
        }
        return String.format(Locale.US, "%.0f km/h", kilometersPerHour);
    }
}
