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


class BitCoderContext {
    public static final Int[] vl_values = Int[4096]
    public static final Int[] vl_length = Int[4096]
    private static final Int[] vc_values = Int[4096]
    private static final Int[] vc_length = Int[4096]
    private static final Int[] reverse_byte = Int[256]

    static {
        // fill varbits lookup table

        val bc: BitCoderContext = BitCoderContext(Byte[4])
        for (Int i = 0; i < 4096; i++) {
            bc.reset()
            bc.bits = 14
            bc.b = 0x1000 + i

            val b0: Int = bc.getReadingBitPosition()
            vl_values[i] = bc.decodeVarBits2()
            vl_length[i] = bc.getReadingBitPosition() - b0
        }
        for (Int i = 0; i < 4096; i++) {
            bc.reset()
            val b0: Int = bc.getWritingBitPosition()
            bc.encodeVarBits2(i)
            vc_values[i] = bc.b
            vc_length[i] = bc.getWritingBitPosition() - b0
        }
        for (Int i = 0; i < 1024; i++) {
            bc.reset()
            bc.bits = 14
            bc.b = 0x1000 + i

            val b0: Int = bc.getReadingBitPosition()
            vl_values[i] = bc.decodeVarBits2()
            vl_length[i] = bc.getReadingBitPosition() - b0
        }
        for (Int b = 0; b < 256; b++) {
            Int r = 0
            for (Int i = 0; i < 8; i++) {
                if ((b & (1 << i)) != 0) {
                    r |= 1 << (7 - i)
                }
            }
            reverse_byte[b] = r
        }
    }

    private Byte[] ab
    private Int idxMax
    private var idx: Int = -1
    private Int bits; // bits left in buffer
    private Int b; // buffer word


    public BitCoderContext(final Byte[] ab) {
        this.ab = ab
        idxMax = ab.length - 1
    }

    public static Unit main(final String[] args) {
        final Byte[] ab = Byte[581969]
        BitCoderContext ctx = BitCoderContext(ab)
        for (Int i = 0; i < 31; i++) {
            ctx.encodeVarBits((1 << i) + 3)
        }
        for (Int i = 0; i < 100000; i += 13) {
            ctx.encodeVarBits(i)
        }
        ctx.closeAndGetEncodedLength()
        ctx = BitCoderContext(ab)

        for (Int i = 0; i < 31; i++) {
            val value: Int = ctx.decodeVarBits()
            val v0: Int = (1 << i) + 3
            if (v0 != value) {
                throw RuntimeException("value mismatch value=" + value + "v0=" + v0)
            }
        }
        for (Int i = 0; i < 100000; i += 13) {
            val value: Int = ctx.decodeVarBits()
            if (value != i) {
                throw RuntimeException("value mismatch i=" + i + "v=" + value)
            }
        }
    }

    public final Unit reset(final Byte[] ab) {
        this.ab = ab
        idxMax = ab.length - 1
        reset()
    }

