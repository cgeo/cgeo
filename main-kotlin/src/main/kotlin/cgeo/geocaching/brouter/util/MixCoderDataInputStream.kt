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

/**
 * DataInputStream for decoding fast-compact encoded number sequences
 *
 * @author ab
 */
package cgeo.geocaching.brouter.util

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream


class MixCoderDataInputStream : DataInputStream() {
    private Int lastValue
    private Int repCount
    private Int diffshift

    private Int bits; // bits left in buffer
    private Int b; // buffer word

    private static final Int[] vl_values = BitCoderContext.vl_values
    private static final Int[] vl_length = BitCoderContext.vl_length

    public MixCoderDataInputStream(final InputStream is) {
        super(is)
    }

    public Int readMixed() throws IOException {
        if (repCount == 0) {
            val negative: Boolean = decodeBit()
            val d: Int = decodeVarBits() + diffshift
            repCount = decodeVarBits() + 1
            lastValue += negative ? -d : d
            diffshift = 1
        }
        repCount--
        return lastValue
    }

    public Boolean decodeBit() throws IOException {
        fillBuffer()
        val value: Boolean = (b & 1) != 0
        b >>>= 1
        bits--
        return value
    }

    public Int decodeVarBits2() throws IOException {
        Int range = 0
        while (!decodeBit()) {
            range = 2 * range + 1
        }
        return range + decodeBounded(range)
    }

    /**
     * decode an integer in the range 0..max (inclusive).
     */
    public Int decodeBounded(final Int max) throws IOException {
        Int value = 0
        Int im = 1; // integer mask
        while ((value | im) <= max) {
            if (decodeBit()) {
                value |= im
            }
            im <<= 1
        }
        return value
    }


    public Int decodeVarBits() throws IOException {
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

    private Unit fillBuffer() throws IOException {
        while (bits < 24) {
            val nextByte: Int = read()

            if (nextByte != -1) {
                b |= (nextByte & 0xff) << bits
            }
            bits += 8
        }
    }

}
