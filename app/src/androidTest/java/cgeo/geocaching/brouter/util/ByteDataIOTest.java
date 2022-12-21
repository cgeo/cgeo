package cgeo.geocaching.brouter.util;

import org.junit.Assert;
import org.junit.Test;

public class ByteDataIOTest {
    @Test
    public void varLengthEncodeDecodeTest() {
        final byte[] ab = new byte[4000];
        final ByteDataWriter w = new ByteDataWriter(ab);
        for (int i = 0; i < 1000; i++) {
            w.writeVarLengthUnsigned(i);
        }
        final ByteDataReader r = new ByteDataReader(ab);

        for (int i = 0; i < 1000; i++) {
            final int value = r.readVarLengthUnsigned();
            Assert.assertTrue("value mismatch", value == i);
        }
    }
}
