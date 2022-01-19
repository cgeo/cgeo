package cgeo.geocaching.brouter.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Frozen instance of Memory efficient Map
 * <p>
 * This one is readily sorted into a singe array for faster access
 *
 * @author ab
 */
public class FrozenLongMap<V> extends CompactLongMap<V> {
    private final long[] faid;
    private final ArrayList<V> flv;
    private int size = 0;
    private int p2size; // next power of 2 of size

    public FrozenLongMap(final CompactLongMap<V> map) {
        size = map.size();

        faid = new long[size];
        flv = new ArrayList<>(size);

        map.moveToFrozenArrays(faid, flv);

        p2size = 0x40000000;
        while (p2size > size) {
            p2size >>= 1;
        }
    }

    @Override
    public boolean put(final long id, final V value) {
        try {
            valueIn = value;
            if (contains(id, true)) {
                return true;
            }
            throw new RuntimeException("cannot only put on existing key in FrozenLongIntMap");
        } finally {
            valueIn = null;
            valueOut = null;
        }
    }

    @Override
    public void fastPut(final long id, final V value) {
        throw new RuntimeException("cannot put on FrozenLongIntMap");
    }

    /**
     * @return the number of entries in this set
     */
    @Override
    public int size() {
        return size;
    }


    /**
     * @return true if "id" is contained in this set.
     */
    @Override
    protected boolean contains(final long id, final boolean doPut) {
        if (size == 0) {
            return false;
        }
        final long[] a = faid;
        int offset = p2size;
        int n = 0;

        while (offset > 0) {
            final int nn = n + offset;
            if (nn < size && a[nn] <= id) {
                n = nn;
            }
            offset >>= 1;
        }
        if (a[n] == id) {
            valueOut = flv.get(n);
            if (doPut) {
                flv.set(n, valueIn);
            }
            return true;
        }
        return false;
    }

    /**
     * @return the value for "id", or null if key unknown
     */
    @Override
    public V get(final long id) {
        if (size == 0) {
            return null;
        }
        final long[] a = faid;
        int offset = p2size;
        int n = 0;

        while (offset > 0) {
            final int nn = n + offset;
            if (nn < size && a[nn] <= id) {
                n = nn;
            }
            offset >>= 1;
        }
        if (a[n] == id) {
            return flv.get(n);
        }
        return null;
    }

    public List<V> getValueList() {
        return flv;
    }

    public long[] getKeyArray() {
        return faid;
    }
}
