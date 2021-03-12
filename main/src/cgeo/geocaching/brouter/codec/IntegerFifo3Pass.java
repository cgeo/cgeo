package cgeo.geocaching.brouter.codec;

/**
 * Special integer fifo suitable for 3-pass encoding
 */
public class IntegerFifo3Pass {
    private int[] a;
    private int size;
    private int pos;

    private int pass;

    public IntegerFifo3Pass(final int capacity) {
        a = capacity < 4 ? new int[4] : new int[capacity];
    }

    /**
     * Starts a new encoding pass and resets the reading pointer
     * from the stats collected in pass2 and writes that to the given context
     */
    public void init() {
        pass++;
        pos = 0;
    }

    /**
     * writes to the fifo in pass2
     */
    public void add(final int value) {
        if (pass == 2) {
            if (size == a.length) {
                final int[] aa = new int[2 * size];
                System.arraycopy(a, 0, aa, 0, size);
                a = aa;
            }
            a[size++] = value;
        }
    }

    /**
     * reads from the fifo in pass3 (in pass1/2 returns just 1)
     */
    public int getNext() {
        return pass == 3 ? get(pos++) : 1;
    }

    private int get(final int idx) {
        if (idx >= size) {
            throw new IndexOutOfBoundsException("list size=" + size + " idx=" + idx);
        }
        return a[idx];
    }
}
