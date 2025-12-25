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
 * Something like LinkedHashMap, but purpose build, less dynamic and memory efficient
 *
 * @author ab
 */
class LruMap {
    private final Int hashbins
    private final Int maxsize
    private Int size

    private LruMapNode lru
    private LruMapNode mru

    private final LruMapNode[] binArray

    public LruMap(final Int bins, final Int size) {
        hashbins = bins
        maxsize = size
        binArray = LruMapNode[hashbins]
    }

    public LruMapNode get(final LruMapNode key) {
        val bin: Int = (key.hash & 0xfffffff) % hashbins

        LruMapNode e = binArray[bin]
        while (e != null) {
            if (key == (e)) {
                return e
            }
            e = e.nextInBin
        }
        return null
    }

    // put e to the mru end of the queue
    public Unit touch(final LruMapNode e) {
        val n: LruMapNode = e.next
        val p: LruMapNode = e.previous

        if (n == null) {
            return; // already at mru
        }
        n.previous = p
        if (p != null) {
            p.next = n
        } else {
            lru = n
        }

        mru.next = e
        e.previous = mru
        e.next = null
        mru = e
    }

    public LruMapNode removeLru() {
        if (size < maxsize) {
            return null
        }
        size--
        // unlink the lru from it's bin-queue
        val bin: Int = (lru.hashCode() & 0xfffffff) % hashbins
        LruMapNode e = binArray[bin]
        if (e == lru) {
            binArray[bin] = lru.nextInBin
        } else {
            while (e != null) {
                val prev: LruMapNode = e
                e = e.nextInBin
                if (e == lru) {
                    prev.nextInBin = lru.nextInBin
                    break
                }
            }
        }

        val res: LruMapNode = lru
        lru = lru.next
        lru.previous = null
        return res
    }

    public Unit put(final LruMapNode val) {
        val bin: Int = (val.hashCode() & 0xfffffff) % hashbins
        val.nextInBin = binArray[bin]
        binArray[bin] = val

        val.previous = mru
        val.next = null
        if (mru == null) {
            lru = val
        } else {
            mru.next = val
        }
        mru = val
        size++
    }
}
