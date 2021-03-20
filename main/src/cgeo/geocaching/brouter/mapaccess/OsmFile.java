/**
 * cache for a single square
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import java.io.IOException;
import java.io.RandomAccessFile;

import cgeo.geocaching.brouter.codec.DataBuffers;
import cgeo.geocaching.brouter.codec.MicroCache;
import cgeo.geocaching.brouter.codec.MicroCache2;
import cgeo.geocaching.brouter.codec.StatCoderContext;
import cgeo.geocaching.brouter.codec.TagValueValidator;
import cgeo.geocaching.brouter.codec.WaypointMatcher;
import cgeo.geocaching.brouter.util.ByteDataReader;
import cgeo.geocaching.brouter.util.Crc32;

final class OsmFile {
    public int lonDegree;
    public int latDegree;
    public String filename;
    private RandomAccessFile is = null;
    private long fileOffset;
    private int[] posIdx;
    private MicroCache[] microCaches;
    private int divisor;
    private int cellsize;
    private int ncaches;
    private int indexsize;

    public OsmFile(PhysicalFile rafile, int lonDegree, int latDegree, DataBuffers dataBuffers) throws Exception {
        this.lonDegree = lonDegree;
        this.latDegree = latDegree;
        int lonMod5 = lonDegree % 5;
        int latMod5 = latDegree % 5;
        int tileIndex = lonMod5 * 5 + latMod5;

        if (rafile != null) {
            divisor = rafile.divisor;

            cellsize = 1000000 / divisor;
            ncaches = divisor * divisor;
            indexsize = ncaches * 4;

            byte[] iobuffer = dataBuffers.iobuffer;
            filename = rafile.fileName;

            long[] index = rafile.fileIndex;
            fileOffset = tileIndex > 0 ? index[tileIndex - 1] : 200L;
            if (fileOffset == index[tileIndex])
                return; // empty

            is = rafile.ra;
            posIdx = new int[ncaches];
            microCaches = new MicroCache[ncaches];
            is.seek(fileOffset);
            is.readFully(iobuffer, 0, indexsize);

            if (rafile.fileHeaderCrcs != null) {
                int headerCrc = Crc32.crc(iobuffer, 0, indexsize);
                if (rafile.fileHeaderCrcs[tileIndex] != headerCrc) {
                    throw new IOException("sub index checksum error");
                }
            }

            ByteDataReader dis = new ByteDataReader(iobuffer);
            for (int i = 0; i < ncaches; i++) {
                posIdx[i] = dis.readInt();
            }
        }
    }

    public boolean hasData() {
        return microCaches != null;
    }

    public MicroCache getMicroCache(int ilon, int ilat) {
        int lonIdx = ilon / cellsize;
        int latIdx = ilat / cellsize;
        int subIdx = (latIdx - divisor * latDegree) * divisor + (lonIdx - divisor * lonDegree);
        return microCaches[subIdx];
    }

    public MicroCache createMicroCache(int ilon, int ilat, DataBuffers dataBuffers, TagValueValidator wayValidator, WaypointMatcher waypointMatcher, OsmNodesMap hollowNodes)
        throws Exception {
        int lonIdx = ilon / cellsize;
        int latIdx = ilat / cellsize;
        MicroCache segment = createMicroCache(lonIdx, latIdx, dataBuffers, wayValidator, waypointMatcher, true, hollowNodes);
        int subIdx = (latIdx - divisor * latDegree) * divisor + (lonIdx - divisor * lonDegree);
        microCaches[subIdx] = segment;
        return segment;
    }

    private int getPosIdx(int idx) {
        return idx == -1 ? indexsize : posIdx[idx];
    }

    public int getDataInputForSubIdx(int subIdx, byte[] iobuffer) throws Exception {
        int startPos = getPosIdx(subIdx - 1);
        int endPos = getPosIdx(subIdx);
        int size = endPos - startPos;
        if (size > 0) {
            is.seek(fileOffset + startPos);
            if (size <= iobuffer.length) {
                is.readFully(iobuffer, 0, size);
            }
        }
        return size;
    }

    public MicroCache createMicroCache(int lonIdx, int latIdx, DataBuffers dataBuffers, TagValueValidator wayValidator,
                                       WaypointMatcher waypointMatcher, boolean reallyDecode, OsmNodesMap hollowNodes) throws Exception {
        int subIdx = (latIdx - divisor * latDegree) * divisor + (lonIdx - divisor * lonDegree);

        byte[] ab = dataBuffers.iobuffer;
        int asize = getDataInputForSubIdx(subIdx, ab);

        if (asize == 0) {
            return MicroCache.emptyCache();
        }
        if (asize > ab.length) {
            ab = new byte[asize];
            asize = getDataInputForSubIdx(subIdx, ab);
        }

        StatCoderContext bc = new StatCoderContext(ab);

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
            int readBytes = (bc.getReadingBitPosition() + 7) >> 3;
            if (readBytes != asize - 4) {
                int crcData = Crc32.crc(ab, 0, asize - 4);
                int crcFooter = new ByteDataReader(ab, asize - 4).readInt();
                if (crcData == crcFooter) {
                    throw new IOException("old, unsupported data-format");
                } else if ((crcData ^ 2) != crcFooter) {
                    throw new IOException("checkum error");
                }
            }
        }
    }

    // set this OsmFile to ghost-state:
    long setGhostState() {
        long sum = 0;
        int nc = microCaches == null ? 0 : microCaches.length;
        for (int i = 0; i < nc; i++) {
            MicroCache mc = microCaches[i];
            if (mc == null)
                continue;
            if (mc.virgin) {
                mc.ghost = true;
                sum += mc.getDataSize();
            } else {
                microCaches[i] = null;
            }
        }
        return sum;
    }

    long collectAll() {
        long deleted = 0;
        int nc = microCaches == null ? 0 : microCaches.length;
        for (int i = 0; i < nc; i++) {
            MicroCache mc = microCaches[i];
            if (mc == null)
                continue;
            if (!mc.ghost) {
                deleted += mc.collect(0);
            }
        }
        return deleted;
    }

    long cleanGhosts() {
        long deleted = 0;
        int nc = microCaches == null ? 0 : microCaches.length;
        for (int i = 0; i < nc; i++) {
            MicroCache mc = microCaches[i];
            if (mc == null)
                continue;
            if (mc.ghost) {
                microCaches[i] = null;
            }
        }
        return deleted;
    }

    void clean(boolean all) {
        int nc = microCaches == null ? 0 : microCaches.length;
        for (int i = 0; i < nc; i++) {
            MicroCache mc = microCaches[i];
            if (mc == null)
                continue;
            if (all || !mc.virgin) {
                microCaches[i] = null;
            }
        }
    }
}
