package cgeo.geocaching.connector.gc;

/**
 * Representation of a position inside an UTFGrid
 *
 * @author blafoo
 *
 */
public final class UTFGridPosition {

    public final int x;
    public final int y;

    UTFGridPosition(final int x, final int y) {
        if (x > UTFGrid.GRID_MAXX) {
            throw new IllegalArgumentException("x outside grid");
        }
        if (y > UTFGrid.GRID_MAXY) {
            throw new IllegalArgumentException("y outside grid");
        }
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}
