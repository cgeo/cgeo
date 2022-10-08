package cgeo.geocaching.utils;

import cgeo.geocaching.location.Units;
import cgeo.geocaching.sensors.GeoData;

import android.os.Build;

public class GeoHeightUtils {
    private static final double[] altitudeReadings = {0.0d, 0.0d, 0.0d, 0.0d, 0.0d};
    private static int altitudeReadingPos = 0;

    private GeoHeightUtils() {
        // utility class
    }

    /** returns a formatted height string, empty, if height==0 */
    public static String getAverageHeight(final GeoData geo, final boolean withSeparator) {
        // remember new altitude reading, and calculate average from past MAX_READINGS readings
        if (geo.hasAltitude() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || geo.getVerticalAccuracyMeters() > 0.0)) {
            altitudeReadings[altitudeReadingPos] = geo.getAltitude();
            altitudeReadingPos = (++altitudeReadingPos) % altitudeReadings.length;
        }
        double averageAltitude = altitudeReadings[0];
        for (int i = 1; i < altitudeReadings.length; i++) {
            averageAltitude += altitudeReadings[i];
        }
        averageAltitude /= (double) altitudeReadings.length;
        return averageAltitude == 0 ? "" : (withSeparator ? Formatter.SEPARATOR : "") + Units.getDistanceFromMeters((float) averageAltitude);
    }
}
