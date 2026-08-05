package cgeo.geocaching.brouter.codec;

import cgeo.geocaching.brouter.util.ByteDataWriter;

/**
 * a micro-cache is a data cache for an area of some square kilometers or some
 * hundreds or thousands nodes
 * <p>
 * This is the basic io-unit: always a full microcache is loaded from the
 * data-file if a node is requested at a position not yet covered by the caches
 * already loaded
 * <p>
 * The nodes are represented in a compact way (typical 20-50 bytes per node),
 * but in a way that they do not depend on each other, and garbage collection is
 * supported to remove the nodes already consumed from the cache.
 * <p>
 * The cache-internal data representation is different from that in the
 * data-files, where a cache is encoded as a whole, allowing more
 * redundancy-removal for a more compact encoding
 */
public class MicroCache extends ByteDataWriter {
    public static final MicroCache emptyNonVirgin = new MicroCache(null);
    public static boolean debug = false;

    static {
        emptyNonVirgin.virgin = false;
    }

    // cache control: a virgin cache can be
    // put to ghost state for later recovery
    public boolean virgin = true;
    public boolean ghost = false;
    protected int[] faid;
    protected int[] fapos;
    protected int size = 0;
    private int delcount = 0;
    private int delbytes = 0;
    private int p2size; // next power of 2 of size

    protected MicroCache(final byte[] ab) {
        super(ab);
    }

    public static MicroCache emptyCache() {
        return new MicroCache(null); // TODO: singleton?
    }

    protected void init(final int size) {
        this.size = size;
        delcount = 0;
        delbytes = 0;
        p2size = 0x40000000;
        while (p2size > size) {
            p2size >>= 1;
        }
    }

    public final int getSize() {
        return size;
    }

    public final int getDataSize() {
        return ab == null ? 0 : ab.length;
    }

    /**
     * Set the internal reader (aboffset, aboffsetEnd) to the body data for the given id
     * <p>
     * If a node is not found in an empty cache, this is usually an edge-effect
     * (data-file does not exist or neighboured data-files of differnt age),
     * but is can as well be a symptom of a node-identity breaking bug.
     * <p>
     * Current implementation always returns false for not-found, however, for
     * regression testing, at least for the case that is most likely a bug
     * (node found but marked as deleted = ready for garbage collection
     * = already consumed) the RunException should be re-enabled
     *
     * @return true if id was found
     */
    public final boolean getAndClear(final long id64) {
        if (size == 0) {
            return false;
        }
        final int id = shrinkId(id64);
        final int[] a = faid;
        int offset = p2size;
        int n = 0;

        while (offset > 0) {
            final int nn = n + offset;
            if (nn < size && a[nn] <= id) {
                n = nn;
            }
            offset >>= 1;
        }
        if (a[n] == id && (fapos[n] & 0x80000000) == 0) {
            aboffset = startPos(n);
            aboffsetEnd = fapos[n];
            fapos[n] |= 0x80000000; // mark deleted
            delbytes += aboffsetEnd - aboffset;
            delcount++;
            return true;
        }
        return false;
    }

    protected final int startPos(final int n) {
        return n > 0 ? fapos[n - 1] & 0x7fffffff : 0;
    }

    public final int collect(final int threshold) {
        if (delcount <= threshold) {
            return 0;
        }

        virgin = false;

        final int nsize = size - delcount;
        if (nsize == 0) {
            faid = null;
            fapos = null;
        } else {
            final int[] nfaid = new int[nsize];
            final int[] nfapos = new int[nsize];
            int idx = 0;

            final byte[] nab = new byte[ab.length - delbytes];
            int nabOff = 0;
            for (int i = 0; i < size; i++) {
                final int pos = fapos[i];
                if ((pos & 0x80000000) == 0) {
                    final int start = startPos(i);
                    final int end = fapos[i];
                    final int len = end - start;
                    System.arraycopy(ab, start, nab, nabOff, len);
                    nfaid[idx] = faid[i];
                    nabOff += len;
                    nfapos[idx] = nabOff;
                    idx++;
                }
            }
            faid = nfaid;
            fapos = nfapos;
            ab = nab;
        }
        final int deleted = delbytes;
        init(nsize);
        return deleted;
    }

    public final void unGhost() {
        ghost = false;
        delcount = 0;
        delbytes = 0;
        for (int i = 0; i < size; i++) {
            fapos[i] &= 0x7fffffff; // clear deleted flags
        }
    }

    /**
     * expand a 32-bit micro-cache-internal id into a 64-bit (lon|lat) global-id
     *
     * @see #shrinkId
     */
    public long expandId(final int id32) {
        throw new IllegalArgumentException("expandId for empty cache");
    }

    /**
     * shrink a 64-bit (lon|lat) global-id into a a 32-bit micro-cache-internal id
     *
     * @see #expandId
     */
    public int shrinkId(final long id64) {
        throw new IllegalArgumentException("shrinkId for empty cache");
    }

}
