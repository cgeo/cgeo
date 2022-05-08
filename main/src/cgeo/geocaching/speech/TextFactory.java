package cgeo.geocaching.speech;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.IConversion;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AngleUtils;

import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

import java.util.Locale;

/**
 * Creates the output to be read by TTS.
 *
 * Note: some languages need to read "one hour" as "a hour" (indefinite article). Also, other languages
 * use the <tt>quantity="1"</tt> plurals rule for other values than 1, such as Slovenian, so it is not
 * possible to store the literal value to use for 1 in this rule. For this reason, we need to have one
 * string for the unit quantity ("one meter") and a plurals rule for everything else.
 *
 * See http://unicode.org/repos/cldr-tmp/trunk/diff/supplemental/language_plural_rules.html for rules
 * on unit expressions.
 */
public class TextFactory {
    private TextFactory() {
        // utility class
    }

    public static String getText(final Geopoint position, final Geopoint target, final float direction) {
        if (position == null || target == null) {
            return null;
        }
        return getDirection(position, target, direction) + ". " + getDistance(position, target);
    }

    public static String getText(final float kilometers) {
        return getDistance(kilometers);
    }

    private static String getDistance(final Geopoint position, final Geopoint target) {
        final float kilometers = position.distanceTo(target);
        return getDistance(kilometers);
    }

    private static String getDistance(final float kilometers) {
        if (Settings.useImperialUnits()) {
            return getDistance(kilometers / IConversion.MILES_TO_KILOMETER,
                    (int) (kilometers * 1000.0 * IConversion.METERS_TO_FEET),
                    3.0f, 0.2f, 300,
                    R.plurals.tts_miles, R.string.tts_one_mile,
                    R.plurals.tts_feet, R.string.tts_one_foot);
        }
        return getDistance(kilometers, (int) (kilometers * 1000.0),
                5.0f, 1.0f, 50,
                R.plurals.tts_kilometers, R.string.tts_one_kilometer,
                R.plurals.tts_meters, R.string.tts_one_meter);
    }

    private static String getDistance(final float farDistance, final int nearDistance,
                                      final float farFarAway, final float farNearAway, final int nearFarAway,
                                      @PluralsRes final int farId, @StringRes final int farOneId, @PluralsRes final int nearId, @StringRes final int nearOneId) {
        if (farDistance >= farFarAway) {
            // example: "5 kilometers" - always without decimal digits
            final int quantity = Math.round(farDistance);
            if (quantity == 1) {
                return getString(farOneId, quantity, String.valueOf(quantity));
            }
            return getQuantityString(farId, quantity, String.valueOf(quantity));
        }
        if (farDistance >= farNearAway) {
            // example: "2.2 kilometers" - decimals if necessary
            final float precision1 = Math.round(farDistance * 10.0f) / 10.0f;
            final float precision0 = Math.round(farDistance);
            if (Math.abs(precision1 - precision0) < 0.0001) {
                // this is an int - e.g. 2 kilometers
                final int quantity = (int) precision0;
                if (quantity == 1) {
                    return getString(farOneId, quantity, String.valueOf(quantity));
                }
                return getQuantityString(farId, quantity, String.valueOf(quantity));
            }
            // this is no int - e.g. 1.7 kilometers
            final String digits = String.format(Locale.getDefault(), "%.1f", farDistance);
            // always use the plural (9 leads to plural)
            return getQuantityString(farId, 9, digits);
        }
        // example: "34 meters"
        int quantity = nearDistance;
        if (quantity > nearFarAway) {
            // example: "120 meters" - rounded to 10 meters
            quantity = (int) Math.round(quantity / 10.0) * 10;
        }
        if (quantity == 1) {
            return getString(nearOneId, quantity, String.valueOf(quantity));
        }
        return getQuantityString(nearId, quantity, String.valueOf(quantity));
    }

    private static String getString(@StringRes final int resourceId, final Object... formatArgs) {
        return CgeoApplication.getInstance().getString(resourceId, formatArgs);
    }

    private static String getQuantityString(@PluralsRes final int resourceId, final int quantity, final Object... formatArgs) {
        return CgeoApplication.getInstance().getResources().getQuantityString(resourceId, quantity, formatArgs);
    }

    private static String getDirection(final Geopoint position, final Geopoint target, final float direction) {
        int hours;

        if (position.distanceTo(target) < 1e-4) {
            // By convention, distance smaller than ten centimers will be represented as 12 o'clock.
            hours = 12;
        } else {
            final float bearing = position.bearingTo(target);
            final float degrees = AngleUtils.normalize(bearing - direction);

            hours = Math.round(degrees / 30);
            if (hours == 0) {
                hours = 12;
            }
        }

        return getString(hours == 1 ? R.string.tts_one_oclock : R.string.tts_oclock, String.valueOf(hours));
    }
}
