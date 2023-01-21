package cgeo.geocaching.brouter.util;


public class BitCoderContext {
    public static final int[] vl_values = new int[4096];
    public static final int[] vl_length = new int[4096];
    private static final int[] vc_values = new int[4096];
    private static final int[] vc_length = new int[4096];
    private static final int[] reverse_byte = new int[256];
    private static final int[] bm2bits = new int[256];

    static {
        // fill varbits lookup table

        final BitCoderContext bc = new BitCoderContext(new byte[4]);
        for (int i = 0; i < 4096; i++) {
            bc.reset();
            bc.bits = 14;
            bc.b = 0x1000 + i;

            final int b0 = bc.getReadingBitPosition();
            vl_values[i] = bc.decodeVarBits2();
            vl_length[i] = bc.getReadingBitPosition() - b0;
        }
        for (int i = 0; i < 4096; i++) {
            bc.reset();
            final int b0 = bc.getWritingBitPosition();
            bc.encodeVarBits2(i);
            vc_values[i] = bc.b;
            vc_length[i] = bc.getWritingBitPosition() - b0;
        }
        for (int i = 0; i < 1024; i++) {
            bc.reset();
            bc.bits = 14;
            bc.b = 0x1000 + i;

            final int b0 = bc.getReadingBitPosition();
            vl_values[i] = bc.decodeVarBits2();
            vl_length[i] = bc.getReadingBitPosition() - b0;
        }
        for (int b = 0; b < 256; b++) {
            int r = 0;
            for (int i = 0; i < 8; i++) {
                if ((b & (1 << i)) != 0) {
                    r |= 1 << (7 - i);
                }
            }
            reverse_byte[b] = r;
        }
        for (int b = 0; b < 8; b++) {
            bm2bits[1 << b] = b;
        }
    }

    private byte[] ab;
    private int idxMax;
    private int idx = -1;
    private int bits; // bits left in buffer
    private int b; // buffer word


    public BitCoderContext(final byte[] ab) {
        this.ab = ab;
        idxMax = ab.length - 1;
    }

    public static void main(final String[] args) {
        final byte[] ab = new byte[581969];
        BitCoderContext ctx = new BitCoderContext(ab);
        for (int i = 0; i < 31; i++) {
            ctx.encodeVarBits((1 << i) + 3);
        }
        for (int i = 0; i < 100000; i += 13) {
            ctx.encodeVarBits(i);
        }
        ctx.closeAndGetEncodedLength();
        ctx = new BitCoderContext(ab);

        for (int i = 0; i < 31; i++) {
            final int value = ctx.decodeVarBits();
            final int v0 = (1 << i) + 3;
            if (v0 != value) {
                throw new RuntimeException("value mismatch value=" + value + "v0=" + v0);
            }
        }
        for (int i = 0; i < 100000; i += 13) {
            final int value = ctx.decodeVarBits();
            if (value != i) {
                throw new RuntimeException("value mismatch i=" + i + "v=" + value);
            }
        }
    }

    public final void reset(final byte[] ab) {
        this.ab = ab;
        idxMax = ab.length - 1;
        reset();
    }

    public final void reset() {
        idx = -1;
        bits = 0;
        b = 0;
    }

    /**
     * encode a distance with a variable bit length
     * (poor mans huffman tree)
     * {@code 1 -> 0}
     * {@code 01 -> 1} + following 1-bit word ( 1..2 )
     * {@code 001 -> 3} + following 2-bit word ( 3..6 )
     * {@code 0001 -> 7} + following 3-bit word ( 7..14 ) etc.
     *
     * @see #decodeVarBits
     */
    public final void encodeVarBits2(int value) {
        int range = 0;
        while (value > range) {
            encodeBit(false);
            value -= range + 1;
            range = 2 * range + 1;
        }
        encodeBit(true);
        encodeBounded(range, value);
    }

    public final void encodeVarBits(final int value) {
        if ((value & 0xfff) == value) {
            flushBuffer();
            b |= vc_values[value] << bits;
            bits += vc_length[value];
        } else {
            encodeVarBits2(value); // slow fallback for large values
        }
    }

    /**
     * @see #encodeVarBits
     */
    public final int decodeVarBits2() {
        int range = 0;
        while (!decodeBit()) {
            range = 2 * range + 1;
        }
        return range + decodeBounded(range);
    }

