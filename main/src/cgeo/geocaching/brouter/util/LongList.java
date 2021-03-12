package cgeo.geocaching.brouter.util;

/**
 * dynamic list of primitive longs
 *
 * @author ab
 */
public class LongList {
    private long[] a;
    private int size;

    public LongList(final int capacity) {
        a = capacity < 4 ? new long[4] : new long[capacity];
    }

    public void add(final long value) {
        if (size == a.length) {
            final long[] aa = new long[2 * size];
            System.arraycopy(a, 0, aa, 0, size);
            a = aa;
        }
        a[size++] = value;
    }

    public long get(final int idx) {
        if (idx >= size) {
            throw new IndexOutOfBoundsException("list size=" + size + " idx=" + idx);
        }
        return a[idx];
    }

    public int size() {
        return size;
    }

}
