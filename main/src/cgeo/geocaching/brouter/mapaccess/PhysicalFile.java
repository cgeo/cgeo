/**
 * cache for a single square
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import cgeo.geocaching.brouter.codec.DataBuffers;
import cgeo.geocaching.brouter.codec.MicroCache;
import cgeo.geocaching.brouter.util.ByteDataReader;
import cgeo.geocaching.brouter.util.Crc32Utils;
import cgeo.geocaching.storage.FileByteReader;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public final class PhysicalFile implements Closeable {
    public long creationTime;
    public int divisor = 80;
    private FileByteReader fbr = null;
    long[] fileIndex = new long[25];
    int[] fileHeaderCrcs;
    String fileName;
    private final int fileIndexCrc;

    public PhysicalFile(final String fileName, final FileInputStream fis, final DataBuffers dataBuffers, final int lookupVersion) throws Exception {
        this.fileName = fileName;
        final byte[] iobuffer = dataBuffers.iobuffer;
        fbr = new FileByteReader(fis);
        fbr.readFully(0, 200, iobuffer);
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
        final long len = fbr.size();

        final long pos = fileIndex[24];
        final int extraLen = 8 + 26 * 4;

        if (len == pos) {
            return; // old format o.k.
        }

        if (len < pos + extraLen) { // > is o.k. for future extensions!
            throw new IOException("file of size " + len + " too short, should be " + (pos + extraLen));
        }

        fbr.readFully(pos, extraLen, iobuffer);
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
        this.fbr.readFully(startPos, length, buffer);
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(fbr);
    }

    /**
     * Checks the integrity of the file using the build-in checksums.
     *
     * * Provided FileInputStream will be closed
     * * Provided fileName is just for logging purposes
     *
     * @return the error message if file corrupt, else null
     */
    public static String checkTileDataIntegrity(final String fileName, final FileInputStream fis) {
        PhysicalFile pf = null;
        try {
            final DataBuffers dataBuffers = new DataBuffers();
            pf = new PhysicalFile(fileName, fis, dataBuffers, -1);
            final int div = pf.divisor;
            for (int lonDegree = 0; lonDegree < 5; lonDegree++) { // doesn't really matter..
                for (int latDegree = 0; latDegree < 5; latDegree++) { // ..where on earth we are
                    final OsmFile osmf = new OsmFile(pf, lonDegree, latDegree, dataBuffers);
                    if (osmf.hasData()) {
                        for (int lonIdx = 0; lonIdx < div; lonIdx++) {
                            for (int latIdx = 0; latIdx < div; latIdx++) {
                                osmf.createMicroCache(lonDegree * div + lonIdx, latDegree * div + latIdx, dataBuffers, null, null, MicroCache.debug, null);
                            }
                        }
                    }
                }
            }
        } catch (IllegalArgumentException iae) {
            return iae.getMessage();
        } catch (Exception e) {
            return e.toString();
        } finally {
            IOUtils.closeQuietly(pf);
        }
        return null;
    }

}
