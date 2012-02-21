package cgeo.geocaching.connector.gc;

/**
 *
 * @author blafoo
 *
 * @see https://github.com/mapbox/mbtiles-spec/blob/master/1.1/utfgrid.md
 *
 */
public final class UTFGrid {

    public static final int GRID_MAXX = 63;
    public static final int GRID_MAXY = 63;

    /** Convert a value from a JSON grid object into an id that can be used as an index */
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

}
