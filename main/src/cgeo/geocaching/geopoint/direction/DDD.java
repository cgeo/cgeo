package cgeo.geocaching.geopoint.direction;

import cgeo.geocaching.geopoint.Geopoint;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value type for the DDD.DDDDD format.
 */
public final class DDD extends Direction {

    /** latitude degree value */
    public final int latDeg;
    /** fractional part of the latitude degree value */
    public final int latDegFrac;

    public final int lonDeg;
    public final int lonDegFrac;

    public DDD(final double latSigned, final double lonSigned) {
        super(latSigned, lonSigned);
        BigDecimal bdLat = BigDecimal.valueOf(latSigned).abs();
        latDeg = bdLat.intValue();
        BigDecimal bdLatFrac = bdLat.subtract(BigDecimal.valueOf(latDeg)).multiply(BD_ONEHOUNDREDTHOUSAND);
        latDegFrac = bdLatFrac.setScale(0, RoundingMode.HALF_UP).intValue();

        BigDecimal bdlon = BigDecimal.valueOf(lonSigned).abs();
        lonDeg = bdlon.intValue();
        BigDecimal bdLonFrac = bdlon.subtract(BigDecimal.valueOf(lonDeg)).multiply(BD_ONEHOUNDREDTHOUSAND);
        lonDegFrac = bdLonFrac.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public static Geopoint createGeopoint(final String latDir, final String latDeg, final String latDegFrac,
            final String lonDir, final String lonDeg, final String lonDegFrac) {
        double lat = 0.0d;
        double lon = 0.0d;
        try {
            lat = Double.parseDouble(latDeg + "." + addZeros(Integer.parseInt(latDegFrac), 5));
            lon = Double.parseDouble(lonDeg + "." + addZeros(Integer.parseInt(lonDegFrac), 5));
        } catch (NumberFormatException e) {
        }
        lat *= "S".equalsIgnoreCase(latDir) ? -1 : 1;
        lon *= "W".equalsIgnoreCase(lonDir) ? -1 : 1;
        return new Geopoint(lat, lon);
    }
}