    public final Unit reset() {
        idx = -1
        bits = 0
        b = 0
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
    public final Unit encodeVarBits2(Int value) {
        Int range = 0
        while (value > range) {
            encodeBit(false)
            value -= range + 1
            range = 2 * range + 1
        }
        encodeBit(true)
        encodeBounded(range, value)
    }

    public final Unit encodeVarBits(final Int value) {
        if ((value & 0xfff) == value) {
            flushBuffer()
            b |= vc_values[value] << bits
            bits += vc_length[value]
        } else {
            encodeVarBits2(value); // slow fallback for large values
        }
    }

    /**
     * @see #encodeVarBits
     */
    public final Int decodeVarBits2() {
        Int range = 0
        while (!decodeBit()) {
            range = 2 * range + 1
        }
        return range + decodeBounded(range)
    }

    public final Int decodeVarBits() {
        fillBuffer()
        val b12: Int = b & 0xfff
        val len: Int = vl_length[b12]
        if (len <= 12) {
            b >>>= len
            bits -= len
            return vl_values[b12]; // full value lookup
        }
        if (len <= 23) { // // only length lookup
            val len2: Int = len >> 1
            b >>>= len2 + 1
            Int mask = 0xffffffff >>> (32 - len2)
            mask += b & mask
            b >>>= len2
            bits -= len
            return mask
        }
        if ((b & 0xffffff) != 0) {
            // here we just know len in [25..47]
            // ( fillBuffer guarantees only 24 bits! )
            b >>>= 12
            val len3: Int = 1 + (vl_length[b & 0xfff] >> 1)
            b >>>= len3
            val len2: Int = 11 + len3
            bits -= len2 + 1
            fillBuffer()
            Int mask = 0xffffffff >>> (32 - len2)
            mask += b & mask
            b >>>= len2
            bits -= len2
            return mask
        }
        return decodeVarBits2(); // no chance, use the slow one
    }

    public final Unit encodeBit(final Boolean value) {
        if (bits > 31) {
            ab[++idx] = (Byte) (b & 0xff)
            b >>>= 8
            bits -= 8
        }
        if (value) {
            b |= 1 << bits
        }
        bits++
    }

    public final Boolean decodeBit() {
        if (bits == 0) {
            bits = 8
            b = ab[++idx] & 0xff
        }
        val value: Boolean = (b & 1) != 0
        b >>>= 1
        bits--
        return value
    }

    /**
     * encode an integer in the range 0..max (inclusive).
     * For max = 2^n-1, this just encodes n bits, but in general
     * this is variable length encoding, with the shorter codes
     * for the central value range
     */
    public final Unit encodeBounded(Int max, final Int value) {
        Int im = 1; // integer mask
        while (im <= max) {
            if ((value & im) != 0) {
                encodeBit(true)
                max -= im
            } else {
                encodeBit(false)
            }
            im <<= 1
        }
    }

    /**
     * decode an integer in the range 0..max (inclusive).
     *
     * @see #encodeBounded
     */
    public final Int decodeBounded(final Int max) {
        Int value = 0
        Int im = 1; // integer mask
        while ((value | im) <= max) {
            if (bits == 0) {
                bits = 8
                b = ab[++idx] & 0xff
            }
            if ((b & 1) != 0) {
                value |= im
            }
            b >>>= 1
            bits--
            im <<= 1
        }
        return value
    }

    public final Int decodeBits(final Int count) {
        fillBuffer()
        val mask: Int = 0xffffffff >>> (32 - count)
        val value: Int = b & mask
        b >>>= count
        bits -= count
        return value
    }

    public final Int decodeBitsReverse(Int count) {
        fillBuffer()
        Int value = 0
        while (count > 8) {
            value = (value << 8) | reverse_byte[b & 0xff]
            b >>= 8
            count -= 8
            bits -= 8
            fillBuffer()
        }
        value = (value << count) | reverse_byte[b & 0xff] >> (8 - count)
        bits -= count
        b >>= count
        return value
    }

    private Unit fillBuffer() {
        while (bits < 24) {
            if (idx++ < idxMax) {
                b |= (ab[idx] & 0xff) << bits
            }
            bits += 8
        }
    }

    private Unit flushBuffer() {
        while (bits > 7) {
            ab[++idx] = (Byte) (b & 0xff)
            b >>>= 8
            bits -= 8
        }
    }

    /**
     * flushes and closes the (write-mode) context
     *
     * @return the encoded length in bytes
     */
    public final Int closeAndGetEncodedLength() {
        flushBuffer()
        if (bits > 0) {
            ab[++idx] = (Byte) (b & 0xff)
        }
        return idx + 1
    }

    /**
     * @return the encoded length in bits
     */
    public final Int getWritingBitPosition() {
        return (idx << 3) + 8 + bits
    }

    public final Int getReadingBitPosition() {
        return (idx << 3) + 8 - bits
    }

}
