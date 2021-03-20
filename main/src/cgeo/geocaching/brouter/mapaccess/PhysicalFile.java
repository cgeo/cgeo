/**
 * cache for a single square
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import cgeo.geocaching.brouter.codec.DataBuffers;
import cgeo.geocaching.brouter.codec.MicroCache;
import cgeo.geocaching.brouter.util.ByteDataReader;
import cgeo.geocaching.brouter.util.Crc32;

final public class PhysicalFile {
    public long creationTime;
    public int divisor = 80;
    RandomAccessFile ra = null;
    long[] fileIndex = new long[25];
    int[] fileHeaderCrcs;
    String fileName;
    private final int fileIndexCrc;

    public PhysicalFile(File f, DataBuffers dataBuffers, int lookupVersion, int lookupMinorVersion) throws Exception {
        fileName = f.getName();
        byte[] iobuffer = dataBuffers.iobuffer;
        ra = new RandomAccessFile(f, "r");
        ra.readFully(iobuffer, 0, 200);
        fileIndexCrc = Crc32.crc(iobuffer, 0, 200);
        ByteDataReader dis = new ByteDataReader(iobuffer);
        for (int i = 0; i < 25; i++) {
            long lv = dis.readLong();
            short readVersion = (short) (lv >> 48);
            if (i == 0 && lookupVersion != -1 && readVersion != lookupVersion) {
                throw new IllegalArgumentException("lookup version mismatch (old rd5?) lookups.dat="
                    + lookupVersion + " " + f.getAbsolutePath() + "=" + readVersion);
            }
            fileIndex[i] = lv & 0xffffffffffffL;
        }

        // read some extra info from the end of the file, if present
        long len = ra.length();

        long pos = fileIndex[24];
        int extraLen = 8 + 26 * 4;

        if (len == pos)
            return; // old format o.k.

        if (len < pos + extraLen) // > is o.k. for future extensions!
        {
            throw new IOException("file of size " + len + " too short, should be " + (pos + extraLen));
        }

        ra.seek(pos);
        ra.readFully(iobuffer, 0, extraLen);
        dis = new ByteDataReader(iobuffer);
        creationTime = dis.readLong();

        int crcData = dis.readInt();
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

    public static void main(String[] args) {
        MicroCache.debug = true;

        String message = checkFileIntegrity(new File(args[0]));

        if (message != null) {
            System.out.println("************************************");
            System.out.println(message);
            System.out.println("************************************");
        }
    }

    /**
     * Checks the integrity of the file using the build-in checksums
     *
     * @return the error message if file corrupt, else null
     */
    public static String checkFileIntegrity(File f) {
        PhysicalFile pf = null;
        try {
            DataBuffers dataBuffers = new DataBuffers();
            pf = new PhysicalFile(f, dataBuffers, -1, -1);
            int div = pf.divisor;
            for (int lonDegree = 0; lonDegree < 5; lonDegree++) // does'nt really matter..
            {
                for (int latDegree = 0; latDegree < 5; latDegree++) // ..where on earth we are
                {
                    OsmFile osmf = new OsmFile(pf, lonDegree, latDegree, dataBuffers);
                    if (osmf.hasData())
                        for (int lonIdx = 0; lonIdx < div; lonIdx++)
                            for (int latIdx = 0; latIdx < div; latIdx++)
                                osmf.createMicroCache(lonDegree * div + lonIdx, latDegree * div + latIdx, dataBuffers, null, null, MicroCache.debug, null);
                }
            }
        } catch (IllegalArgumentException iae) {
            return iae.getMessage();
        } catch (Exception e) {
            return e.toString();
        } finally {
            if (pf != null)
                try {
                    pf.ra.close();
                } catch (Exception ee) {
                }
        }
        return null;
    }
}
