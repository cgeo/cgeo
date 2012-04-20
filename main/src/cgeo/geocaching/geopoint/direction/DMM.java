package cgeo.geocaching.geopoint.direction;

import cgeo.geocaching.geopoint.Geopoint;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DMM extends Direction {

    public final int latDeg;
    public final double latMinRaw;
    public final int latMin;
    public final int latMinFrac;

    public final int lonDeg;
    public final double lonMinRaw;
    public final int lonMin;
    public final int lonMinFrac;

    public DMM(final double latSigned, final double lonSigned) {
        super(latSigned, lonSigned);
        BigDecimal bdLat = BigDecimal.valueOf(latSigned).abs();
        latDeg = bdLat.intValue();
        BigDecimal bdLatMin = bdLat.subtract(BigDecimal.valueOf(latDeg)).multiply(BD_SIXTY);
        // Rounding here ...
        bdLatMin = bdLatMin.setScale(3, RoundingMode.HALF_UP);
        latMinRaw = bdLatMin.doubleValue();
        latMin = bdLatMin.intValue();
        BigDecimal bdLatMinFrac = bdLatMin.subtract(BigDecimal.valueOf(latMin)).multiply(BD_THOUSAND);
        latMinFrac = bdLatMinFrac.setScale(0, RoundingMode.HALF_UP).intValue();

        BigDecimal bdlon = BigDecimal.valueOf(lonSigned).abs();
        lonDeg = bdlon.intValue();
        BigDecimal bdLonMin = bdlon.subtract(BigDecimal.valueOf(lonDeg)).multiply(BD_SIXTY);
        // Rounding here ...
        bdLonMin = bdLonMin.setScale(3, RoundingMode.HALF_UP);
        lonMinRaw = bdLonMin.doubleValue();
        lonMin = bdLonMin.intValue();
        BigDecimal bdLonMinFrac = bdLonMin.subtract(BigDecimal.valueOf(lonMin)).multiply(BD_THOUSAND);
        lonMinFrac = bdLonMinFrac.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public static Geopoint createGeopoint(final String latDir, final String latDeg, final String latMin, final String latMinFrac,
            final String lonDir, final String lonDeg, final String lonMin, final String lonMinFrac) {
        double lat = 0.0d;
        double lon = 0.0d;
        try {
            lat = Double.parseDouble(latDeg) + Double.parseDouble(latMin + "." + addZeros(Integer.parseInt(latMinFrac), 3)) / D60;
            lon = Double.parseDouble(lonDeg) + Double.parseDouble(lonMin + "." + addZeros(Integer.parseInt(lonMinFrac), 3)) / D60;
        } catch (NumberFormatException e) {
        }
        lat *= "S".equalsIgnoreCase(latDir) ? -1 : 1;
        lon *= "W".equalsIgnoreCase(lonDir) ? -1 : 1;
        return new Geopoint(lat, lon);
    }
}