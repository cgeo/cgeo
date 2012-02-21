package cgeo.geocaching.connector.gc;

import cgeo.geocaching.Settings;

import android.util.Log;

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
            Log.e(Settings.tag, "x outside grid");
            throw new IllegalArgumentException("x outside grid");
        }
        if (y > UTFGrid.GRID_MAXY) {
            Log.e(Settings.tag, "y outside grid");
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
