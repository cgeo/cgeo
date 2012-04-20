package cgeo.geocaching.geopoint.direction;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

public class Direction {
    /* Constant values needed for calculation */
    static final double D60 = 60.0d;
    private static final double D1000 = 1000.0d;
    static final double D3600 = 3600.0d;
    static final BigDecimal BD_SIXTY = BigDecimal.valueOf(D60);
    static final BigDecimal BD_THOUSAND = BigDecimal.valueOf(D1000);
    static final BigDecimal BD_ONEHOUNDREDTHOUSAND = BigDecimal.valueOf(100000.0d);

    /** latitude direction, 'N' or 'S' */
    public final char latDir;
    /** longitude direction, 'E' or 'W' */
    public final char lonDir;

    public Direction(final double latSigned, final double lonSigned) {
        latDir = latSigned < 0 ? 'S' : 'N';
        lonDir = lonSigned < 0 ? 'W' : 'E';
    }

    protected static String addZeros(final int value, final int len) {
        return StringUtils.leftPad(Integer.toString(value), len, '0');
    }
}