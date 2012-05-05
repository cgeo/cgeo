package cgeo.geocaching.connector.gc;

import java.util.List;

/**
 * 
 * @author blafoo
 * 
 * @see <a href="https://github.com/mapbox/mbtiles-spec/blob/master/1.1/utfgrid.md">Mapbox</a>
 * 
 */
public final class UTFGrid {

    public static final int GRID_MAXX = 63;
    public static final int GRID_MAXY = 63;

    /**
     * Convert a value from a JSON grid object into an id that can be used as an index
     * It's not used at the moment due to optimizations.
     * But maybe we need it some day...
     */
    public static short getUTFGridId(final char value) {
        short result = (short) value;
        if (result >= 93) {
            result--;
        }
        if (result >= 35) {
            result--;
        }
        return (short) (result - 32);
    }

    /** Calculate from a list of positions (x/y) the coords */
    protected static UTFGridPosition getPositionInGrid(List<UTFGridPosition> positions) {
        int minX = GRID_MAXX;
        int maxX = 0;
        int minY = GRID_MAXY;
        int maxY = 0;
        for (UTFGridPosition pos : positions) {
            minX = Math.min(minX, pos.x);
            maxX = Math.max(maxX, pos.x);
            minY = Math.min(minY, pos.y);
            maxY = Math.max(maxY, pos.y);
        }
        return new UTFGridPosition((minX + maxX) / 2, (minY + maxY) / 2);
    }

}
