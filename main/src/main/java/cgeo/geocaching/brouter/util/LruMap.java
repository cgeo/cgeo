package cgeo.geocaching.brouter.util;

/**
 * Something like LinkedHashMap, but purpose build, less dynamic and memory efficient
 *
 * @author ab
 */
public final class LruMap {
    private final int hashbins;
    private final int maxsize;
    private int size;

    private LruMapNode lru;
    private LruMapNode mru;

    private final LruMapNode[] binArray;

    public LruMap(final int bins, final int size) {
        hashbins = bins;
        maxsize = size;
        binArray = new LruMapNode[hashbins];
    }

    public LruMapNode get(final LruMapNode key) {
        final int bin = (key.hash & 0xfffffff) % hashbins;

        LruMapNode e = binArray[bin];
        while (e != null) {
            if (key.equals(e)) {
                return e;
            }
            e = e.nextInBin;
        }
        return null;
    }

    // put e to the mru end of the queue
    public void touch(final LruMapNode e) {
        final LruMapNode n = e.next;
        final LruMapNode p = e.previous;

        if (n == null) {
            return; // already at mru
        }
        n.previous = p;
        if (p != null) {
            p.next = n;
        } else {
            lru = n;
        }

        mru.next = e;
        e.previous = mru;
        e.next = null;
        mru = e;
    }

    public LruMapNode removeLru() {
        if (size < maxsize) {
            return null;
        }
        size--;
        // unlink the lru from it's bin-queue
        final int bin = (lru.hashCode() & 0xfffffff) % hashbins;
        LruMapNode e = binArray[bin];
        if (e == lru) {
            binArray[bin] = lru.nextInBin;
        } else {
            while (e != null) {
                final LruMapNode prev = e;
                e = e.nextInBin;
                if (e == lru) {
                    prev.nextInBin = lru.nextInBin;
                    break;
                }
            }
        }

        final LruMapNode res = lru;
        lru = lru.next;
        lru.previous = null;
        return res;
    }

    public void put(final LruMapNode val) {
        final int bin = (val.hashCode() & 0xfffffff) % hashbins;
        val.nextInBin = binArray[bin];
        binArray[bin] = val;

        val.previous = mru;
        val.next = null;
        if (mru == null) {
            lru = val;
        } else {
            mru.next = val;
        }
        mru = val;
        size++;
    }
}
