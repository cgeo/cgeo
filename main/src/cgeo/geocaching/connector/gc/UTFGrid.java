package cgeo.geocaching.connector.gc;

import java.util.List;

/**
 *
 * @see <a href="https://github.com/mapbox/mbtiles-spec/blob/master/1.1/utfgrid.md">Mapbox</a>
 *
 */
final class UTFGrid {

    static final int GRID_MAXX = 63;
    static final int GRID_MAXY = 63;

    /** Calculate from a list of positions (x/y) the coords */
    static UTFGridPosition getPositionInGrid(final List<UTFGridPosition> positions) {
        int minX = GRID_MAXX;
        int maxX = 0;
        int minY = GRID_MAXY;
        int maxY = 0;
        for (final UTFGridPosition pos : positions) {
            minX = Math.min(minX, pos.x);
            maxX = Math.max(maxX, pos.x);
            minY = Math.min(minY, pos.y);
            maxY = Math.max(maxY, pos.y);
        }
        return new UTFGridPosition((minX + maxX) / 2, (minY + maxY) / 2);
    }

}
