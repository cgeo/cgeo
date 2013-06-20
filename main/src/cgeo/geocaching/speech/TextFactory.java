package cgeo.geocaching.speech;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.IConversion;
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

        if (Settings.isUseMetricUnits()) {
            if (kilometers >= 5.0) {
                int quantity = Math.round(kilometers);
                return getQuantityString(R.plurals.tts_kilometers, quantity, String.valueOf(quantity));
            }
            if (kilometers >= 1.0) {
                float precision1 = Math.round(kilometers * 10.0f) / 10.0f;
                float precision0 = Math.round(kilometers);
                if (precision1 == precision0) {
                    // this is an int - e.g. 2 kilometers
                    int quantity = (int) precision0;
                    return getQuantityString(R.plurals.tts_kilometers, quantity, String.valueOf(quantity));
                }
                // this is no int - e.g. 1.7 kilometers
                String digits = String.format(Locale.getDefault(), "%.1f", kilometers);
                // always use the plural (9 leads to plural)
                return getQuantityString(R.plurals.tts_kilometers, 9, digits);
            }
            int meters = (int) Math.round(kilometers * 1000.0);
            if (meters > 50) {
                meters = (int) Math.round(meters / 10.0) * 10;
            }
            return getQuantityString(R.plurals.tts_meters, meters, String.valueOf(meters));
        }

        float miles = kilometers / IConversion.MILES_TO_KILOMETER;
        if (miles >= 3.0) {
            int quantity = Math.round(miles);
            return getQuantityString(R.plurals.tts_miles, quantity, String.valueOf(quantity));
        }
        if (miles >= 0.2) { // approx 1000 ft
            float precision1 = Math.round(miles * 10.0f) / 10.0f;
            float precision0 = Math.round(miles);
            if (precision1 == precision0) {
                // this is an int - e.g. 2 miles
                int quantity = (int) precision0;
                return getQuantityString(R.plurals.tts_miles, quantity, String.valueOf(quantity));
            }
            // this is no int - e.g. 1.7 miles
            String digits = String.format(Locale.getDefault(), "%.1f", miles);
            // always use the plural (9 leads to plural)
            return getQuantityString(R.plurals.tts_miles, 9, digits);
        }
        int feet = (int) (kilometers * 1000.0 * IConversion.METERS_TO_FEET);
        if (feet > 300) {
            feet = (int) Math.round(feet / 10.0) * 10;
        }
        return getQuantityString(R.plurals.tts_feet, feet, String.valueOf(feet));
    }

    private static String getString(int resourceId, Object... formatArgs) {
        return cgeoapplication.getInstance().getString(resourceId, formatArgs);
    }

    private static String getQuantityString(int resourceId, int quantity, Object... formatArgs) {
        return cgeoapplication.getInstance().getResources().getQuantityString(resourceId, quantity, formatArgs);
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
