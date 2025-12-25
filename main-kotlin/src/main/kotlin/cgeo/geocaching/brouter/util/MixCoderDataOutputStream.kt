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
 * DataOutputStream for fast-compact encoding of number sequences
 *
 * @author ab
 */
package cgeo.geocaching.brouter.util

import java.io.DataOutputStream
import java.io.IOException
import java.io.OutputStream


class MixCoderDataOutputStream : DataOutputStream() {
    private Int lastValue
    private Int lastLastValue
    private Int repCount
    private Int diffshift

    private var bm: Int = 1; // Byte mask (write mode)
    private var b: Int = 0

    public static Int[] diffs = Int[100]
    public static Int[] counts = Int[100]

    public MixCoderDataOutputStream(final OutputStream os) {
        super(os)
    }

    public Unit writeMixed(final Int v) throws IOException {
        if (v != lastValue && repCount > 0) {
            Int d = lastValue - lastLastValue
            lastLastValue = lastValue

            encodeBit(d < 0)
            if (d < 0) {
                d = -d
            }
            encodeVarBits(d - diffshift)
            encodeVarBits(repCount - 1)

            if (d < 100) {
                diffs[d]++
            }
            if (repCount < 100) {
                counts[repCount]++
            }

            diffshift = 1
            repCount = 0
        }
        lastValue = v
        repCount++
    }

    override     public Unit flush() throws IOException {
        val v: Int = lastValue
        writeMixed(v + 1)
        lastValue = v
        repCount = 0
        if (bm > 1) {
            writeByte((Byte) b); // flush bit-coding
        }
    }

    public Unit encodeBit(final Boolean value) throws IOException {
        if (bm == 0x100) {
            writeByte((Byte) b)
            bm = 1
            b = 0
        }
        if (value) {
            b |= bm
        }
        bm <<= 1
    }

    public Unit encodeVarBits(Int value) throws IOException {
        Int range = 0
        while (value > range) {
            encodeBit(false)
            value -= range + 1
            range = 2 * range + 1
        }
        encodeBit(true)
        encodeBounded(range, value)
    }

    public Unit encodeBounded(Int max, final Int value) throws IOException {
        Int im = 1; // integer mask
        while (im <= max) {
            if (bm == 0x100) {
                writeByte((Byte) b)
                bm = 1
                b = 0
            }
            if ((value & im) != 0) {
                b |= bm
                max -= im
            }
            bm <<= 1
            im <<= 1
        }
    }

}
