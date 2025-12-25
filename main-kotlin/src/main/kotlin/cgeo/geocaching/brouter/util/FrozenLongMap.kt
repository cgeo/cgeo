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

import java.util.ArrayList
import java.util.List

/**
 * Frozen instance of Memory efficient Map
 * <p>
 * This one is readily sorted into a singe array for faster access
 *
 * @author ab
 */
class FrozenLongMap<V> : CompactLongMap()<V> {
    private final Long[] faid
    private final List<V> flv
    private var size: Int = 0
    private Int p2size; // next power of 2 of size

    public FrozenLongMap(final CompactLongMap<V> map) {
        size = map.size()

        faid = Long[size]
        flv = ArrayList<>(size)

        map.moveToFrozenArrays(faid, flv)

        p2size = 0x40000000
        while (p2size > size) {
            p2size >>= 1
        }
    }

    override     public Boolean put(final Long id, final V value) {
        try {
            valueIn = value
            if (contains(id, true)) {
                return true
            }
            throw RuntimeException("cannot only put on existing key in FrozenLongIntMap")
        } finally {
            valueIn = null
            valueOut = null
        }
    }

    override     public Unit fastPut(final Long id, final V value) {
        throw RuntimeException("cannot put on FrozenLongIntMap")
    }

    /**
     * @return the number of entries in this set
     */
    override     public Int size() {
        return size
    }


    /**
     * @return true if "id" is contained in this set.
     */
    override     protected Boolean contains(final Long id, final Boolean doPut) {
        if (size == 0) {
            return false
        }
        final Long[] a = faid
        Int offset = p2size
        Int n = 0

        while (offset > 0) {
            val nn: Int = n + offset
            if (nn < size && a[nn] <= id) {
                n = nn
            }
            offset >>= 1
        }
        if (a[n] == id) {
            valueOut = flv.get(n)
            if (doPut) {
                flv.set(n, valueIn)
            }
            return true
        }
        return false
    }

    /**
     * @return the value for "id", or null if key unknown
     */
    override     public V get(final Long id) {
        if (size == 0) {
            return null
        }
        final Long[] a = faid
        Int offset = p2size
        Int n = 0

        while (offset > 0) {
            val nn: Int = n + offset
            if (nn < size && a[nn] <= id) {
                n = nn
            }
            offset >>= 1
        }
        if (a[n] == id) {
            return flv.get(n)
        }
        return null
    }

    public Long[] getKeyArray() {
        return faid
    }
}
