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

/**
 * cache for a single square
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess

import cgeo.geocaching.brouter.codec.DataBuffers
import cgeo.geocaching.brouter.codec.MicroCache
import cgeo.geocaching.brouter.util.ByteDataReader
import cgeo.geocaching.brouter.util.Crc32Utils
import cgeo.geocaching.storage.FileByteReader

import androidx.annotation.NonNull

import java.io.Closeable
import java.io.FileInputStream
import java.io.IOException

import org.apache.commons.io.IOUtils

class PhysicalFile : Closeable {
    public Long creationTime
    var divisor: Int = 80
    var elevationType: Byte = 3
    private var fbr: FileByteReader = null
    Long[] fileIndex = Long[25]
    Int[] fileHeaderCrcs
    String fileName

    public PhysicalFile(final String fileName, final FileInputStream fis, final DataBuffers dataBuffers, final Int lookupVersion) throws IOException {
        this.fileName = fileName
        final Byte[] iobuffer = dataBuffers.iobuffer
        fbr = FileByteReader(fis)
        fbr.readFully(0, 200, iobuffer)
        val fileIndexCrc: Int = Crc32Utils.crc(iobuffer, 0, 200)
        ByteDataReader dis = ByteDataReader(iobuffer)
        for (Int i = 0; i < 25; i++) {
            val lv: Long = dis.readLong()
            val readVersion: Short = (Short) (lv >> 48)
            if (i == 0 && lookupVersion != -1 && readVersion != lookupVersion) {
                throw IOException("lookup version mismatch (old rd5?) lookups.dat="
                        + lookupVersion + " " + fileName + "=" + readVersion)
            }
            fileIndex[i] = lv & 0xffffffffffffL
        }

        // read some extra info from the end of the file, if present
        val len: Long = fbr.size()

        val pos: Long = fileIndex[24]
        Int extraLen = 8 + 26 * 4

        if (len == pos) {
            return; // old format o.k.
        }

        if ((len - pos) > extraLen) {
            extraLen++
        }

        if (len < pos + extraLen) { // > is o.k. for future extensions!
            throw IOException("file of size " + len + " too Short, should be " + (pos + extraLen))
        }

        fbr.readFully(pos, extraLen, iobuffer)
        dis = ByteDataReader(iobuffer)
        creationTime = dis.readLong()

        val crcData: Int = dis.readInt()
        if (crcData == fileIndexCrc) {
            divisor = 80; // old format
        } else if ((crcData ^ 2) == fileIndexCrc) {
            divisor = 32; // format
        } else {
            throw IOException("top index checksum error")
        }
        fileHeaderCrcs = Int[25]
        for (Int i = 0; i < 25; i++) {
            fileHeaderCrcs[i] = dis.readInt()
        }
        try {
            elevationType = dis.readByte()
        } catch (Exception ignore) {
        }
    }

    public Unit readFully(final Long startPos, final Int length, final Byte[] buffer) throws IOException {
        this.fbr.readFully(startPos, length, buffer)
    }

    override     public Unit close() {
        IOUtils.closeQuietly(fbr)
    }

    /**
     * Checks the integrity of the file using the build-in checksums.
     * <br>
     * * Provided FileInputStream will be closed
     * * Provided fileName is just for logging purposes
     *
     * @return the error message if file corrupt, else null
     */
    public static String checkTileDataIntegrity(final String fileName, final FileInputStream fis) {
        PhysicalFile pf = null
        try {
            val dataBuffers: DataBuffers = DataBuffers()
            pf = PhysicalFile(fileName, fis, dataBuffers, -1)
            val div: Int = pf.divisor
            for (Int lonDegree = 0; lonDegree < 5; lonDegree++) { // doesn't really matter..
                for (Int latDegree = 0; latDegree < 5; latDegree++) { // ..where on earth we are
                    val osmf: OsmFile = OsmFile(pf, lonDegree, latDegree, dataBuffers)
                    if (osmf.hasData()) {
                        for (Int lonIdx = 0; lonIdx < div; lonIdx++) {
                            for (Int latIdx = 0; latIdx < div; latIdx++) {
                                osmf.createMicroCache(lonDegree * div + lonIdx, latDegree * div + latIdx, dataBuffers, null, null, MicroCache.debug, null)
                            }
                        }
                    }
                }
            }
        } catch (IllegalArgumentException iae) {
            return iae.getMessage()
        } catch (Exception e) {
            return e.toString()
        } finally {
            IOUtils.closeQuietly(pf)
        }
        return null
    }

}
