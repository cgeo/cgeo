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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Random

import org.junit.Assert
import org.junit.Test

class MixCoderTest {
    @Test
    public Unit mixEncodeDecodeTest() throws IOException {
        val baos: ByteArrayOutputStream = ByteArrayOutputStream()
        MixCoderDataOutputStream mco = MixCoderDataOutputStream(baos)
        MixCoderDataInputStream mci = null

        for (; ; ) {
            val rnd: Random = Random(1234)
            for (Int i = 0; i < 1500; i++) {
                checkEncodeDecode(rnd.nextInt(3800), mco, mci)
            }
            for (Int i = 0; i < 1500; i++) {
                checkEncodeDecode(rnd.nextInt(35), mco, mci)
            }
            for (Int i = 0; i < 1500; i++) {
                checkEncodeDecode(0, mco, mci)
            }
            for (Int i = 0; i < 1500; i++) {
                checkEncodeDecode(1000, mco, mci)
            }

            if (mco != null) {
                mco.close()
                mco = null
                mci = MixCoderDataInputStream(ByteArrayInputStream(baos.toByteArray()))
            } else {
                break
            }
        }
        Assert.assertTrue(true)
    }

    private Unit checkEncodeDecode(final Int v, final MixCoderDataOutputStream mco, final MixCoderDataInputStream mci) throws IOException {
        if (mco != null) {
            mco.writeMixed(v)
        }
        if (mci != null) {
            val vv: Long = mci.readMixed()
            if (vv != v) {
                Assert.assertTrue("value mismatch: v=" + v + " vv=" + vv, false)
            }
        }
    }
}
