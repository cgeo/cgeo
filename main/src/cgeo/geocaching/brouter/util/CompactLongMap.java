package cgeo.geocaching.brouter.util;

import java.util.ArrayList;

/**
 * Memory efficient Map to map a long-key to an object-value
 * <p>
 * Implementation is such that basically the 12 bytes
 * per entry is allocated that's needed to store
 * a long- and an object-value.
 * This class does not implement the Map interface
 * because it's not complete (remove() is not implemented,
 * CompactLongMap can only grow.)
 *
 * @author ab
 */
public class CompactLongMap<V> {
    protected static final int MAXLISTS = 31; // enough for size Integer.MAX_VALUE
    private static boolean earlyDuplicateCheck;
    protected V value_in;
    protected V value_out;
    private long[][] al;
    private final int[] pa;
    private int size = 0;
    private final int _maxKeepExponent = 14; // the maximum exponent to keep the invalid arrays
    private Object[][] vla; // value list array


    /*
     *
     * The Map extension:
     * next 5 protected methods are needed to implement value-support
     * overwrite them all to support value structures other than the
     * long-values implemented here as a sample.
     *
     * Furthermore, put() and get() method need to be implemented
     * to access the values.
     *
     * Note that this map does not behave exactly like java.util.Map
     * - put(..) with already existing key throws exception
     * - get(..) with non-existing key thros exception
     *
     * If you have keys that cannot easily be mapped on long's, use
     * a hash-function to do the mapping. But note that, in comparison
     * to java.util.HashMap, in that case the keys itself are not saved,
     * only the hash-values, so you need to be sure that random duplicate
     * hashs are either excluded by the structure of your data or that
     * you can handle the possible IllegalArgumentException
     *
     */

    public CompactLongMap() {
        // pointer array
        pa = new int[MAXLISTS];

        // allocate key lists
        al = new long[MAXLISTS][];
        al[0] = new long[1]; // make the first array (the transient buffer)

        // same for the values
        vla = new Object[MAXLISTS][];
        vla[0] = new Object[1];

        earlyDuplicateCheck = Boolean.getBoolean("earlyDuplicateCheck");
    }

    public boolean put(long id, V value) {
        try {
            value_in = value;
            if (contains(id, true)) {
                return true;
            }
            vla[0][0] = value;
            _add(id);
            return false;
        } finally {
            value_in = null;
            value_out = null;
        }
    }

    /**
     * Same as put( id, value ) but duplicate check
     * is skipped for performance. Be aware that you
     * can get a duplicate exception later on if the
     * map is restructured!
     * with System parameter earlyDuplicateCheck=true you
     * can enforce the early duplicate check for debugging
     *
     * @param id    the key to insert
     * @param value the value to insert object
     * @throws IllegalArgumentException for duplicates if enabled
     */
    public void fastPut(long id, V value) {
        if (earlyDuplicateCheck && contains(id)) {
            throw new IllegalArgumentException("duplicate key found in early check: " + id);
        }
        vla[0][0] = value;
        _add(id);
    }

    /**
     * Get the value for the given id
     *
     * @param id the key to query
     * @return the object, or null if id not known
     */
    public V get(long id) {
        try {
            if (contains(id, false)) {
                return value_out;
            }
            return null;
        } finally {
            value_out = null;
        }
    }


    /**
     * @return the number of entries in this map
     */
    public int size() {
        return size;
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
            vla[idx] = new Object[n];
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

    /**
     * @return true if "id" is contained in this set.
     */
    public boolean contains(long id) {
        try {
            return contains(id, false);
        } finally {
            value_out = null;
        }
    }

    protected boolean contains(long id, boolean doPut) {
        // determine the first empty array
        int bp = size; // treat size as bitpattern
        int idx = 1;

        while (bp != 0) {
            if ((bp & 1) == 1) {
                // array at idx is valid, check
                if (contains(idx, id, doPut)) {
                    return true;
                }
            }
            idx++;
            bp >>= 1;
        }
        return false;
    }


    // does sorted array "a" contain "id" ?
    private boolean contains(int idx, long id, boolean doPut) {
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
            value_out = (V) vla[idx][n];
            if (doPut)
                vla[idx][n] = value_in;
            return true;
        }
        return false;
    }

    protected void moveToFrozenArrays(long[] faid, ArrayList<V> flv) {
        for (int i = 1; i < MAXLISTS; i++) {
            pa[i] = 0;
        }

        for (int ti = 0; ti < size; ti++) // target-index
        {
            int bp = size; // treat size as bitpattern
            int minIdx = -1;
            long minId = 0;
            int idx = 1;
            while (bp != 0) {
                if ((bp & 1) == 1) {
                    int p = pa[idx];
                    if (p < al[idx].length) {
                        long currentId = al[idx][p];
                        if (minIdx < 0 || currentId < minId) {
                            minIdx = idx;
                            minId = currentId;
                        }
                    }
                }
                idx++;
                bp >>= 1;
            }
            faid[ti] = minId;
            flv.add((V) vla[minIdx][pa[minIdx]]);
            pa[minIdx]++;

            if (ti > 0 && faid[ti - 1] == minId) {
                throw new IllegalArgumentException("duplicate key found in late check: " + minId);
            }
        }

        // free the non-frozen arrays
        al = null;
        vla = null;
    }

}
