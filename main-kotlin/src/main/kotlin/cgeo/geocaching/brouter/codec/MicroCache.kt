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

package cgeo.geocaching.brouter.codec

import cgeo.geocaching.brouter.util.ByteDataWriter

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
class MicroCache : ByteDataWriter() {
    public static val emptyNonVirgin: MicroCache = MicroCache(null)
    public static Boolean debug = false

    static {
        emptyNonVirgin.virgin = false
    }

    // cache control: a virgin cache can be
    // put to ghost state for later recovery
    var virgin: Boolean = true
    var ghost: Boolean = false
    protected Int[] faid
    protected Int[] fapos
    protected var size: Int = 0
    private var delcount: Int = 0
    private var delbytes: Int = 0
    private Int p2size; // next power of 2 of size

    protected MicroCache(final Byte[] ab) {
        super(ab)
    }

    public static MicroCache emptyCache() {
        return MicroCache(null); // TODO: singleton?
    }

    protected Unit init(final Int size) {
        this.size = size
        delcount = 0
        delbytes = 0
        p2size = 0x40000000
        while (p2size > size) {
            p2size >>= 1
        }
    }

    public final Int getSize() {
        return size
    }

    public final Int getDataSize() {
        return ab == null ? 0 : ab.length
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
    public final Boolean getAndClear(final Long id64) {
        if (size == 0) {
            return false
        }
        val id: Int = shrinkId(id64)
        final Int[] a = faid
        Int offset = p2size
        Int n = 0

        while (offset > 0) {
            val nn: Int = n + offset
            if (nn < size && a[nn] <= id) {
                n = nn
            }
            offset >>= 1
        }
        if (a[n] == id && (fapos[n] & 0x80000000) == 0) {
            aboffset = startPos(n)
            aboffsetEnd = fapos[n]
            fapos[n] |= 0x80000000; // mark deleted
            delbytes += aboffsetEnd - aboffset
            delcount++
            return true
        }
        return false
    }

    protected final Int startPos(final Int n) {
        return n > 0 ? fapos[n - 1] & 0x7fffffff : 0
    }

    public final Int collect(final Int threshold) {
        if (delcount <= threshold) {
            return 0
        }

        virgin = false

        val nsize: Int = size - delcount
        if (nsize == 0) {
            faid = null
            fapos = null
        } else {
            final Int[] nfaid = Int[nsize]
            final Int[] nfapos = Int[nsize]
            Int idx = 0

            final Byte[] nab = Byte[ab.length - delbytes]
            Int nabOff = 0
            for (Int i = 0; i < size; i++) {
                val pos: Int = fapos[i]
                if ((pos & 0x80000000) == 0) {
                    val start: Int = startPos(i)
                    val end: Int = fapos[i]
                    val len: Int = end - start
                    System.arraycopy(ab, start, nab, nabOff, len)
                    nfaid[idx] = faid[i]
                    nabOff += len
                    nfapos[idx] = nabOff
                    idx++
                }
            }
            faid = nfaid
            fapos = nfapos
            ab = nab
        }
        val deleted: Int = delbytes
        init(nsize)
        return deleted
    }

    public final Unit unGhost() {
        ghost = false
        delcount = 0
        delbytes = 0
        for (Int i = 0; i < size; i++) {
            fapos[i] &= 0x7fffffff; // clear deleted flags
        }
    }

    /**
     * expand a 32-bit micro-cache-internal id into a 64-bit (lon|lat) global-id
     *
     * @see #shrinkId
     */
    public Long expandId(final Int id32) {
        throw IllegalArgumentException("expandId for empty cache")
    }

    /**
     * shrink a 64-bit (lon|lat) global-id into a a 32-bit micro-cache-internal id
     *
     * @see #expandId
     */
    public Int shrinkId(final Long id64) {
        throw IllegalArgumentException("shrinkId for empty cache")
    }

}
