package cgeo.geocaching.speech;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.AngleUtils;

import java.util.Locale;

/**
 * Creates the output to be read by TTS.
 *
 */
public class TextFactory {
    public static String getText(Geopoint position, Geopoint target, float direction) {
        if (position == null || target == null) {
            return null;
        }
        return getDirection(position, target, direction) + ". " + getDistance(position, target);
    }

    private static String getDistance(Geopoint position, Geopoint target) {
        float kilometers = position.distanceTo(target);
        if (kilometers >= 5.0) {
            return getString(R.string.tts_kilometers, String.valueOf(Math.round(kilometers)));
        }
        if (kilometers >= 1.0) {
            String digits = String.format(Locale.getDefault(), "%.1f", kilometers);
            return getString(R.string.tts_kilometers, digits);
        }
        int meters = (int) (kilometers * 1000.0);
        if (meters > 50) {
            return getString(R.string.tts_meters, String.valueOf(Math.round(meters / 10.0) * 10));
        }
        return getString(R.string.tts_meters, String.valueOf(meters));
    }

    private static String getString(int resourceId, Object... formatArgs) {
        return cgeoapplication.getInstance().getString(resourceId, formatArgs);
    }

    private static String getDirection(Geopoint position, Geopoint target, float direction) {
        final int bearing = (int) position.bearingTo(target);
        int degrees = (int) AngleUtils.normalize(bearing - direction);

        int hours = (degrees + 15) / 30;
        if (hours == 0) {
            hours = 12;
        }
        return getString(R.string.tts_oclock, String.valueOf(hours));
    }
}
