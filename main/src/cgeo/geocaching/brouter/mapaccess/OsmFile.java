/**
 * cache for a single square
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import cgeo.geocaching.brouter.codec.DataBuffers;
import cgeo.geocaching.brouter.codec.MicroCache;
import cgeo.geocaching.brouter.codec.MicroCache2;
import cgeo.geocaching.brouter.codec.StatCoderContext;
import cgeo.geocaching.brouter.codec.TagValueValidator;
import cgeo.geocaching.brouter.codec.WaypointMatcher;
import cgeo.geocaching.brouter.util.ByteDataReader;
import cgeo.geocaching.brouter.util.Crc32Utils;

import java.io.IOException;

final class OsmFile {
    public int lonDegree;
    public int latDegree;
    public String filename;
    private PhysicalFile rafile = null;
    private long fileOffset;
    private int[] posIdx;
    private MicroCache[] microCaches;
    private int divisor;
    private int cellsize;
    private int indexsize;

    OsmFile(final PhysicalFile rafile, final int lonDegree, final int latDegree, final DataBuffers dataBuffers) throws Exception {
        this.lonDegree = lonDegree;
        this.latDegree = latDegree;
        final int lonMod5 = lonDegree % 5;
        final int latMod5 = latDegree % 5;
        final int tileIndex = lonMod5 * 5 + latMod5;

        if (rafile != null) {
            divisor = rafile.divisor;

            cellsize = 1000000 / divisor;
            final int ncaches = divisor * divisor;
            indexsize = ncaches * 4;

            final byte[] iobuffer = dataBuffers.iobuffer;
            filename = rafile.fileName;

            final long[] index = rafile.fileIndex;
            fileOffset = tileIndex > 0 ? index[tileIndex - 1] : 200L;
            if (fileOffset == index[tileIndex]) {
                return; // empty
            }

            this.rafile = rafile;
            posIdx = new int[ncaches];
            microCaches = new MicroCache[ncaches];
            this.rafile.readFully(fileOffset, indexsize, iobuffer);

            if (rafile.fileHeaderCrcs != null) {
                final int headerCrc = Crc32Utils.crc(iobuffer, 0, indexsize);
                if (rafile.fileHeaderCrcs[tileIndex] != headerCrc) {
                    throw new IOException("sub index checksum error");
                }
            }

            final ByteDataReader dis = new ByteDataReader(iobuffer);
            for (int i = 0; i < ncaches; i++) {
                posIdx[i] = dis.readInt();
            }
        }
    }

    public boolean hasData() {
        return microCaches != null;
    }

    public MicroCache getMicroCache(final int ilon, final int ilat) {
        final int lonIdx = ilon / cellsize;
        final int latIdx = ilat / cellsize;
        final int subIdx = (latIdx - divisor * latDegree) * divisor + (lonIdx - divisor * lonDegree);
        return microCaches[subIdx];
    }

    public MicroCache createMicroCache(final int ilon, final int ilat, final DataBuffers dataBuffers, final TagValueValidator wayValidator, final WaypointMatcher waypointMatcher, final OsmNodesMap hollowNodes)
            throws Exception {
        final int lonIdx = ilon / cellsize;
        final int latIdx = ilat / cellsize;
        final MicroCache segment = createMicroCache(lonIdx, latIdx, dataBuffers, wayValidator, waypointMatcher, true, hollowNodes);
        final int subIdx = (latIdx - divisor * latDegree) * divisor + (lonIdx - divisor * lonDegree);
        microCaches[subIdx] = segment;
        return segment;
    }

    private int getPosIdx(final int idx) {
        return idx == -1 ? indexsize : posIdx[idx];
    }

    public int getDataInputForSubIdx(final int subIdx, final byte[] iobuffer) throws Exception {
        final int startPos = getPosIdx(subIdx - 1);
        final int endPos = getPosIdx(subIdx);
        final int size = endPos - startPos;
        if (size > 0 && size <= iobuffer.length) {
            this.rafile.readFully(fileOffset + startPos, size, iobuffer);
        }
        return size;
    }

    public MicroCache createMicroCache(final int lonIdx, final int latIdx, final DataBuffers dataBuffers, final TagValueValidator wayValidator,
                                       final WaypointMatcher waypointMatcher, final boolean reallyDecode, final OsmNodesMap hollowNodes) throws Exception {
        final int subIdx = (latIdx - divisor * latDegree) * divisor + (lonIdx - divisor * lonDegree);

        byte[] ab = dataBuffers.iobuffer;
        int asize = getDataInputForSubIdx(subIdx, ab);

        if (asize == 0) {
            return MicroCache.emptyCache();
        }
        if (asize > ab.length) {
            ab = new byte[asize];
            asize = getDataInputForSubIdx(subIdx, ab);
        }

        final StatCoderContext bc = new StatCoderContext(ab);

        try {
            if (!reallyDecode) {
                return null;
            }
            if (hollowNodes == null) {
                return new MicroCache2(bc, dataBuffers, lonIdx, latIdx, divisor, wayValidator, waypointMatcher);
            }
            new DirectWeaver(bc, dataBuffers, lonIdx, latIdx, divisor, wayValidator, waypointMatcher, hollowNodes);
            return MicroCache.emptyNonVirgin;
        } finally {
            // crc check only if the buffer has not been fully read
            final int readBytes = (bc.getReadingBitPosition() + 7) >> 3;
            if (readBytes != asize - 4) {
                final int crcData = Crc32Utils.crc(ab, 0, asize - 4);
                final int crcFooter = new ByteDataReader(ab, asize - 4).readInt();
                if (crcData == crcFooter) {
                    throw new IOException("old, unsupported data-format");
                } else if ((crcData ^ 2) != crcFooter) {
                    throw new IOException("checkum error");
                }
            }
        }
    }

    // set this OsmFile to ghost-state:
    public long setGhostState() {
        long sum = 0;
        final int nc = microCaches == null ? 0 : microCaches.length;
        for (int i = 0; i < nc; i++) {
            final MicroCache mc = microCaches[i];
            if (mc == null) {
                continue;
            }
            if (mc.virgin) {
                mc.ghost = true;
                sum += mc.getDataSize();
            } else {
                microCaches[i] = null;
            }
        }
        return sum;
    }

    public long collectAll() {
        long deleted = 0;
        final int nc = microCaches == null ? 0 : microCaches.length;
        for (int i = 0; i < nc; i++) {
            final MicroCache mc = microCaches[i];
            if (mc == null) {
                continue;
            }
            if (!mc.ghost) {
                deleted += mc.collect(0);
            }
        }
        return deleted;
    }

    public long cleanGhosts() {
        final long deleted = 0;
        final int nc = microCaches == null ? 0 : microCaches.length;
        for (int i = 0; i < nc; i++) {
            final MicroCache mc = microCaches[i];
            if (mc == null) {
                continue;
            }
            if (mc.ghost) {
                microCaches[i] = null;
            }
        }
        return deleted;
    }

    public void clean(final boolean all) {
        final int nc = microCaches == null ? 0 : microCaches.length;
        for (int i = 0; i < nc; i++) {
            final MicroCache mc = microCaches[i];
            if (mc == null) {
                continue;
            }
            if (all || !mc.virgin) {
                microCaches[i] = null;
            }
        }
    }
}
