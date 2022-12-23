package cgeo.geocaching.brouter.codec;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class StatCoderContextTest {
    @Test
    public void noisyVarBitsEncodeDecodeTest() {
        final byte[] ab = new byte[40000];
        StatCoderContext ctx = new StatCoderContext(ab);
        for (int noisybits = 1; noisybits < 12; noisybits++) {
            for (int i = 0; i < 1000; i++) {
                ctx.encodeNoisyNumber(i, noisybits);
            }
        }
        ctx.closeAndGetEncodedLength();
        ctx = new StatCoderContext(ab);

        for (int noisybits = 1; noisybits < 12; noisybits++) {
            for (int i = 0; i < 1000; i++) {
                final int value = ctx.decodeNoisyNumber(noisybits);
                if (value != i) {
                    Assert.fail("value mismatch: noisybits=" + noisybits + " i=" + i + " value=" + value);
                }
            }
        }
    }

    @Test
    public void noisySignedVarBitsEncodeDecodeTest() {
        final byte[] ab = new byte[80000];
        StatCoderContext ctx = new StatCoderContext(ab);
        for (int noisybits = 0; noisybits < 12; noisybits++) {
            for (int i = -1000; i < 1000; i++) {
                ctx.encodeNoisyDiff(i, noisybits);
            }
        }
        ctx.closeAndGetEncodedLength();
        ctx = new StatCoderContext(ab);

        for (int noisybits = 0; noisybits < 12; noisybits++) {
            for (int i = -1000; i < 1000; i++) {
                final int value = ctx.decodeNoisyDiff(noisybits);
                if (value != i) {
                    Assert.fail("value mismatch: noisybits=" + noisybits + " i=" + i + " value=" + value);
                }
            }
        }
    }

    @Test
    public void predictedValueEncodeDecodeTest() {
        final byte[] ab = new byte[80000];
        StatCoderContext ctx = new StatCoderContext(ab);
        for (int value = -100; value < 100; value += 5) {
            for (int predictor = -200; predictor < 200; predictor += 7) {
                ctx.encodePredictedValue(value, predictor);
            }
        }
        ctx.closeAndGetEncodedLength();
        ctx = new StatCoderContext(ab);

        for (int value = -100; value < 100; value += 5) {
            for (int predictor = -200; predictor < 200; predictor += 7) {
                final int decodedValue = ctx.decodePredictedValue(predictor);
                if (value != decodedValue) {
                    Assert.fail("value mismatch: value=" + value + " predictor=" + predictor + " decodedValue=" + decodedValue);
                }
            }
        }
    }

    @Test
    public void sortedArrayEncodeDecodeTest() {
        final Random rand = new Random();
        final int size = 1000000;
        final int[] values = new int[size];
        for (int i = 0; i < size; i++) {
            values[i] = rand.nextInt() & 0x0fffffff;
        }
        values[5] = 175384; // force collision
        values[8] = 175384;

        values[15] = 275384; // force neighbours
        values[18] = 275385;

        Arrays.sort(values);

        final byte[] ab = new byte[3000000];
        StatCoderContext ctx = new StatCoderContext(ab);
        ctx.encodeSortedArray(values, 0, size, 0x08000000, 0);

        ctx.closeAndGetEncodedLength();
        ctx = new StatCoderContext(ab);

        final int[] decodedValues = new int[size];
        ctx.decodeSortedArray(decodedValues, 0, size, 27, 0);

        for (int i = 0; i < size; i++) {
            if (values[i] != decodedValues[i]) {
                Assert.fail("mismatch at i=" + i + " " + values[i] + "<>" + decodedValues[i]);
            }
        }
    }
}
