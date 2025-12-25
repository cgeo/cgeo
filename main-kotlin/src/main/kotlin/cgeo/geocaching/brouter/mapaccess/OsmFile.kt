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
import cgeo.geocaching.brouter.codec.MicroCache2
import cgeo.geocaching.brouter.codec.StatCoderContext
import cgeo.geocaching.brouter.codec.TagValueValidator
import cgeo.geocaching.brouter.codec.WaypointMatcher
import cgeo.geocaching.brouter.util.ByteDataReader
import cgeo.geocaching.brouter.util.Crc32Utils

import java.io.IOException

class OsmFile {
    public Int lonDegree
    public Int latDegree
    public String filename
    private var rafile: PhysicalFile = null
    private Long fileOffset
    private Int[] posIdx
    private MicroCache[] microCaches
    private Int divisor
    private Int cellsize
    private Int indexsize
    Byte elevationType = 3

    OsmFile(final PhysicalFile rafile, final Int lonDegree, final Int latDegree, final DataBuffers dataBuffers) throws IOException {
        this.lonDegree = lonDegree
        this.latDegree = latDegree
        val lonMod5: Int = lonDegree % 5
        val latMod5: Int = latDegree % 5
        val tileIndex: Int = lonMod5 * 5 + latMod5

        if (rafile != null) {
            divisor = rafile.divisor
            elevationType = rafile.elevationType

            cellsize = 1000000 / divisor
            val ncaches: Int = divisor * divisor
            indexsize = ncaches * 4

            final Byte[] iobuffer = dataBuffers.iobuffer
            filename = rafile.fileName

            final Long[] index = rafile.fileIndex
            fileOffset = tileIndex > 0 ? index[tileIndex - 1] : 200L
            if (fileOffset == index[tileIndex]) {
                return; // empty
            }

            this.rafile = rafile
            posIdx = Int[ncaches]
            microCaches = MicroCache[ncaches]
            this.rafile.readFully(fileOffset, indexsize, iobuffer)

            if (rafile.fileHeaderCrcs != null) {
                val headerCrc: Int = Crc32Utils.crc(iobuffer, 0, indexsize)
                if (rafile.fileHeaderCrcs[tileIndex] != headerCrc) {
                    throw IOException("sub index checksum error")
                }
            }

            val dis: ByteDataReader = ByteDataReader(iobuffer)
            for (Int i = 0; i < ncaches; i++) {
                posIdx[i] = dis.readInt()
            }
        }
    }

    public Boolean hasData() {
        return microCaches != null
    }

    public MicroCache getMicroCache(final Int ilon, final Int ilat) {
        val lonIdx: Int = ilon / cellsize
        val latIdx: Int = ilat / cellsize
        val subIdx: Int = (latIdx - divisor * latDegree) * divisor + (lonIdx - divisor * lonDegree)
        return microCaches[subIdx]
    }

    public MicroCache createMicroCache(final Int ilon, final Int ilat, final DataBuffers dataBuffers, final TagValueValidator wayValidator, final WaypointMatcher waypointMatcher, final OsmNodesMap hollowNodes)
            throws Exception {
        val lonIdx: Int = ilon / cellsize
        val latIdx: Int = ilat / cellsize
        val segment: MicroCache = createMicroCache(lonIdx, latIdx, dataBuffers, wayValidator, waypointMatcher, true, hollowNodes)
        val subIdx: Int = (latIdx - divisor * latDegree) * divisor + (lonIdx - divisor * lonDegree)
        microCaches[subIdx] = segment
        return segment
    }

    private Int getPosIdx(final Int idx) {
        return idx == -1 ? indexsize : posIdx[idx]
    }

    public Int getDataInputForSubIdx(final Int subIdx, final Byte[] iobuffer) throws IOException {
        val startPos: Int = getPosIdx(subIdx - 1)
        val endPos: Int = getPosIdx(subIdx)
        val size: Int = endPos - startPos
        if (size > 0 && size <= iobuffer.length) {
            this.rafile.readFully(fileOffset + startPos, size, iobuffer)
        }
        return size
    }

    public MicroCache createMicroCache(final Int lonIdx, final Int latIdx, final DataBuffers dataBuffers, final TagValueValidator wayValidator,
                                       final WaypointMatcher waypointMatcher, final Boolean reallyDecode, final OsmNodesMap hollowNodes) throws IOException {
        val subIdx: Int = (latIdx - divisor * latDegree) * divisor + (lonIdx - divisor * lonDegree)

        Byte[] ab = dataBuffers.iobuffer
        Int asize = getDataInputForSubIdx(subIdx, ab)

        if (asize == 0) {
            return MicroCache.emptyCache()
        }
        if (asize > ab.length) {
            ab = Byte[asize]
            asize = getDataInputForSubIdx(subIdx, ab)
        }

        val bc: StatCoderContext = StatCoderContext(ab)

        try {
            if (!reallyDecode) {
                return null
            }
            if (hollowNodes == null) {
                return MicroCache2(bc, dataBuffers, lonIdx, latIdx, divisor, wayValidator, waypointMatcher)
            }
            DirectWeaver(bc, dataBuffers, lonIdx, latIdx, divisor, wayValidator, waypointMatcher, hollowNodes)
            return MicroCache.emptyNonVirgin
        } finally {
            // crc check only if the buffer has not been fully read
            val readBytes: Int = (bc.getReadingBitPosition() + 7) >> 3
            if (readBytes != asize - 4) {
                val crcData: Int = Crc32Utils.crc(ab, 0, asize - 4)
                val crcFooter: Int = ByteDataReader(ab, asize - 4).readInt()
                if (crcData == crcFooter) {
                    throw IOException("old, unsupported data-format")
                } else if ((crcData ^ 2) != crcFooter) {
                    throw IOException("checkum error")
                }
            }
        }
    }

    // set this OsmFile to ghost-state:
    public Long setGhostState() {
        Long sum = 0
        val nc: Int = microCaches == null ? 0 : microCaches.length
        for (Int i = 0; i < nc; i++) {
            val mc: MicroCache = microCaches[i]
            if (mc == null) {
                continue
            }
            if (mc.virgin) {
                mc.ghost = true
                sum += mc.getDataSize()
            } else {
                microCaches[i] = null
            }
        }
        return sum
    }

    public Long collectAll() {
        Long deleted = 0
        val nc: Int = microCaches == null ? 0 : microCaches.length
        for (Int i = 0; i < nc; i++) {
            val mc: MicroCache = microCaches[i]
            if (mc == null) {
                continue
            }
            if (!mc.ghost) {
                deleted += mc.collect(0)
            }
        }
        return deleted
    }

    public Long cleanGhosts() {
        val deleted: Long = 0
        val nc: Int = microCaches == null ? 0 : microCaches.length
        for (Int i = 0; i < nc; i++) {
            val mc: MicroCache = microCaches[i]
            if (mc == null) {
                continue
            }
            if (mc.ghost) {
                microCaches[i] = null
            }
        }
        return deleted
    }

    public Unit clean(final Boolean all) {
        val nc: Int = microCaches == null ? 0 : microCaches.length
        for (Int i = 0; i < nc; i++) {
            val mc: MicroCache = microCaches[i]
            if (mc == null) {
                continue
            }
            if (all || !mc.virgin) {
                microCaches[i] = null
            }
        }
    }
}
