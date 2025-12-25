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

class ByteDataIOTest {
    @Test
    public Unit varLengthEncodeDecodeTest() {
        final Byte[] ab = Byte[4000]
        val w: ByteDataWriter = ByteDataWriter(ab)
        for (Int i = 0; i < 1000; i++) {
            w.writeVarLengthUnsigned(i)
        }
        val r: ByteDataReader = ByteDataReader(ab)

        for (Int i = 0; i < 1000; i++) {
            val value: Int = r.readVarLengthUnsigned()
            Assert.assertEquals("value mismatch", value, i)
        }
    }
}
