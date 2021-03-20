package cgeo.geocaching.brouter.util;

/**
 * TinyDenseLongMap implements the DenseLongMap interface
 * but actually is made for a medium count of non-dense keys
 * <p>
 * It's used as a replacement for DenseLongMap where we
 * have limited memory and far less keys than maykey
 *
 * @author ab
 */
public class TinyDenseLongMap extends DenseLongMap {
    protected static final int MAXLISTS = 31; // enough for size Integer.MAX_VALUE
    private final long[][] al;
    private final int[] pa;
    private int size = 0;
    private final int _maxKeepExponent = 14; // the maximum exponent to keep the invalid arrays
    private final byte[][] vla; // value list array


    public TinyDenseLongMap() {
        super();

        // pointer array
        pa = new int[MAXLISTS];

        // allocate key lists
        al = new long[MAXLISTS][];
        al[0] = new long[1]; // make the first array (the transient buffer)

        // same for the values
        vla = new byte[MAXLISTS][];
        vla[0] = new byte[1];
    }

    private void fillReturnValue(byte[] rv, int idx, int p) {
        rv[0] = vla[idx][p];
        if (rv.length == 2) {
            vla[idx][p] = rv[1];
        }
    }

    @Override
    public void put(long id, int value) {
        byte[] rv = new byte[2];
        rv[1] = (byte) value;
        if (contains(id, rv)) {
            return;
        }

        vla[0][0] = (byte) value;
        _add(id);
    }


    /**
     * Get the byte for the given id
     *
     * @param id the key to query
     * @return the object
     * @throws IllegalArgumentException if id is unknown
     */
    @Override
    public int getInt(long id) {
        byte[] rv = new byte[1];
        if (contains(id, rv)) {
            return rv[0];
        }
        return -1;
    }


    private boolean _add(long id) {
        if (size == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("cannot grow beyond size Integer.MAX_VALUE");
        }

        // put the new entry in the first array
        al[0][0] = id;

        // determine the first empty array
        int bp = size++; // treat size as bitpattern
        int idx = 1;
        int n = 1;

        pa[0] = 1;
        pa[1] = 1;

        while ((bp & 1) == 1) {
            bp >>= 1;
            pa[idx++] = n;
            n <<= 1;
        }

        // create it if not existant
        if (al[idx] == null) {
            al[idx] = new long[n];
            vla[idx] = new byte[n];
        }

        // now merge the contents of arrays 0...idx-1 into idx
        while (n > 0) {
            long maxId = 0;
            int maxIdx = -1;

            for (int i = 0; i < idx; i++) {
                int p = pa[i];
                if (p > 0) {
                    long currentId = al[i][p - 1];
                    if (maxIdx < 0 || currentId > maxId) {
                        maxIdx = i;
                        maxId = currentId;
                    }
                }
            }

            // current maximum found, copy to target array
            if (n < al[idx].length && maxId == al[idx][n]) {
                throw new IllegalArgumentException("duplicate key found in late check: " + maxId);
            }
            --n;
            al[idx][n] = maxId;
            vla[idx][n] = vla[maxIdx][pa[maxIdx] - 1];

            --pa[maxIdx];
        }

        // de-allocate empty arrays of a certain size (fix at 64kByte)
        while (idx-- > _maxKeepExponent) {
            al[idx] = null;
            vla[idx] = null;
        }

        return false;
    }


    private boolean contains(long id, byte[] rv) {
        // determine the first empty array
        int bp = size; // treat size as bitpattern
        int idx = 1;

        while (bp != 0) {
            if ((bp & 1) == 1) {
                // array at idx is valid, check
                if (contains(idx, id, rv)) {
                    return true;
                }
            }
            idx++;
            bp >>= 1;
        }
        return false;
    }


    // does sorted array "a" contain "id" ?
    private boolean contains(int idx, long id, byte[] rv) {
        long[] a = al[idx];
        int offset = a.length;
        int n = 0;

        while ((offset >>= 1) > 0) {
            int nn = n + offset;
            if (a[nn] <= id) {
                n = nn;
            }
        }
        if (a[n] == id) {
            if (rv != null) {
                fillReturnValue(rv, idx, n);
            }
            return true;
        }
        return false;
    }

}
