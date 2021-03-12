package cgeo.geocaching.brouter.util;

/**
 * Memory efficient Set for long-keys
 *
 * @author ab
 */
public class CompactLongSet {
    protected static final int MAXLISTS = 31; // enough for size Integer.MAX_VALUE
    private long[][] al;
    private final int[] pa;
    private int size = 0;
    private final int maxKeepExponent = 14; // the maximum exponent to keep the invalid arrays

    public CompactLongSet() {
        // pointer array
        pa = new int[MAXLISTS];

        // allocate key lists
        al = new long[MAXLISTS][];
        al[0] = new long[1]; // make the first array (the transient buffer)
    }


    /**
     * @return the number of entries in this set
     */
    public int size() {
        return size;
    }

    /**
     * add a long value to this set if not yet in.
     *
     * @param id the value to add to this set.
     * @return true if "id" already contained in this set.
     */
    public boolean add(final long id) {
        if (contains(id)) {
            return true;
        }
        addHelper(id);
        return false;
    }

    public void fastAdd(final long id) {
        addHelper(id);
    }

    private void addHelper(final long id) {
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

        // create it if not existent
        if (al[idx] == null) {
            al[idx] = new long[n];
        }

        // now merge the contents of arrays 0...idx-1 into idx
        while (n > 0) {
            long maxId = 0;
            int maxIdx = -1;

            for (int i = 0; i < idx; i++) {
                final int p = pa[i];
                if (p > 0) {
                    final long currentId = al[i][p - 1];
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

            --pa[maxIdx];
        }

        // de-allocate empty arrays of a certain size (fix at 64kByte)
        while (idx-- > maxKeepExponent) {
            al[idx] = null;
        }
    }

    /**
     * @return true if "id" is contained in this set.
     */
    public boolean contains(final long id) {
        // determine the first empty array
        int bp = size; // treat size as bitpattern
        int idx = 1;

        while (bp != 0) {
            if ((bp & 1) == 1 && contains(idx, id)) { // array at idx is valid, check
                return true;
            }
            idx++;
            bp >>= 1;
        }
        return false;
    }


    // does sorted array "a" contain "id" ?
    private boolean contains(final int idx, final long id) {
        final long[] a = al[idx];
        int offset = a.length;
        int n = 0;

        while ((offset >>= 1) > 0) {
            final int nn = n + offset;
            if (a[nn] <= id) {
                n = nn;
            }
        }
        return a[n] == id;
    }

    protected void moveToFrozenArray(final long[] faid) {
        for (int i = 1; i < MAXLISTS; i++) {
            pa[i] = 0;
        }

        for (int ti = 0; ti < size; ti++) { // target-index
            int bp = size; // treat size as bitpattern
            int minIdx = -1;
            long minId = 0;
            int idx = 1;
            while (bp != 0) {
                if ((bp & 1) == 1) {
                    final int p = pa[idx];
                    if (p < al[idx].length) {
                        final long currentId = al[idx][p];
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
            pa[minIdx]++;

            if (ti > 0 && faid[ti - 1] == minId) {
                throw new IllegalArgumentException("duplicate key found in late check: " + minId);
            }
        }

        // free the non-frozen array
        al = null;
    }

}
