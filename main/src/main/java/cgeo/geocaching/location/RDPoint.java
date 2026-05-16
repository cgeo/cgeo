package cgeo.geocaching.location;

import cgeo.geocaching.utils.MatcherWrapper;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Represents Dutch Rijksdriehoek (RD) coordinates.
 */
public class RDPoint {

        //                                                 (1)                               (2)                         (3)
        static final Pattern PATTERN_RD = Pattern.compile(
            "(^|\\s)(RD|RIJKSDRIEHOEK(?:STELSEL)?)\\s*(?:X\\s*)?(\\d{5,6}(?:[.,]\\d+)?)"
                + "\\s*(?:[,;/]|\\s)+(?:Y\\s*)?(\\d{5,6}(?:[.,]\\d+)?)(?=$|\\s|[.,;:])",
            Pattern.CASE_INSENSITIVE
        );

    private static final double X0 = 155000.0d;
    private static final double Y0 = 463000.0d;
    private static final double LAT0 = 52.15517440d;
    private static final double LON0 = 5.38720621d;

    private final double x;
    private final double y;

    public RDPoint(final String rdString) {
        final MatcherWrapper matcher = new MatcherWrapper(PATTERN_RD, rdString);
        if (!matcher.find()) {
            throw new ParseException("Unable to recognize RD format in String '" + rdString + "'");
        }

        this.x = parseDouble(matcher.group(3), rdString);
        this.y = parseDouble(matcher.group(4), rdString);
    }

    private RDPoint(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    @NonNull
    public static RDPoint latLong2RD(final Geopoint geopoint) {
        final double dLat = 0.36d * (geopoint.getLatitude() - LAT0);
        final double dLon = 0.36d * (geopoint.getLongitude() - LON0);

        final double x = X0
                + 190094.945d * dLon
                - 11832.228d * dLat * dLon
                - 114.221d * dLat * dLat * dLon
                - 32.391d * dLon * dLon * dLon
                - 0.705d * dLat
                - 2.340d * dLat * dLat * dLat * dLon
                - 0.608d * dLat * dLon * dLon * dLon
                - 0.008d * dLon * dLon
                + 0.148d * dLat * dLat * dLon * dLon * dLon;

        final double y = Y0
                + 309056.544d * dLat
                + 3638.893d * dLon * dLon
                + 73.077d * dLat * dLat
                - 157.984d * dLat * dLon * dLon
                + 59.788d * dLat * dLat * dLat
                + 0.433d * dLon
                - 6.439d * dLat * dLat * dLon * dLon
                - 0.032d * dLat * dLon
                + 0.092d * dLon * dLon * dLon * dLon
                - 0.054d * dLat * dLon * dLon * dLon * dLon;

        return new RDPoint(x, y);
    }

    @NonNull
    public Geopoint toLatLong() {
        final double dX = (x - X0) * 1e-5;
        final double dY = (y - Y0) * 1e-5;

        final double lat = LAT0 + (
                3235.65389d * dY
                        - 32.58297d * dX * dX
                        - 0.2475d * dY * dY
                        - 0.84978d * dX * dX * dY
                        - 0.0655d * dY * dY * dY
                        - 0.01709d * dX * dX * dY * dY
                        - 0.00738d * dX
                        + 0.0053d * dX * dX * dX * dX
                        - 0.00039d * dX * dX * dY * dY * dY
                        + 0.00033d * dX * dX * dX * dX * dY
                        - 0.00012d * dX * dY
        ) / 3600.0d;

        final double lon = LON0 + (
                5260.52916d * dX
                        + 105.94684d * dX * dY
                        + 2.45656d * dX * dY * dY
                        - 0.81885d * dX * dX * dX
                        + 0.05594d * dX * dY * dY * dY
                        - 0.05607d * dX * dX * dX * dY
                        + 0.01199d * dY
                        - 0.00256d * dX * dX * dX * dY * dY
                        + 0.00128d * dX * dY * dY * dY * dY
                        + 0.00022d * dY * dY
                        - 0.00022d * dX * dX
                        + 0.00026d * dX * dX * dX * dX * dX
        ) / 3600.0d;

        return new Geopoint(lat, lon);
    }

    @Override
    @NonNull
    public String toString() {
        return String.format(Locale.getDefault(), "RD X %d Y %d", Math.round(x), Math.round(y));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    private static double parseDouble(final String value, final String source) {
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (final NumberFormatException ignored) {
            throw new ParseException("Invalid RD number in String '" + source + "'");
        }
    }

    public static class ParseException extends NumberFormatException {
        private static final long serialVersionUID = 1L;

        ParseException(final String message) {
            super(message);
        }
    }
}
