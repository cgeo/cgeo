package cgeo.geocaching.utils;

import cgeo.geocaching.location.Units;
import cgeo.geocaching.sensors.GeoData;

import android.location.Location;
import android.os.Build;

public class GeoHeightUtils {
    private static final double[] altitudeReadings = {0.0d, 0.0d, 0.0d, 0.0d, 0.0d};
    private static int altitudeReadingPos = 0;

    private GeoHeightUtils() {
        // utility class
    }

    /** returns a formatted height string, empty, if height==0 */
    public static String getAverageHeight(final GeoData geo, final boolean onlyFullReadings) {
        // remember new altitude reading, and calculate average from past MAX_READINGS readings
        if (geo.hasAltitude() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || geo.getVerticalAccuracyMeters() > 0.0)) {
            altitudeReadings[altitudeReadingPos] = getAltitude(geo);
            altitudeReadingPos = (++altitudeReadingPos) % altitudeReadings.length;
        }
        double averageAltitude = 0;
        int countReadings = 0;
        for (double altitude : altitudeReadings) {
            averageAltitude += altitude;
            if (altitude != 0) {
                countReadings++;
            }
        }
        averageAltitude /= (double) altitudeReadings.length;
        return averageAltitude == 0 || (onlyFullReadings && countReadings < 5) ? "" : Formatter.SEPARATOR + (countReadings < 5 ? "~ " : "") + Units.formatElevation((float) averageAltitude);
    }


    /** returns altitude in meters; prefers msl altitude if available */
    public static double getAltitude(final Location geo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (geo.hasMslAltitude()) {
                return geo.getMslAltitudeMeters();
            }
        }
        return geo.getAltitude();
    }
}
