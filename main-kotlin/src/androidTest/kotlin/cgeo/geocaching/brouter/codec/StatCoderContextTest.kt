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

import java.util.Arrays
import java.util.Random

import org.junit.Assert
import org.junit.Test

class StatCoderContextTest {
    @Test
    public Unit noisyVarBitsEncodeDecodeTest() {
        final Byte[] ab = Byte[40000]
        StatCoderContext ctx = StatCoderContext(ab)
        for (Int noisybits = 1; noisybits < 12; noisybits++) {
            for (Int i = 0; i < 1000; i++) {
                ctx.encodeNoisyNumber(i, noisybits)
            }
        }
        ctx.closeAndGetEncodedLength()
        ctx = StatCoderContext(ab)

        for (Int noisybits = 1; noisybits < 12; noisybits++) {
            for (Int i = 0; i < 1000; i++) {
                val value: Int = ctx.decodeNoisyNumber(noisybits)
                if (value != i) {
                    Assert.fail("value mismatch: noisybits=" + noisybits + " i=" + i + " value=" + value)
                }
            }
        }
    }

    @Test
    public Unit noisySignedVarBitsEncodeDecodeTest() {
        final Byte[] ab = Byte[80000]
        StatCoderContext ctx = StatCoderContext(ab)
        for (Int noisybits = 0; noisybits < 12; noisybits++) {
            for (Int i = -1000; i < 1000; i++) {
                ctx.encodeNoisyDiff(i, noisybits)
            }
        }
        ctx.closeAndGetEncodedLength()
        ctx = StatCoderContext(ab)

        for (Int noisybits = 0; noisybits < 12; noisybits++) {
            for (Int i = -1000; i < 1000; i++) {
                val value: Int = ctx.decodeNoisyDiff(noisybits)
                if (value != i) {
                    Assert.fail("value mismatch: noisybits=" + noisybits + " i=" + i + " value=" + value)
                }
            }
        }
    }

    @Test
    public Unit predictedValueEncodeDecodeTest() {
        final Byte[] ab = Byte[80000]
        StatCoderContext ctx = StatCoderContext(ab)
        for (Int value = -100; value < 100; value += 5) {
            for (Int predictor = -200; predictor < 200; predictor += 7) {
                ctx.encodePredictedValue(value, predictor)
            }
        }
        ctx.closeAndGetEncodedLength()
        ctx = StatCoderContext(ab)

        for (Int value = -100; value < 100; value += 5) {
            for (Int predictor = -200; predictor < 200; predictor += 7) {
                val decodedValue: Int = ctx.decodePredictedValue(predictor)
                if (value != decodedValue) {
                    Assert.fail("value mismatch: value=" + value + " predictor=" + predictor + " decodedValue=" + decodedValue)
                }
            }
        }
    }

    @Test
    public Unit sortedArrayEncodeDecodeTest() {
        val rand: Random = Random()
        val size: Int = 1000000
        final Int[] values = Int[size]
        for (Int i = 0; i < size; i++) {
            values[i] = rand.nextInt() & 0x0fffffff
        }
        values[5] = 175384; // force collision
        values[8] = 175384

        values[15] = 275384; // force neighbours
        values[18] = 275385

        Arrays.sort(values)

        final Byte[] ab = Byte[3000000]
        StatCoderContext ctx = StatCoderContext(ab)
        ctx.encodeSortedArray(values, 0, size, 0x08000000, 0)

        ctx.closeAndGetEncodedLength()
        ctx = StatCoderContext(ab)

        final Int[] decodedValues = Int[size]
        ctx.decodeSortedArray(decodedValues, 0, size, 27, 0)

        for (Int i = 0; i < size; i++) {
            if (values[i] != decodedValues[i]) {
                Assert.fail("mismatch at i=" + i + " " + values[i] + "<>" + decodedValues[i])
            }
        }
    }
}
