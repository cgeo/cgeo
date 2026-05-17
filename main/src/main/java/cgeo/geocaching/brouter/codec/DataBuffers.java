package cgeo.geocaching.brouter.codec;

import cgeo.geocaching.brouter.util.BitCoderContext;

/**
 * Container for some re-usable databuffers for the decoder
 */
public final class DataBuffers {
    public final byte[] iobuffer;
    public final byte[] tagbuf1 = new byte[256];
    public final BitCoderContext bctx1 = new BitCoderContext(tagbuf1);
    public final byte[] bbuf1 = new byte[65636];
    public final int[] ibuf1 = new int[4096];
    public final int[] ibuf2 = new int[2048];
    public final int[] ibuf3 = new int[2048];
    public final int[] alon = new int[2048];
    public final int[] alat = new int[2048];

    public DataBuffers() {
        this(new byte[65636]);
    }

    /**
     * construct a set of databuffers except
     * for 'iobuffer', where the given array is used
     */
    public DataBuffers(final byte[] iobuffer) {
        this.iobuffer = iobuffer;
    }

}
