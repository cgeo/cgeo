package cgeo.geocaching.connector.gc;

/**
 *
 * @author blafoo
 *
 * @see https://github.com/mapbox/mbtiles-spec/blob/master/1.1/utfgrid.md
 * 
 */
public class UTFGrid {

    /** Convert a value from a JSON grid object into an id that can be used as an index */
    public static byte getUTFGridId(final byte value) {
        byte result = value;
        if (result >= 93) {
            result--;
        }
        if (result >= 35) {
            result--;
        }
        return (byte) (result - 32);
    }

}
