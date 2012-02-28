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

    public UTFGridPosition(final int x, final int y) {
        assert x >= 0 && x <= UTFGrid.GRID_MAXX : "x outside bounds";
        assert y >= 0 && y <= UTFGrid.GRID_MAXY : "y outside bounds";

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
