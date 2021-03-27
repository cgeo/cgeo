package cgeo.geocaching.brouter.util;

import org.junit.Assert;
import org.junit.Test;

public class BitCoderContextTest {
    @Test
    public void varBitsEncodeDecodeTest() {
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
            Assert.assertTrue("value mismatch value=" + value + "v0=" + v0, v0 == value);
        }
        for (int i = 0; i < 100000; i += 13) {
            final int value = ctx.decodeVarBits();
            Assert.assertTrue("value mismatch i=" + i + "v=" + value, value == i);
        }
    }

    @Test
    public void boundedEncodeDecodeTest() {
        final byte[] ab = new byte[581969];
        BitCoderContext ctx = new BitCoderContext(ab);
        for (int max = 1; max < 1000; max++) {
            for (int val = 0; val <= max; val++) {
                ctx.encodeBounded(max, val);
            }
        }
        ctx.closeAndGetEncodedLength();

        ctx = new BitCoderContext(ab);

        for (int max = 1; max < 1000; max++) {
            for (int val = 0; val <= max; val++) {
                final int valDecoded = ctx.decodeBounded(max);
                if (valDecoded != val) {
                    Assert.fail("mismatch at max=" + max + " " + valDecoded + "<>" + val);
                }
            }
        }
    }
}
