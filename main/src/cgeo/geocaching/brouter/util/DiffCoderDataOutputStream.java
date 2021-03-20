/**
 * DataOutputStream extended by varlength diff coding
 *
 * @author ab
 */
package cgeo.geocaching.brouter.util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public final class DiffCoderDataOutputStream extends DataOutputStream {
    private final long[] lastValues = new long[10];

    public DiffCoderDataOutputStream(OutputStream os) {
        super(os);
    }

    public void writeDiffed(long v, int idx) throws IOException {
        long d = v - lastValues[idx];
        lastValues[idx] = v;
        writeSigned(d);
    }

    public void writeSigned(long v) throws IOException {
        writeUnsigned(v < 0 ? ((-v) << 1) | 1 : v << 1);
    }

    public void writeUnsigned(long v) throws IOException {
        do
        {
            long i7 = v & 0x7f;
            v >>= 7;
            if (v != 0)
                i7 |= 0x80;
            writeByte((byte) (i7 & 0xff));
        }
        while (v != 0);
    }
}
