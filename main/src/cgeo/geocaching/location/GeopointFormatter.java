package cgeo.geocaching.location;

import cgeo.geocaching.utils.Formatter;

import java.util.Locale;

/**
 * Formatting of Geopoint.
 */
public class GeopointFormatter {

    private GeopointFormatter() {
        // utility class
    }

    /**
     * Predefined formats.
     */
    public enum Format {
        /** Example: "10,123456 -0,123456" */
        LAT_LON_DECDEGREE,

        /** Example: "10.123456,-0.123456" (unlocalized) */
        LAT_LON_DECDEGREE_COMMA,

        /** Example: "N 10° 12,345 · W 5° 12,345" */
        LAT_LON_DECMINUTE,

        /** Example: "N10 12,345 W5 12,345" */
        LAT_LON_DECMINUTE_SHORT,

        /** Example: "N 10° 12.345 W 5° 12.345" */
        LAT_LON_DECMINUTE_RAW,

        /** Example: "N 10° 12' 34" W 5° 12' 34" */
        LAT_LON_DECSECOND,

        /** Example: "N 0.123456°" */
        LAT_DECDEGREE,

        /** Example: "-0.123456" (unlocalized latitude) */
        LAT_DECDEGREE_RAW,

        /** Example: "N 01° 02.034" */
        LAT_DECMINUTE,

        /** Example: "N 10 12,345" */
        LAT_DECMINUTE_RAW,

        /** Example: "n 01° 02' 03.045" */
        LAT_DECMINSEC,

        /** Example: "W 0.123456°" */
        LON_DECDEGREE,

        /** Example: "-0.123456" (unlocalized longitude) */
        LON_DECDEGREE_RAW,

        /** Example: "W 001° 02.034" */
        LON_DECMINUTE,

        /** Example: "W 5 12,345" */
        LON_DECMINUTE_RAW,

        /** Example: "W 001° 02' 03.045" */
        LON_DECMINSEC,

        /** Example: "32U E 549996 N 5600860" */
        UTM
    }

    /**
     * Formats a Geopoint.
     *
     * @param gp
     *            the Geopoint to format
     * @param format
     *            one of the predefined formats
     * @return the formatted coordinates
     */
    public static String format(final Format format, final Geopoint gp) {
        final double latSigned = gp.getLatitude();
        final double lonSigned = gp.getLongitude();

        switch (format) {
            case LAT_LON_DECDEGREE:
                return String.format(Locale.getDefault(), "%.6f° %.6f°", latSigned, lonSigned);

            case LAT_LON_DECDEGREE_COMMA:
                return String.format((Locale) null, "%.6f,%.6f", latSigned, lonSigned);

            case LAT_LON_DECMINUTE: {
                final Geopoint rgp = gp.roundedAt(60 * 1000);
                return String.format(Locale.getDefault(), "%c %02d° %06.3f\'" + Formatter.SEPARATOR + "%c %03d° %06.3f\'",
                        rgp.getLatDir(), rgp.getLatDeg(), rgp.getLatMinRaw(),
                        rgp.getLonDir(), rgp.getLonDeg(), rgp.getLonMinRaw());
            }

            case LAT_LON_DECMINUTE_SHORT: {
                final Geopoint rgp = gp.roundedAt(60 * 1000);
                return String.format(Locale.getDefault(), "%c%d %06.3f %c%d %06.3f",
                        rgp.getLatDir(), rgp.getLatDeg(), rgp.getLatMinRaw(),
                        rgp.getLonDir(), rgp.getLonDeg(), rgp.getLonMinRaw());
            }

            case LAT_LON_DECMINUTE_RAW: {
                final Geopoint rgp = gp.roundedAt(60 * 1000);
                return String.format((Locale) null, "%c %02d° %06.3f %c %03d° %06.3f",
                        rgp.getLatDir(), rgp.getLatDeg(), rgp.getLatMinRaw(),
                        rgp.getLonDir(), rgp.getLonDeg(), rgp.getLonMinRaw());
            }

            case LAT_LON_DECSECOND: {
                final Geopoint rgp = gp.roundedAt(3600 * 1000);
                return String.format(Locale.getDefault(), "%c %02d° %02d' %06.3f\"" + Formatter.SEPARATOR + "%c %03d° %02d' %06.3f\"",
                        rgp.getLatDir(), rgp.getLatDeg(), rgp.getLatMin(), rgp.getLatSecRaw(),
                        rgp.getLonDir(), rgp.getLonDeg(), rgp.getLonMin(), rgp.getLonSecRaw());
            }

            case LAT_DECDEGREE:
                return String.format((Locale) null, "%c %.5f°", gp.getLatDir(), Math.abs(latSigned));

            case LAT_DECDEGREE_RAW:
                return String.format((Locale) null, "%.6f", latSigned);

            case LAT_DECMINUTE: {
                final Geopoint rgp = gp.roundedAt(60 * 1000);
                return String.format(Locale.getDefault(), "%c %02d° %06.3f\'", rgp.getLatDir(), rgp.getLatDeg(), rgp.getLatMinRaw());
            }

            case LAT_DECMINUTE_RAW: {
                final Geopoint rgp = gp.roundedAt(60 * 1000);
                return String.format(Locale.getDefault(), "%c %02d %06.3f", rgp.getLatDir(), rgp.getLatDeg(), rgp.getLatMinRaw());
            }

            case LAT_DECMINSEC: {
                final Geopoint rgp = gp.roundedAt(3600 * 1000);
                return String.format(Locale.getDefault(), "%c %02d° %02d\' %06.3f\"", rgp.getLatDir(), rgp.getLatDeg(), rgp.getLatMin(), rgp.getLatSecRaw());
            }

            case LON_DECDEGREE:
                return String.format((Locale) null, "%c %.5f°", gp.getLonDir(), Math.abs(lonSigned));

            case LON_DECDEGREE_RAW:
                return String.format((Locale) null, "%.6f", lonSigned);

            case LON_DECMINUTE: {
                final Geopoint rgp = gp.roundedAt(60 * 1000);
                return String.format(Locale.getDefault(), "%c %03d° %06.3f\'", rgp.getLonDir(), rgp.getLonDeg(), rgp.getLonMinRaw());
            }

            case LON_DECMINUTE_RAW: {
                final Geopoint rgp = gp.roundedAt(60 * 1000);
                return String.format(Locale.getDefault(), "%c %03d %06.3f", rgp.getLonDir(), rgp.getLonDeg(), rgp.getLonMinRaw());
            }

            case LON_DECMINSEC: {
                final Geopoint rgp = gp.roundedAt(3600 * 1000);
                return String.format(Locale.getDefault(), "%c %03d° %02d\' %06.3f\"", rgp.getLonDir(), rgp.getLonDeg(), rgp.getLonMin(), rgp.getLonSecRaw());
            }

            case UTM: {
                return UTMPoint.latLong2UTM(gp).toString();
            }

        }
        throw new IllegalStateException(); // cannot happen, if switch case is enum complete
    }

    /**
     * Reformats coordinates for Clipboard.
     * It removes the middle dot if present.
     */
    public static CharSequence reformatForClipboard(final CharSequence coordinatesToCopy) {
        return coordinatesToCopy.toString().replace(Formatter.SEPARATOR, " ").replaceAll(",", ".");
    }
}
