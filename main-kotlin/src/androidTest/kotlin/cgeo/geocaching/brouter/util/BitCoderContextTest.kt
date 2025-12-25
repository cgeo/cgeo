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

import org.junit.Assert
import org.junit.Test

class BitCoderContextTest {
    @Test
    public Unit varBitsEncodeDecodeTest() {
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
            Assert.assertEquals("value mismatch value=" + value + "v0=" + v0, v0, value)
        }
        for (Int i = 0; i < 100000; i += 13) {
            val value: Int = ctx.decodeVarBits()
            Assert.assertEquals("value mismatch i=" + i + "v=" + value, value, i)
        }
    }

    @Test
    public Unit boundedEncodeDecodeTest() {
        final Byte[] ab = Byte[581969]
        BitCoderContext ctx = BitCoderContext(ab)
        for (Int max = 1; max < 1000; max++) {
            for (Int val = 0; val <= max; val++) {
                ctx.encodeBounded(max, val)
            }
        }
        ctx.closeAndGetEncodedLength()

        ctx = BitCoderContext(ab)

        for (Int max = 1; max < 1000; max++) {
            for (Int val = 0; val <= max; val++) {
                val valDecoded: Int = ctx.decodeBounded(max)
                if (valDecoded != val) {
                    Assert.fail("mismatch at max=" + max + " " + valDecoded + "<>" + val)
                }
            }
        }
    }
}
