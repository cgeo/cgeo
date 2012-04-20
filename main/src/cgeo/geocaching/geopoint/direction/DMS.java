package cgeo.geocaching.geopoint.direction;

import cgeo.geocaching.geopoint.Geopoint;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DMS extends Direction {

    public final int latDeg;
    public final int latMin;
    public final double latSecRaw;
    public final int latSec;
    public final int latSecFrac;

    public final int lonDeg;
    public final int lonMin;
    public final double lonSecRaw;
    public final int lonSec;
    public final int lonSecFrac;

    public DMS(final double latSigned, final double lonSigned) {
        super(latSigned, lonSigned);
        BigDecimal bdLat = BigDecimal.valueOf(latSigned).abs();
        latDeg = bdLat.intValue();
        BigDecimal bdLatMin = bdLat.subtract(BigDecimal.valueOf(latDeg)).multiply(BD_SIXTY);
        latMin = bdLatMin.intValue();
        BigDecimal bdLatSec = bdLatMin.subtract(BigDecimal.valueOf(latMin)).multiply(BD_SIXTY);
        // Rounding here ...
        bdLatSec = bdLatSec.setScale(3, RoundingMode.HALF_UP);
        latSecRaw = bdLatSec.doubleValue();
        latSec = bdLatSec.intValue();
        BigDecimal bdLatSecFrac = bdLatSec.subtract(BigDecimal.valueOf(latSec)).multiply(BD_THOUSAND);
        latSecFrac = bdLatSecFrac.setScale(0, RoundingMode.HALF_UP).intValue();

        BigDecimal bdlon = BigDecimal.valueOf(lonSigned).abs();
        lonDeg = bdlon.intValue();
        BigDecimal bdLonMin = bdlon.subtract(BigDecimal.valueOf(lonDeg)).multiply(BD_SIXTY);
        lonMin = bdLonMin.intValue();
        BigDecimal bdLonSec = bdLonMin.subtract(BigDecimal.valueOf(lonMin)).multiply(BD_SIXTY);
        // Rounding here ...
        bdLonSec = bdLonSec.setScale(3, RoundingMode.HALF_UP);
        lonSecRaw = bdLonSec.doubleValue();
        lonSec = bdLonSec.intValue();
        BigDecimal bdLonSecFrac = bdLonSec.subtract(BigDecimal.valueOf(lonSec)).multiply(BD_THOUSAND);
        lonSecFrac = bdLonSecFrac.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public static Geopoint createGeopoint(final String latDir, final String latDeg, final String latMin, final String latSec, final String latSecFrac,
            final String lonDir, final String lonDeg, final String lonMin, final String lonSec, final String lonSecFrac) {
        double lat = 0.0d;
        double lon = 0.0d;
        try {
            lat = Double.parseDouble(latDeg) + Double.parseDouble(latMin) / D60 + Double.parseDouble(latSec + "." + addZeros(Integer.parseInt(latSecFrac), 3)) / D3600;
            lon = Double.parseDouble(lonDeg) + Double.parseDouble(lonMin) / D60 + Double.parseDouble(lonSec + "." + addZeros(Integer.parseInt(lonSecFrac), 3)) / D3600;
        } catch (NumberFormatException e) {
        }
        lat *= "S".equalsIgnoreCase(latDir) ? -1 : 1;
        lon *= "W".equalsIgnoreCase(lonDir) ? -1 : 1;
        return new Geopoint(lat, lon);
    }
}