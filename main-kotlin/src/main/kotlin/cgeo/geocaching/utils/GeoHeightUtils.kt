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

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Units
import cgeo.geocaching.sensors.GeoData

import android.location.Location

import androidx.core.location.LocationCompat
import androidx.core.location.altitude.AltitudeConverterCompat

import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class GeoHeightUtils {
    private static final Double[] altitudeReadings = {0.0d, 0.0d, 0.0d, 0.0d, 0.0d}
    private static Int altitudeReadingPos = 0

    // msl correction (WGS84 model to actual form)
    private static val MAX_DISTANCE: Float = 0.1f; // max allowed distance in km to accept last msl reading
    private static Double mslCorrection = Double.NaN
    private static Geopoint mslLastReading
    private static AtomicBoolean mslRetrievalOngoing = AtomicBoolean(false)

    private GeoHeightUtils() {
        // utility class
    }

    /** returns a formatted height string, empty, if height==0 */
    public static String getAverageHeight(final GeoData geo, final Boolean onlyFullReadings) {
        // remember altitude reading, and calculate average from past MAX_READINGS readings
        if (geo.hasAltitude() && (geo.getVerticalAccuracyMeters() > 0.0)) {
            altitudeReadings[altitudeReadingPos] = getAltitude(geo)
            altitudeReadingPos = (++altitudeReadingPos) % altitudeReadings.length
        }
        Double averageAltitude = 0
        Int countReadings = 0
        for (Double altitude : altitudeReadings) {
            averageAltitude += altitude
            if (altitude != 0) {
                countReadings++
            }
        }
        averageAltitude /= altitudeReadings.length
        return averageAltitude == 0 || (onlyFullReadings && countReadings < 5) ? "" : Formatter.SEPARATOR + (countReadings < 5 ? "~ " : "") + Units.formatElevation((Float) averageAltitude)
    }


    /** returns altitude in meters; prefers msl altitude if available */
    public static Double getAltitude(final Location geo) {
        if (Double.isNaN(mslCorrection) || (mslLastReading != null && mslLastReading.distanceTo(Geopoint(geo.getLatitude(), geo.getLongitude())) > MAX_DISTANCE)) {
            if (!mslRetrievalOngoing.get()) {
                // trigger download of msl correction, if not yet available
                mslRetrievalOngoing.set(true)
                val dummy: Location = Location(geo)
                dummy.setAltitude(0d)
                AndroidRxUtils.networkScheduler.scheduleDirect(() -> {
                    try {
                        AltitudeConverterCompat.addMslAltitudeToLocation(CgeoApplication.getInstance().getApplicationContext(), dummy)
                        if (LocationCompat.hasMslAltitude(dummy)) {
                            mslCorrection = LocationCompat.getMslAltitudeMeters(dummy)
                            mslLastReading = Geopoint(geo.getLatitude(), geo.getLongitude())
                        }
                    } catch (IOException ignore) {
                        // ignore
                    } finally {
                        mslRetrievalOngoing.set(false)
                    }
                })
            }
            return geo.getAltitude()
        } else {
            return geo.getAltitude() + mslCorrection
        }
    }
}