    public final int decodeVarBits() {
        fillBuffer();
        final int b12 = b & 0xfff;
        final int len = vl_length[b12];
        if (len <= 12) {
            b >>>= len;
            bits -= len;
            return vl_values[b12]; // full value lookup
        }
        if (len <= 23) { // // only length lookup
            final int len2 = len >> 1;
            b >>>= len2 + 1;
            int mask = 0xffffffff >>> (32 - len2);
            mask += b & mask;
            b >>>= len2;
            bits -= len;
            return mask;
        }
        if ((b & 0xffffff) != 0) {
            // here we just know len in [25..47]
            // ( fillBuffer guarantees only 24 bits! )
            b >>>= 12;
            final int len3 = 1 + (vl_length[b & 0xfff] >> 1);
            b >>>= len3;
            final int len2 = 11 + len3;
            bits -= len2 + 1;
            fillBuffer();
            int mask = 0xffffffff >>> (32 - len2);
            mask += b & mask;
            b >>>= len2;
            bits -= len2;
            return mask;
        }
        return decodeVarBits2(); // no chance, use the slow one
    }

    public final void encodeBit(final boolean value) {
        if (bits > 31) {
            ab[++idx] = (byte) (b & 0xff);
            b >>>= 8;
            bits -= 8;
        }
        if (value) {
            b |= 1 << bits;
        }
        bits++;
    }

    public final boolean decodeBit() {
        if (bits == 0) {
            bits = 8;
            b = ab[++idx] & 0xff;
        }
        final boolean value = (b & 1) != 0;
        b >>>= 1;
        bits--;
        return value;
    }

    /**
     * encode an integer in the range 0..max (inclusive).
     * For max = 2^n-1, this just encodes n bits, but in general
     * this is variable length encoding, with the shorter codes
     * for the central value range
     */
    public final void encodeBounded(int max, final int value) {
        int im = 1; // integer mask
        while (im <= max) {
            if ((value & im) != 0) {
                encodeBit(true);
                max -= im;
            } else {
                encodeBit(false);
            }
            im <<= 1;
        }
    }

    /**
     * decode an integer in the range 0..max (inclusive).
     *
     * @see #encodeBounded
     */
    public final int decodeBounded(final int max) {
        int value = 0;
        int im = 1; // integer mask
        while ((value | im) <= max) {
            if (bits == 0) {
                bits = 8;
                b = ab[++idx] & 0xff;
            }
            if ((b & 1) != 0) {
                value |= im;
            }
            b >>>= 1;
            bits--;
            im <<= 1;
        }
        return value;
    }

    public final int decodeBits(final int count) {
        fillBuffer();
        final int mask = 0xffffffff >>> (32 - count);
        final int value = b & mask;
        b >>>= count;
        bits -= count;
        return value;
    }

    public final int decodeBitsReverse(int count) {
        fillBuffer();
        int value = 0;
        while (count > 8) {
            value = (value << 8) | reverse_byte[b & 0xff];
            b >>= 8;
            count -= 8;
            bits -= 8;
            fillBuffer();
        }
        value = (value << count) | reverse_byte[b & 0xff] >> (8 - count);
        bits -= count;
        b >>= count;
        return value;
    }

    private void fillBuffer() {
        while (bits < 24) {
            if (idx++ < idxMax) {
                b |= (ab[idx] & 0xff) << bits;
            }
            bits += 8;
        }
    }

    private void flushBuffer() {
        while (bits > 7) {
            ab[++idx] = (byte) (b & 0xff);
            b >>>= 8;
            bits -= 8;
        }
    }

    /**
     * flushes and closes the (write-mode) context
     *
     * @return the encoded length in bytes
     */
    public final int closeAndGetEncodedLength() {
        flushBuffer();
        if (bits > 0) {
            ab[++idx] = (byte) (b & 0xff);
        }
        return idx + 1;
    }

    /**
     * @return the encoded length in bits
     */
    public final int getWritingBitPosition() {
        return (idx << 3) + 8 + bits;
    }

    public final int getReadingBitPosition() {
        return (idx << 3) + 8 - bits;
    }

    public final void setReadingBitPosition(final int pos) {
        idx = pos >>> 3;
        bits = (idx << 3) + 8 - pos;
        b = ab[idx] & 0xff;
        b >>>= 8 - bits;
    }
}
