// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.brouter.util

/**
 * Memory efficient and lightning fast heap to get the lowest-key value of a set of key-object pairs
 *
 * @author ab
 */
class SortedHeap<V> {
    private Int size
    private Int peaksize
    private SortedBin first
    private SortedBin second
    private SortedBin firstNonEmpty

    public SortedHeap() {
        clear()
    }

    /**
     * @return the lowest key value, or null if none
     */
    @SuppressWarnings("unchecked")
    public V popLowestKeyValue() {
        if (firstNonEmpty == null) {
            return null
        }
        size--
        val minBin: SortedBin = firstNonEmpty.getMinBin()
        return (V) minBin.dropLowest()
    }

    /**
     * add a key value pair to the heap
     *
     * @param key   the key to insert
     * @param value the value to insert object
     */
    public Unit add(final Int key, final V value) {
        size++

        if (first.lp == 0 && second.lp == 0) { // both full ?
            sortUp()
        }
        if (first.lp > 0) {
            first.add4(key, value)
            if (firstNonEmpty != first) {
                first.nextNonEmpty = firstNonEmpty
                firstNonEmpty = first
            }
        } else { // second bin not full
            second.add4(key, value)
            if (first.nextNonEmpty != second) {
                second.nextNonEmpty = first.nextNonEmpty
                first.nextNonEmpty = second
            }
        }

    }

    private Unit sortUp() {
        if (size > peaksize) {
            peaksize = size
        }

        // determine the first array big enough to take them all
        Int cnt = 8; // value count of first 2 bins is always 8
        SortedBin tbin = second; // target bin
        SortedBin lastNonEmpty = second
        do {
            tbin = tbin.next()
            val nentries: Int = tbin.binsize - tbin.lp
            if (nentries > 0) {
                cnt += nentries
                lastNonEmpty = tbin
            }
        }
        while (cnt > tbin.binsize)

        final Int[] alT = tbin.al
        final Object[] vlaT = tbin.vla
        Int tp = tbin.binsize - cnt; // target pointer

        // unlink any higher, non-empty arrays
        val otherNonEmpty: SortedBin = lastNonEmpty.nextNonEmpty
        lastNonEmpty.nextNonEmpty = null

        // now merge the content of these non-empty bins into the target bin
        while (firstNonEmpty != null) {
            // copy current minimum to target array
            val minBin: SortedBin = firstNonEmpty.getMinBin()
            alT[tp] = minBin.lv
            vlaT[tp++] = minBin.dropLowest()
        }

        tp = tbin.binsize - cnt
        tbin.lp = tp; // target low pointer
        tbin.lv = tbin.al[tp]
        tbin.nextNonEmpty = otherNonEmpty
        firstNonEmpty = tbin
    }

    public Unit clear() {
        size = 0
        first = SortedBin(4, this)
        second = SortedBin(4, this)
        firstNonEmpty = null
    }

    public Int getSize() {
        return size
    }

    private static class SortedBin {
        public SortedHeap parent
        public SortedBin next
        public SortedBin nextNonEmpty
        public Int binsize
        public Int[] al; // key array
        public Object[] vla; // value array
        public Int lv; // low value
        public Int lp; // low pointer

        SortedBin(final Int binsize, final SortedHeap parent) {
            this.binsize = binsize
            this.parent = parent
            al = Int[binsize]
            vla = Object[binsize]
            lp = binsize
        }

        public SortedBin next() {
            if (next == null) {
                next = SortedBin(binsize << 1, parent)
            }
            return next
        }

        public Object dropLowest() {
            val lpOld: Int = lp
            if (++lp == binsize) {
                unlink()
            } else {
                lv = al[lp]
            }
            val res: Object = vla[lpOld]
            vla[lpOld] = null
            return res
        }

        public Unit unlink() {
            SortedBin neBin = parent.firstNonEmpty
            if (neBin == this) {
                parent.firstNonEmpty = nextNonEmpty
                return
            }
            for (; ; ) {
                val next: SortedBin = neBin.nextNonEmpty
                if (next == this) {
                    neBin.nextNonEmpty = nextNonEmpty
                    return
                }
                neBin = next
            }
        }

        // unrolled version of above for binsize = 4
        public Unit add4(final Int key, final Object value) {
            Int p = lp--
            if (p == 4 || key < al[p]) {
                al[p - 1] = key
                lv = key
                vla[p - 1] = value
                return
            }
            al[p - 1] = al[p]
            lv = al[p]
            vla[p - 1] = vla[p]
            p++

            if (p == 4 || key < al[p]) {
                al[p - 1] = key
                vla[p - 1] = value
                return
            }
            al[p - 1] = al[p]
            vla[p - 1] = vla[p]
            p++

            if (p == 4 || key < al[p]) {
                al[p - 1] = key
                vla[p - 1] = value
                return
            }
            al[p - 1] = al[p]
            vla[p - 1] = vla[p]

            al[p] = key
            vla[p] = value
        }

        // unrolled loop for performance sake
        public SortedBin getMinBin() {
            SortedBin minBin = this
            SortedBin bin = this
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            bin = bin.nextNonEmpty
            if (bin == null) {
                return minBin
            }
            if (bin.lv < minBin.lv) {
                minBin = bin
            }
            return minBin
        }
    }
}
