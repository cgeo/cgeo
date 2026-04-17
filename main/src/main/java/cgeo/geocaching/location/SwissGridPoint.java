package cgeo.geocaching.location;

import cgeo.geocaching.utils.MatcherWrapper;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Represents Swiss grid coordinates (CH1903+/LV95 and CH1903/LV03).
 */
public class SwissGridPoint {

    private static final double LV95_EASTING_OFFSET = 2000000.0d;
    private static final double LV95_NORTHING_OFFSET = 1000000.0d;

        //                                                    (1)                   (2)                         (3)
        static final Pattern PATTERN_SWISS = Pattern.compile(
            "(^|\\s)(CH1903\\+?|LV95|LV03|SWISS(?:\\s*GRID)?)\\s*(?:E\\s*)?(\\d{5,7}(?:[.,]\\d+)?)"
                + "\\s*(?:[,;/]|\\s)+(?:N\\s*)?(\\d{5,7}(?:[.,]\\d+)?)(?=$|\\s|[.,;:])",
            Pattern.CASE_INSENSITIVE
        );

    private final double lv95Easting;
    private final double lv95Northing;

    public SwissGridPoint(final String swissGridString) {
        final MatcherWrapper matcher = new MatcherWrapper(PATTERN_SWISS, swissGridString);
        if (!matcher.find()) {
            throw new ParseException("Unable to recognize Swiss grid format in String '" + swissGridString + "'");
        }

        final String prefix = matcher.group(2).toUpperCase(Locale.US).replace(" ", "");
        final double easting = parseDouble(matcher.group(3), swissGridString);
        final double northing = parseDouble(matcher.group(4), swissGridString);

        final boolean isLv95;
        if ("LV03".equals(prefix)) {
            isLv95 = false;
        } else if ("LV95".equals(prefix) || "CH1903+".equals(prefix)) {
            isLv95 = true;
        } else {
            isLv95 = easting >= 1000000.0d || northing >= 1000000.0d;
        }

        this.lv95Easting = isLv95 ? easting : easting + LV95_EASTING_OFFSET;
        this.lv95Northing = isLv95 ? northing : northing + LV95_NORTHING_OFFSET;
    }

    private SwissGridPoint(final double lv95Easting, final double lv95Northing) {
        this.lv95Easting = lv95Easting;
        this.lv95Northing = lv95Northing;
    }

    @NonNull
    public static SwissGridPoint latLong2SwissGrid(final Geopoint geopoint) {
        final double latSexagesimal = geopoint.getLatitude() * 3600.0d;
        final double lonSexagesimal = geopoint.getLongitude() * 3600.0d;

        final double latAux = (latSexagesimal - 169028.66d) / 10000.0d;
        final double lonAux = (lonSexagesimal - 26782.5d) / 10000.0d;

        final double lv95Easting = 2600072.37d
                + 211455.93d * lonAux
                - 10938.51d * lonAux * latAux
                - 0.36d * lonAux * latAux * latAux
                - 44.54d * lonAux * lonAux * lonAux;

        final double lv95Northing = 1200147.07d
                + 308807.95d * latAux
                + 3745.25d * lonAux * lonAux
                + 76.63d * latAux * latAux
                - 194.56d * lonAux * lonAux * latAux
                + 119.79d * latAux * latAux * latAux;

        return new SwissGridPoint(lv95Easting, lv95Northing);
    }

    @NonNull
    public Geopoint toLatLong() {
        final double yAux = (lv95Easting - 2600000.0d) / 1000000.0d;
        final double xAux = (lv95Northing - 1200000.0d) / 1000000.0d;

        final double lat = 16.9023892d
                + 3.238272d * xAux
                - 0.270978d * yAux * yAux
                - 0.002528d * xAux * xAux
                - 0.0447d * yAux * yAux * xAux
                - 0.0140d * xAux * xAux * xAux;

        final double lon = 2.6779094d
                + 4.728982d * yAux
                + 0.791484d * yAux * xAux
                + 0.1306d * yAux * xAux * xAux
                - 0.0436d * yAux * yAux * yAux;

        return new Geopoint(lat * 100.0d / 36.0d, lon * 100.0d / 36.0d);
    }

    @Override
    @NonNull
    public String toString() {
        return String.format(Locale.getDefault(), "LV95 E %d N %d", Math.round(lv95Easting), Math.round(lv95Northing));
    }

    public double getLv95Easting() {
        return lv95Easting;
    }

    public double getLv95Northing() {
        return lv95Northing;
    }

    private static double parseDouble(final String value, final String source) {
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (final NumberFormatException ignored) {
            throw new ParseException("Invalid Swiss grid number in String '" + source + "'");
        }
    }

    public static class ParseException extends NumberFormatException {
        private static final long serialVersionUID = 1L;

        ParseException(final String message) {
            super(message);
        }
    }
}
