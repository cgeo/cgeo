package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.sensors.GeoData;

import android.location.Location;
import android.os.Build;

import androidx.core.location.LocationCompat;
import androidx.core.location.altitude.AltitudeConverterCompat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GeoHeightUtils {
    private static final double[] altitudeReadings = {0.0d, 0.0d, 0.0d, 0.0d, 0.0d};
    private static int altitudeReadingPos = 0;

    // msl correction (WGS84 model to actual form)
    private static final float MAX_DISTANCE = 0.1f; // max allowed distance in km to accept last msl reading
    private static double mslCorrection = Double.NaN;
    private static Geopoint mslLastReading;
    private static AtomicBoolean mslRetrievalOngoing = new AtomicBoolean(false);

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
        averageAltitude /= altitudeReadings.length;
        return averageAltitude == 0 || (onlyFullReadings && countReadings < 5) ? "" : Formatter.SEPARATOR + (countReadings < 5 ? "~ " : "") + Units.formatElevation((float) averageAltitude);
    }


    /** returns altitude in meters; prefers msl altitude if available */
    public static double getAltitude(final Location geo) {
        if (Double.isNaN(mslCorrection) || (mslLastReading != null && mslLastReading.distanceTo(new Geopoint(geo.getLatitude(), geo.getLongitude())) > MAX_DISTANCE)) {
            if (!mslRetrievalOngoing.get()) {
                // trigger download of msl correction, if not yet available
                mslRetrievalOngoing.set(true);
                final Location dummy = new Location(geo);
                dummy.setAltitude(0d);
                AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
                    try {
                        AltitudeConverterCompat.addMslAltitudeToLocation(CgeoApplication.getInstance().getApplicationContext(), dummy);
                        if (LocationCompat.hasMslAltitude(dummy)) {
                            mslCorrection = LocationCompat.getMslAltitudeMeters(dummy);
                            mslLastReading = new Geopoint(geo.getLatitude(), geo.getLongitude());
                        }
                    } catch (IOException ignore) {
                        // ignore
                    } finally {
                        mslRetrievalOngoing.set(false);
                    }
                });
            }
            return geo.getAltitude();
        } else {
            return geo.getAltitude() + mslCorrection;
        }
    }
}
