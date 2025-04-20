package cgeo.geocaching.brouter.util;

/**
 * Memory efficient and lightning fast heap to get the lowest-key value of a set of key-object pairs
 *
 * @author ab
 */
public final class SortedHeap<V> {
    private int size;
    private int peaksize;
    private SortedBin first;
    private SortedBin second;
    private SortedBin firstNonEmpty;

    public SortedHeap() {
        clear();
    }

    /**
     * @return the lowest key value, or null if none
     */
    @SuppressWarnings("unchecked")
    public V popLowestKeyValue() {
        if (firstNonEmpty == null) {
            return null;
        }
        size--;
        final SortedBin minBin = firstNonEmpty.getMinBin();
        return (V) minBin.dropLowest();
    }

    /**
     * add a key value pair to the heap
     *
     * @param key   the key to insert
     * @param value the value to insert object
     */
    public void add(final int key, final V value) {
        size++;

        if (first.lp == 0 && second.lp == 0) { // both full ?
            sortUp();
        }
        if (first.lp > 0) {
            first.add4(key, value);
            if (firstNonEmpty != first) {
                first.nextNonEmpty = firstNonEmpty;
                firstNonEmpty = first;
            }
        } else { // second bin not full
            second.add4(key, value);
            if (first.nextNonEmpty != second) {
                second.nextNonEmpty = first.nextNonEmpty;
                first.nextNonEmpty = second;
            }
        }

    }

    private void sortUp() {
        if (size > peaksize) {
            peaksize = size;
        }

        // determine the first array big enough to take them all
        int cnt = 8; // value count of first 2 bins is always 8
        SortedBin tbin = second; // target bin
        SortedBin lastNonEmpty = second;
        do {
            tbin = tbin.next();
            final int nentries = tbin.binsize - tbin.lp;
            if (nentries > 0) {
                cnt += nentries;
                lastNonEmpty = tbin;
            }
        }
        while (cnt > tbin.binsize);

        final int[] alT = tbin.al;
        final Object[] vlaT = tbin.vla;
        int tp = tbin.binsize - cnt; // target pointer

        // unlink any higher, non-empty arrays
        final SortedBin otherNonEmpty = lastNonEmpty.nextNonEmpty;
        lastNonEmpty.nextNonEmpty = null;

        // now merge the content of these non-empty bins into the target bin
        while (firstNonEmpty != null) {
            // copy current minimum to target array
            final SortedBin minBin = firstNonEmpty.getMinBin();
            alT[tp] = minBin.lv;
            vlaT[tp++] = minBin.dropLowest();
        }

        tp = tbin.binsize - cnt;
        tbin.lp = tp; // new target low pointer
        tbin.lv = tbin.al[tp];
        tbin.nextNonEmpty = otherNonEmpty;
        firstNonEmpty = tbin;
    }

    public void clear() {
        size = 0;
        first = new SortedBin(4, this);
        second = new SortedBin(4, this);
        firstNonEmpty = null;
    }

    public int getSize() {
        return size;
    }

    private static final class SortedBin {
        public SortedHeap parent;
        public SortedBin next;
        public SortedBin nextNonEmpty;
        public int binsize;
        public int[] al; // key array
        public Object[] vla; // value array
        public int lv; // low value
        public int lp; // low pointer

        SortedBin(final int binsize, final SortedHeap parent) {
            this.binsize = binsize;
            this.parent = parent;
            al = new int[binsize];
            vla = new Object[binsize];
            lp = binsize;
        }

        public SortedBin next() {
            if (next == null) {
                next = new SortedBin(binsize << 1, parent);
            }
            return next;
        }

        public Object dropLowest() {
            final int lpOld = lp;
            if (++lp == binsize) {
                unlink();
            } else {
                lv = al[lp];
            }
            final Object res = vla[lpOld];
            vla[lpOld] = null;
            return res;
        }

        public void unlink() {
            SortedBin neBin = parent.firstNonEmpty;
            if (neBin == this) {
                parent.firstNonEmpty = nextNonEmpty;
                return;
            }
            for (; ; ) {
                final SortedBin next = neBin.nextNonEmpty;
                if (next == this) {
                    neBin.nextNonEmpty = nextNonEmpty;
                    return;
                }
                neBin = next;
            }
        }

        // unrolled version of above for binsize = 4
        public void add4(final int key, final Object value) {
            int p = lp--;
            if (p == 4 || key < al[p]) {
                al[p - 1] = key;
                lv = key;
                vla[p - 1] = value;
                return;
            }
            al[p - 1] = al[p];
            lv = al[p];
            vla[p - 1] = vla[p];
            p++;

            if (p == 4 || key < al[p]) {
                al[p - 1] = key;
                vla[p - 1] = value;
                return;
            }
            al[p - 1] = al[p];
            vla[p - 1] = vla[p];
            p++;

            if (p == 4 || key < al[p]) {
                al[p - 1] = key;
                vla[p - 1] = value;
                return;
            }
            al[p - 1] = al[p];
            vla[p - 1] = vla[p];

            al[p] = key;
            vla[p] = value;
        }

        // unrolled loop for performance sake
        public SortedBin getMinBin() {
            SortedBin minBin = this;
            SortedBin bin = this;
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            bin = bin.nextNonEmpty;
            if (bin == null) {
                return minBin;
            }
            if (bin.lv < minBin.lv) {
                minBin = bin;
            }
            return minBin;
        }
    }
}
