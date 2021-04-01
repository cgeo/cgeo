/**
 * cache for a single square
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import androidx.annotation.NonNull;

import org.apache.commons.io.IOUtils;

import cgeo.geocaching.brouter.codec.DataBuffers;
import cgeo.geocaching.brouter.codec.MicroCache;
import cgeo.geocaching.brouter.util.ByteDataReader;
import cgeo.geocaching.brouter.util.Crc32Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public final class PhysicalFile {
    public long creationTime;
    public int divisor = 80;
    long[] fileIndex = new long[25];
    int[] fileHeaderCrcs;
    String fileName;
    private final int fileIndexCrc;

    private final FileChannel fileChannel;

    public PhysicalFile(final FileInputStream fis, final String name, final DataBuffers dataBuffers, final int lookupVersion, final int lookupMinorVersion) throws Exception {
        fileName = name;
        fileChannel = fis.getChannel();

        final byte[] iobuffer = dataBuffers.iobuffer;
        readFully(0, 200, iobuffer);
        fileIndexCrc = Crc32Utils.crc(iobuffer, 0, 200);
        ByteDataReader dis = new ByteDataReader(iobuffer);
        for (int i = 0; i < 25; i++) {
            final long lv = dis.readLong();
            final short readVersion = (short) (lv >> 48);
            if (i == 0 && lookupVersion != -1 && readVersion != lookupVersion) {
                throw new IllegalArgumentException("lookup version mismatch (old rd5?) lookups.dat="
                    + lookupVersion + " " + fileName + "=" + readVersion);
            }
            fileIndex[i] = lv & 0xffffffffffffL;
        }

        // read some extra info from the end of the file, if present
        final long len = fileChannel.size();

        final long pos = fileIndex[24];
        final int extraLen = 8 + 26 * 4;

        if (len == pos) {
            return; // old format o.k.
        }

        if (len < pos + extraLen) { // > is o.k. for future extensions!
            throw new IOException("file of size " + len + " too short, should be " + (pos + extraLen));
        }

        readFully(pos, extraLen, iobuffer);
        dis = new ByteDataReader(iobuffer);
        creationTime = dis.readLong();

        final int crcData = dis.readInt();
        if (crcData == fileIndexCrc) {
            divisor = 80; // old format
        } else if ((crcData ^ 2) == fileIndexCrc) {
            divisor = 32; // new format
        } else {
            throw new IOException("top index checksum error");
        }
        fileHeaderCrcs = new int[25];
        for (int i = 0; i < 25; i++) {
            fileHeaderCrcs[i] = dis.readInt();
        }
    }

    public void readFully(final long startPos, final int length, @NonNull final byte[] buffer) throws IOException {
        final int readBytes = readFile(this.fileChannel, startPos, length, buffer, 0);
        if (readBytes != length) {
            throw new IOException("Could not read requested number of bytes (" + buffer.length + "), read only " + readBytes + " bytges");
        }
    }

    private static int readFile(@NonNull final FileChannel channel, final long startPos, final int length, @NonNull final byte[] buffer, final int bufferOffset) throws IOException {
        if (bufferOffset + length >= buffer.length) {
            throw new IllegalArgumentException("Requested read length " + length + " will not fit in given buffer length " + buffer.length + " (offset: " + bufferOffset + ")");
        }

        final ByteBuffer bb = ByteBuffer.wrap(buffer, bufferOffset, length);
        return channel.read(bb, startPos);
    }

    public void close() {
        IOUtils.closeQuietly(fileChannel);
    }
}
