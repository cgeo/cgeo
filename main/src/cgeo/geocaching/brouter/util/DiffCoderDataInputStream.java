/**
 * DataInputStream extended by varlength diff coding
 *
 * @author ab
 */
package cgeo.geocaching.brouter.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;


public final class DiffCoderDataInputStream extends DataInputStream {
    private final long[] lastValues = new long[10];

    public DiffCoderDataInputStream(final InputStream is) {
        super(is);
    }

    public long readDiffed(final int idx) throws IOException {
        final long d = readSigned();
        final long v = lastValues[idx] + d;
        lastValues[idx] = v;
        return v;
    }

    public long readSigned() throws IOException {
        final long v = readUnsigned();
        return (v & 1) == 0 ? v >> 1 : -(v >> 1);
    }

    public long readUnsigned() throws IOException {
        long v = 0;
        int shift = 0;
        for (; ; ) {
            final long i7 = readByte() & 0xff;
            v |= (i7 & 0x7f) << shift;
            if ((i7 & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        return v;
    }
}
