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
    public enum Format {
        /** Example: "-0,123456 10,123456" */
        LAT_LON_DECDEGREE,
        /** Example: "-0.123456,10.123456" (unlocalized) */
        LAT_LON_DECDEGREE_COMMA,
        /** Example: "W 5° 12,345 N 10° 12,345" */
        LAT_LON_DECMINUTE,
        /** Example: "W 5° 12,345 | N 10° 12,345" */
        LAT_LON_DECMINUTE_PIPE,
        /** Example: "-0.123456" (unlocalized latitude) */
        LAT_DECDEGREE_RAW,
        /** Example: "W 5° 12,345" */
        LAT_DECMINUTE,
        /** Example: "W 5 12,345" */
        LAT_DECMINUTE_RAW,
        /** Example: "-0.123456" (unlocalized longitude) */
        LON_DECDEGREE_RAW,
        /** Example: "N 10° 12,345" */
        LON_DECMINUTE,
        /** Example: "N 10 12,345" */
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
        final double lat = Math.abs(latSigned);
        final double lon = Math.abs(lonSigned);
        final double latFloor = Math.floor(lat);
        final double lonFloor = Math.floor(lon);
        final double latMin = (lat - latFloor) * 60;
        final double lonMin = (lon - lonFloor) * 60;
        final char latDir = latSigned < 0 ? 'S' : 'N';
        final char lonDir = lonSigned < 0 ? 'W' : 'E';

        switch (format) {
            case LAT_LON_DECDEGREE:
                return String.format("%.6f %.6f", latSigned, lonSigned);

            case LAT_LON_DECDEGREE_COMMA:
                return String.format((Locale) null, "%.6f,%.6f", latSigned, lonSigned);

            case LAT_LON_DECMINUTE:
                return String.format("%c %02.0f° %.3f %c %03.0f° %.3f",
                        latDir, latFloor, latMin, lonDir, lonFloor, lonMin);

            case LAT_LON_DECMINUTE_PIPE:
                return String.format("%c %02.0f° %.3f | %c %03.0f° %.3f",
                        latDir, latFloor, latMin, lonDir, lonFloor, lonMin);

            case LAT_DECDEGREE_RAW:
                return String.format((Locale) null, "%.6f", latSigned);

            case LAT_DECMINUTE:
                return String.format("%c %02.0f° %.3f", latDir, latFloor, latMin);

            case LAT_DECMINUTE_RAW:
                return String.format("%c %02.0f %.3f", latDir, latFloor, latMin);

            case LON_DECDEGREE_RAW:
                return String.format((Locale) null, "%.6f", lonSigned);

            case LON_DECMINUTE:
                return String.format("%c %03.0f° %.3f", lonDir, lonFloor, lonMin);

            case LON_DECMINUTE_RAW:
                return String.format("%c %03.0f %.3f", lonDir, lonFloor, lonMin);
        }

        // Keep the compiler happy even though it cannot happen
        return null;
    }

}
