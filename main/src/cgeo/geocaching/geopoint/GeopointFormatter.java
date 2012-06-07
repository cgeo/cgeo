package cgeo.geocaching.geopoint;

import java.util.Locale;

/**
 * Formatting of Geopoint.
 */
public class GeopointFormatter
{
    /**
     * Predefined formats.
     */
    public static enum Format {
        /** Example: "10,123456 -0,123456" */
        LAT_LON_DECDEGREE,

        /** Example: "10.123456 -0.123456" (unlocalized) */
        LAT_LON_DECDEGREE_RAW,

        /** Example: "10.123456,-0.123456" (unlocalized) */
        LAT_LON_DECDEGREE_COMMA,

        /** Example: "N 10° 12,345 · W 5° 12,345" */
        LAT_LON_DECMINUTE,

        /** Example: "N 10° 12.345 W 5° 12.345" */
        LAT_LON_DECMINUTE_RAW,

        /** Example: "N 10° 12' 34" W 5° 12' 34"" */
        LAT_LON_DECSECOND,

        /** Example: "-0.123456" (unlocalized latitude) */
        LAT_DECDEGREE_RAW,

        /** Example: "N 10° 12,345" */
        LAT_DECMINUTE,

        /** Example: "N 10 12,345" */
        LAT_DECMINUTE_RAW,

        /** Example: "-0.123456" (unlocalized longitude) */
        LON_DECDEGREE_RAW,

        /** Example: "W 5° 12,345" */
        LON_DECMINUTE,

        /** Example: "W 5 12,345" */
        LON_DECMINUTE_RAW;
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
    public static String format(final Format format, final Geopoint gp)
    {
        final double latSigned = gp.getLatitude();
        final double lonSigned = gp.getLongitude();

        switch (format) {
            case LAT_LON_DECDEGREE:
                return String.format("%.6f %.6f", latSigned, lonSigned);

            case LAT_LON_DECDEGREE_COMMA:
                return String.format((Locale) null, "%.6f,%.6f", latSigned, lonSigned);

            case LAT_LON_DECMINUTE:
                return String.format("%c %02d° %06.3f · %c %03d° %06.3f",
                        gp.getLatDir(), gp.getLatDeg(), gp.getLatMinRaw(), gp.getLonDir(), gp.getLonDeg(), gp.getLonMinRaw());

            case LAT_LON_DECMINUTE_RAW:
                return String.format((Locale) null, "%c %02d° %06.3f %c %03d° %06.3f",
                        gp.getLatDir(), gp.getLatDeg(), gp.getLatMinRaw(), gp.getLonDir(), gp.getLonDeg(), gp.getLonMinRaw());

            case LAT_LON_DECSECOND:
                return String.format("%c %02d° %02d' %06.3f\" · %c %03d° %02d' %06.3f\"",
                        gp.getLatDir(), gp.getLatDeg(), gp.getLatMin(), gp.getLatSecRaw(),
                        gp.getLonDir(), gp.getLonDeg(), gp.getLonMin(), gp.getLonSecRaw());

            case LAT_LON_DECDEGREE_RAW:
                return String.format((Locale) null, "%.6f %.6f", latSigned, lonSigned);

            case LAT_DECDEGREE_RAW:
                return String.format((Locale) null, "%.6f", latSigned);

            case LAT_DECMINUTE:
                return String.format("%c %02d° %06.3f", gp.getLatDir(), gp.getLatDeg(), gp.getLatMinRaw());

            case LAT_DECMINUTE_RAW:
                return String.format("%c %02d %06.3f", gp.getLatDir(), gp.getLatDeg(), gp.getLatMinRaw());

            case LON_DECDEGREE_RAW:
                return String.format((Locale) null, "%.6f", lonSigned);

            case LON_DECMINUTE:
                return String.format("%c %03d° %06.3f", gp.getLonDir(), gp.getLonDeg(), gp.getLonMinRaw());

            case LON_DECMINUTE_RAW:
                return String.format("%c %03d %06.3f", gp.getLonDir(), gp.getLonDeg(), gp.getLonMinRaw());
        }

        // Keep the compiler happy even though it cannot happen
        return null;
    }

}
