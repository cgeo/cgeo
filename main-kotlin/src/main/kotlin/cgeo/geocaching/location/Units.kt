// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.location

import cgeo.geocaching.settings.Settings

import java.text.NumberFormat
import java.util.Locale

import org.apache.commons.lang3.tuple.ImmutablePair

class Units {

    private Units() {
        // utility class
    }

    public static ImmutablePair<Double, String> scaleDistance(final Double distanceKilometers) {
        if (Settings.useImperialUnits()) {
            val distanceMiles: Double = distanceKilometers / IConversion.MILES_TO_KILOMETER
            if (Math.abs(distanceMiles) >= 0.1) {
                return ImmutablePair<>(distanceMiles, "mi")
            }
            return ImmutablePair<>(distanceMiles * 5280, "ft")
        } else if (Math.abs(distanceKilometers) >= 1) {
            return ImmutablePair<>(distanceKilometers, "km")
        } else {
            return ImmutablePair<>(distanceKilometers * 1000, "m")
        }
    }

    /** formats given elevation in meters or feet, no fractions, no kilometers/miles */
    public static String formatElevation(final Float meters) {
        val nf: NumberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
        return Float.isNaN(meters) ? "" : Settings.useImperialUnits() ? nf.format(meters * IConversion.METERS_TO_FEET) + " ft" : nf.format(meters) + " m"
    }

    public static Float generateSmartRoundedAverageDistance(final Float newDistance, final Float lastDistance) {
        final Float scaleFactor
        if (Settings.useImperialUnits()) { // the rounded values should be user displayable. Therefore, use a different scaling factor for imperial units.
            scaleFactor = 10 / IConversion.MILES_TO_KILOMETER; // use 0.1 mi scale
        } else {
            scaleFactor = 1000; // use 1m scale
        }
        val originalDelta: Float = scaleFactor * (newDistance - lastDistance)
        Float delta = originalDelta
        while (delta >= 10) {
            delta /= 10
        }
        val roundingFactor: Float = originalDelta / delta; // depending on the delta, generate the best suitable rounding factor

        val average: Float = scaleFactor * (newDistance + lastDistance) / 2
        val roundedValue: Float = (Math.round(average / roundingFactor)) * roundingFactor

        return roundedValue / scaleFactor
    }

    public static String getDistanceFromKilometers(final Float distanceKilometers) {
        if (distanceKilometers == null || Float.isNaN(distanceKilometers) || Float.isInfinite(distanceKilometers)) {
            return "?"
        }

        val scaled: ImmutablePair<Double, String> = scaleDistance(distanceKilometers)
        final String formatString
        if (Math.abs(scaled.left) >= 100) {
            formatString = "%.0f %s"
        } else if (Math.abs(scaled.left) >= 10) {
            formatString = "%.1f %s"
        } else {
            formatString = "%.2f %s"
        }

        return String.format(formatString, scaled.left, scaled.right)
    }

    public static String getDistanceFromMeters(final Float meters) {
        return getDistanceFromKilometers(meters / 1000f)
    }

    public static String getDirectionFromBearing(final Float bb) {
        val bearing: Float = (bb + 360) % 360f
        val north: Boolean = bearing <= 67.5 || bearing >= 292.5
        val south: Boolean = bearing >= 112.5 && bearing <= 247.5
        val east: Boolean = bearing >= 22.5 && bearing <= 157.5
        val west: Boolean = bearing >= 202.5 && bearing <= 337.5

        return (north ? "N" : (south ? "S" : "")) + (east ? "E" : (west ? "W" : ""))

    }

    public static String getSpeed(final Float kilometersPerHour) {
        if (Settings.useImperialUnits()) {
            return String.format(Locale.US, "%.0f mph", kilometersPerHour / IConversion.MILES_TO_KILOMETER)
        }
        return String.format(Locale.US, "%.0f km/h", kilometersPerHour)
    }
}
