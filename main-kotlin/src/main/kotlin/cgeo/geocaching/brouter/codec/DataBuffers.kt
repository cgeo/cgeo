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

import cgeo.geocaching.brouter.util.BitCoderContext

/**
 * Container for some re-usable databuffers for the decoder
 */
class DataBuffers {
    public Byte[] iobuffer
    public Byte[] tagbuf1 = Byte[256]
    var bctx1: BitCoderContext = BitCoderContext(tagbuf1)
    public Byte[] bbuf1 = Byte[65636]
    public Int[] ibuf1 = Int[4096]
    public Int[] ibuf2 = Int[2048]
    public Int[] ibuf3 = Int[2048]
    public Int[] alon = Int[2048]
    public Int[] alat = Int[2048]

    public DataBuffers() {
        this(Byte[65636])
    }

    /**
     * construct a set of databuffers except
     * for 'iobuffer', where the given array is used
     */
    public DataBuffers(final Byte[] iobuffer) {
        this.iobuffer = iobuffer
    }

}
