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
    public final static MicroCache emptyNonVirgin = new MicroCache(null);
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

    protected MicroCache(byte[] ab) {
        super(ab);
    }

    public static MicroCache emptyCache() {
        return new MicroCache(null); // TODO: singleton?
    }

    protected void init(int size) {
        this.size = size;
        delcount = 0;
        delbytes = 0;
        p2size = 0x40000000;
        while (p2size > size)
            p2size >>= 1;
    }

    public final void finishNode(long id) {
        fapos[size] = aboffset;
        faid[size] = shrinkId(id);
        size++;
    }

    public final void discardNode() {
        aboffset = startPos(size);
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
    public final boolean getAndClear(long id64) {
        if (size == 0) {
            return false;
        }
        int id = shrinkId(id64);
        int[] a = faid;
        int offset = p2size;
        int n = 0;

        while (offset > 0) {
            int nn = n + offset;
            if (nn < size && a[nn] <= id) {
                n = nn;
            }
            offset >>= 1;
        }
        if (a[n] == id) {
            if ((fapos[n] & 0x80000000) == 0) {
                aboffset = startPos(n);
                aboffsetEnd = fapos[n];
                fapos[n] |= 0x80000000; // mark deleted
                delbytes += aboffsetEnd - aboffset;
                delcount++;
                return true;
            } else // .. marked as deleted
            {
                // throw new RuntimeException( "MicroCache: node already consumed: id=" + id );
            }
        }
        return false;
    }

    protected final int startPos(int n) {
        return n > 0 ? fapos[n - 1] & 0x7fffffff : 0;
    }

    public final int collect(int threshold) {
        if (delcount <= threshold) {
            return 0;
        }

        virgin = false;

        int nsize = size - delcount;
        if (nsize == 0) {
            faid = null;
            fapos = null;
        } else {
            int[] nfaid = new int[nsize];
            int[] nfapos = new int[nsize];
            int idx = 0;

            byte[] nab = new byte[ab.length - delbytes];
            int nab_off = 0;
            for (int i = 0; i < size; i++) {
                int pos = fapos[i];
                if ((pos & 0x80000000) == 0) {
                    int start = startPos(i);
                    int end = fapos[i];
                    int len = end - start;
                    System.arraycopy(ab, start, nab, nab_off, len);
                    nfaid[idx] = faid[i];
                    nab_off += len;
                    nfapos[idx] = nab_off;
                    idx++;
                }
            }
            faid = nfaid;
            fapos = nfapos;
            ab = nab;
        }
        int deleted = delbytes;
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
     * @return the 64-bit global id for the given cache-position
     */
    public final long getIdForIndex(int i) {
        int id32 = faid[i];
        return expandId(id32);
    }

    /**
     * expand a 32-bit micro-cache-internal id into a 64-bit (lon|lat) global-id
     *
     * @see #shrinkId
     */
    public long expandId(int id32) {
        throw new IllegalArgumentException("expandId for empty cache");
    }

    /**
     * shrink a 64-bit (lon|lat) global-id into a a 32-bit micro-cache-internal id
     *
     * @see #expandId
     */
    public int shrinkId(long id64) {
        throw new IllegalArgumentException("shrinkId for empty cache");
    }

    /**
     * @return true if the given lon/lat position is internal for that micro-cache
     */
    public boolean isInternal(int ilon, int ilat) {
        throw new IllegalArgumentException("isInternal for empty cache");
    }

    /**
     * (stasticially) encode the micro-cache into the format used in the datafiles
     *
     * @param buffer byte array to encode into (considered big enough)
     * @return the size of the encoded data
     */
    public int encodeMicroCache(byte[] buffer) {
        throw new IllegalArgumentException("encodeMicroCache for empty cache");
    }

    /**
     * Compare the content of this microcache to another
     *
     * @return null if equals, else a diff-report
     */
    public String compareWith(MicroCache mc) {
        String msg = _compareWith(mc);
        if (msg != null) {
            StringBuilder sb = new StringBuilder(msg);
            sb.append("\nencode cache:\n").append(summary());
            sb.append("\ndecode cache:\n").append(mc.summary());
            return sb.toString();
        }
        return null;
    }

    private String summary() {
        StringBuilder sb = new StringBuilder("size=" + size + " aboffset=" + aboffset);
        for (int i = 0; i < size; i++) {
            sb.append("\nidx=" + i + " faid=" + faid[i] + " fapos=" + fapos[i]);
        }
        return sb.toString();
    }

    private String _compareWith(MicroCache mc) {
        if (size != mc.size) {
            return "size missmatch: " + size + "->" + mc.size;
        }
        for (int i = 0; i < size; i++) {
            if (faid[i] != mc.faid[i]) {
                return "faid missmatch at index " + i + ":" + faid[i] + "->" + mc.faid[i];
            }
            int start = i > 0 ? fapos[i - 1] : 0;
            int end = fapos[i] < mc.fapos[i] ? fapos[i] : mc.fapos[i];
            int len = end - start;
            for (int offset = 0; offset < len; offset++) {
                if (mc.ab.length <= start + offset) {
                    return "data buffer too small";
                }
                if (ab[start + offset] != mc.ab[start + offset]) {
                    return "data missmatch at index " + i + " offset=" + offset;
                }
            }
            if (fapos[i] != mc.fapos[i]) {
                return "fapos missmatch at index " + i + ":" + fapos[i] + "->" + mc.fapos[i];
            }
        }
        if (aboffset != mc.aboffset) {
            return "datasize missmatch: " + aboffset + "->" + mc.aboffset;
        }
        return null;
    }

    public void calcDelta(MicroCache mc1, MicroCache mc2) {
        int idx1 = 0;
        int idx2 = 0;

        while (idx1 < mc1.size || idx2 < mc2.size) {
            int id1 = idx1 < mc1.size ? mc1.faid[idx1] : Integer.MAX_VALUE;
            int id2 = idx2 < mc2.size ? mc2.faid[idx2] : Integer.MAX_VALUE;
            int id;
            if (id1 >= id2) {
                id = id2;
                int start2 = idx2 > 0 ? mc2.fapos[idx2 - 1] : 0;
                int len2 = mc2.fapos[idx2++] - start2;

                if (id1 == id2) {
                    // id exists in both caches, compare data
                    int start1 = idx1 > 0 ? mc1.fapos[idx1 - 1] : 0;
                    int len1 = mc1.fapos[idx1++] - start1;
                    if (len1 == len2) {
                        int i = 0;
                        while (i < len1) {
                            if (mc1.ab[start1 + i] != mc2.ab[start2 + i]) {
                                break;
                            }
                            i++;
                        }
                        if (i == len1) {
                            continue; // same data -> do nothing
                        }
                    }
                }
                write(mc2.ab, start2, len2);
            } else {
                idx1++;
                id = id1; // deleted node
            }
            fapos[size] = aboffset;
            faid[size] = id;
            size++;
        }
    }

    public void addDelta(MicroCache mc1, MicroCache mc2, boolean keepEmptyNodes) {
        int idx1 = 0;
        int idx2 = 0;

        while (idx1 < mc1.size || idx2 < mc2.size) {
            int id1 = idx1 < mc1.size ? mc1.faid[idx1] : Integer.MAX_VALUE;
            int id2 = idx2 < mc2.size ? mc2.faid[idx2] : Integer.MAX_VALUE;
            if (id1 >= id2) // data from diff file wins
            {
                int start2 = idx2 > 0 ? mc2.fapos[idx2 - 1] : 0;
                int len2 = mc2.fapos[idx2++] - start2;
                if (keepEmptyNodes || len2 > 0) {
                    write(mc2.ab, start2, len2);
                    fapos[size] = aboffset;
                    faid[size++] = id2;
                }
                if (id1 == id2) // // id exists in both caches
                {
                    idx1++;
                }
            } else // use data from base file
            {
                int start1 = idx1 > 0 ? mc1.fapos[idx1 - 1] : 0;
                int len1 = mc1.fapos[idx1++] - start1;
                write(mc1.ab, start1, len1);
                fapos[size] = aboffset;
                faid[size++] = id1;
            }
        }
    }
}
