package cgeo.geocaching.brouter.codec;

import java.util.HashMap;

import cgeo.geocaching.brouter.util.ByteDataReader;
import cgeo.geocaching.brouter.util.IByteArrayUnifier;

/**
 * MicroCache2 is the new format that uses statistical encoding and
 * is able to do access filtering and waypoint matching during encoding
 */
public final class MicroCache2 extends MicroCache {
    private final int lonBase;
    private final int latBase;
    private final int cellsize;

    public MicroCache2(int size, byte[] databuffer, int lonIdx, int latIdx, int divisor) throws Exception {
        super(databuffer); // sets ab=databuffer, aboffset=0

        faid = new int[size];
        fapos = new int[size];
        this.size = 0;
        cellsize = 1000000 / divisor;
        lonBase = lonIdx * cellsize;
        latBase = latIdx * cellsize;
    }

    public MicroCache2(StatCoderContext bc, DataBuffers dataBuffers, int lonIdx, int latIdx, int divisor, TagValueValidator wayValidator, WaypointMatcher waypointMatcher) throws Exception {
        super(null);
        cellsize = 1000000 / divisor;
        lonBase = lonIdx * cellsize;
        latBase = latIdx * cellsize;

        TagValueCoder wayTagCoder = new TagValueCoder(bc, dataBuffers, wayValidator);
        TagValueCoder nodeTagCoder = new TagValueCoder(bc, dataBuffers, null);
        NoisyDiffCoder nodeIdxDiff = new NoisyDiffCoder(bc);
        NoisyDiffCoder nodeEleDiff = new NoisyDiffCoder(bc);
        NoisyDiffCoder extLonDiff = new NoisyDiffCoder(bc);
        NoisyDiffCoder extLatDiff = new NoisyDiffCoder(bc);
        NoisyDiffCoder transEleDiff = new NoisyDiffCoder(bc);

        size = bc.decodeNoisyNumber(5);
        faid = size > dataBuffers.ibuf2.length ? new int[size] : dataBuffers.ibuf2;
        fapos = size > dataBuffers.ibuf3.length ? new int[size] : dataBuffers.ibuf3;


        int[] alon = size > dataBuffers.alon.length ? new int[size] : dataBuffers.alon;
        int[] alat = size > dataBuffers.alat.length ? new int[size] : dataBuffers.alat;

        if (debug)
            System.out.println("*** decoding cache of size=" + size + " for lonIdx=" + lonIdx + " latIdx=" + latIdx);

        bc.decodeSortedArray(faid, 0, size, 29, 0);

        for (int n = 0; n < size; n++) {
            long id64 = expandId(faid[n]);
            alon[n] = (int) (id64 >> 32);
            alat[n] = (int) (id64 & 0xffffffff);
        }

        int netdatasize = bc.decodeNoisyNumber(10);
        ab = netdatasize > dataBuffers.bbuf1.length ? new byte[netdatasize] : dataBuffers.bbuf1;
        aboffset = 0;

        int[] validBits = new int[(size + 31) >> 5];

        int finaldatasize = 0;

        LinkedListContainer reverseLinks = new LinkedListContainer(size, dataBuffers.ibuf1);

        int selev = 0;
        for (int n = 0; n < size; n++) // loop over nodes
        {
            int ilon = alon[n];
            int ilat = alat[n];

            // future escapes (turn restrictions?)
            short trExceptions = 0;
            int featureId = bc.decodeVarBits();
            if (featureId == 13) {
                fapos[n] = aboffset;
                validBits[n >> 5] |= 1 << n; // mark dummy-node valid
                continue; // empty node escape (delta files only)
            }
            while (featureId != 0) {
                int bitsize = bc.decodeNoisyNumber(5);

                if (featureId == 2) // exceptions to turn-restriction
                {
                    trExceptions = (short) bc.decodeBounded(1023);
                } else if (featureId == 1) // turn-restriction
                {
                    writeBoolean(true);
                    writeShort(trExceptions); // exceptions from previous feature
                    trExceptions = 0;

                    writeBoolean(bc.decodeBit()); // isPositive
                    writeInt(ilon + bc.decodeNoisyDiff(10)); // fromLon
                    writeInt(ilat + bc.decodeNoisyDiff(10)); // fromLat
                    writeInt(ilon + bc.decodeNoisyDiff(10)); // toLon
                    writeInt(ilat + bc.decodeNoisyDiff(10)); // toLat
                } else {
                    for (int i = 0; i < bitsize; i++)
                        bc.decodeBit(); // unknown feature, just skip
                }
                featureId = bc.decodeVarBits();
            }
            writeBoolean(false);

            selev += nodeEleDiff.decodeSignedValue();
            writeShort((short) selev);
            TagValueWrapper nodeTags = nodeTagCoder.decodeTagValueSet();
            writeVarBytes(nodeTags == null ? null : nodeTags.data);

            int links = bc.decodeNoisyNumber(1);
            if (debug)
                System.out.println("***   decoding node " + ilon + "/" + ilat + " with links=" + links);
            for (int li = 0; li < links; li++) {
                int sizeoffset = 0;
                int nodeIdx = n + nodeIdxDiff.decodeSignedValue();

                int dlon_remaining;
                int dlat_remaining;

                boolean isReverse = false;
                if (nodeIdx != n) // internal (forward-) link
                {
                    dlon_remaining = alon[nodeIdx] - ilon;
                    dlat_remaining = alat[nodeIdx] - ilat;
                } else {
                    isReverse = bc.decodeBit();
                    dlon_remaining = extLonDiff.decodeSignedValue();
                    dlat_remaining = extLatDiff.decodeSignedValue();
                }
                if (debug)
                    System.out.println("***     decoding link to " + (ilon + dlon_remaining) + "/" + (ilat + dlat_remaining) + " extern=" + (nodeIdx == n));

                TagValueWrapper wayTags = wayTagCoder.decodeTagValueSet();

                boolean linkValid = wayTags != null || wayValidator == null;
                if (linkValid) {
                    int startPointer = aboffset;
                    sizeoffset = writeSizePlaceHolder();

                    writeVarLengthSigned(dlon_remaining);
                    writeVarLengthSigned(dlat_remaining);

                    validBits[n >> 5] |= 1 << n; // mark source-node valid
                    if (nodeIdx != n) // valid internal (forward-) link
                    {
                        reverseLinks.addDataElement(nodeIdx, n); // register reverse link
                        finaldatasize += 1 + aboffset - startPointer; // reserve place for reverse
                        validBits[nodeIdx >> 5] |= 1 << nodeIdx; // mark target-node valid
                    }
                    writeModeAndDesc(isReverse, wayTags == null ? null : wayTags.data);
                }

                if (!isReverse) // write geometry for forward links only
                {
                    WaypointMatcher matcher = wayTags == null || wayTags.accessType < 2 ? null : waypointMatcher;
                    int ilontarget = ilon + dlon_remaining;
                    int ilattarget = ilat + dlat_remaining;
                    if (matcher != null) {
                        if (!matcher.start(ilon, ilat, ilontarget, ilattarget)) {
                            matcher = null;
                        }
                    }

                    int transcount = bc.decodeVarBits();
                    if (debug)
                        System.out.println("***       decoding geometry with count=" + transcount);
                    int count = transcount + 1;
                    for (int i = 0; i < transcount; i++) {
                        int dlon = bc.decodePredictedValue(dlon_remaining / count);
                        int dlat = bc.decodePredictedValue(dlat_remaining / count);
                        dlon_remaining -= dlon;
                        dlat_remaining -= dlat;
                        count--;
                        int elediff = transEleDiff.decodeSignedValue();
                        if (wayTags != null) {
                            writeVarLengthSigned(dlon);
                            writeVarLengthSigned(dlat);
                            writeVarLengthSigned(elediff);
                        }

                        if (matcher != null)
                            matcher.transferNode(ilontarget - dlon_remaining, ilattarget - dlat_remaining);
                    }
                    if (matcher != null)
                        matcher.end();
                }
                if (linkValid) {
                    injectSize(sizeoffset);
                }
            }
            fapos[n] = aboffset;
        }

        // calculate final data size
        int finalsize = 0;
        int startpos = 0;
        for (int i = 0; i < size; i++) {
            int endpos = fapos[i];
            if ((validBits[i >> 5] & (1 << i)) != 0) {
                finaldatasize += endpos - startpos;
                finalsize++;
            }
            startpos = endpos;
        }
        // append the reverse links at the end of each node
        byte[] abOld = ab;
        int[] faidOld = faid;
        int[] faposOld = fapos;
        int sizeOld = size;
        ab = new byte[finaldatasize];
        faid = new int[finalsize];
        fapos = new int[finalsize];
        aboffset = 0;
        size = 0;

        startpos = 0;
        for (int n = 0; n < sizeOld; n++) {
            int endpos = faposOld[n];
            if ((validBits[n >> 5] & (1 << n)) != 0) {
                int len = endpos - startpos;
                System.arraycopy(abOld, startpos, ab, aboffset, len);
                if (debug)
                    System.out.println("*** copied " + len + " bytes from " + aboffset + " for node " + n);
                aboffset += len;

                int cnt = reverseLinks.initList(n);
                if (debug)
                    System.out.println("*** appending " + cnt + " reverse links for node " + n);

                for (int ri = 0; ri < cnt; ri++) {
                    int nodeIdx = reverseLinks.getDataElement();
                    int sizeoffset = writeSizePlaceHolder();
                    writeVarLengthSigned(alon[nodeIdx] - alon[n]);
                    writeVarLengthSigned(alat[nodeIdx] - alat[n]);
                    writeModeAndDesc(true, null);
                    injectSize(sizeoffset);
                }
                faid[size] = faidOld[n];
                fapos[size] = aboffset;
                size++;
            }
            startpos = endpos;
        }
        init(size);
    }

    public byte[] readUnified(int len, IByteArrayUnifier u) {
        byte[] b = u.unify(ab, aboffset, len);
        aboffset += len;
        return b;
    }

    @Override
    public long expandId(int id32) {
        int dlon = 0;
        int dlat = 0;

        for (int bm = 1; bm < 0x8000; bm <<= 1) {
            if ((id32 & 1) != 0)
                dlon |= bm;
            if ((id32 & 2) != 0)
                dlat |= bm;
            id32 >>= 2;
        }

        int lon32 = lonBase + dlon;
        int lat32 = latBase + dlat;

        return ((long) lon32) << 32 | lat32;
    }

    @Override
    public int shrinkId(long id64) {
        int lon32 = (int) (id64 >> 32);
        int lat32 = (int) (id64 & 0xffffffff);
        int dlon = lon32 - lonBase;
        int dlat = lat32 - latBase;
        int id32 = 0;

        for (int bm = 0x4000; bm > 0; bm >>= 1) {
            id32 <<= 2;
            if ((dlon & bm) != 0)
                id32 |= 1;
            if ((dlat & bm) != 0)
                id32 |= 2;
        }
        return id32;
    }

    @Override
    public boolean isInternal(int ilon, int ilat) {
        return ilon >= lonBase && ilon < lonBase + cellsize
            && ilat >= latBase && ilat < latBase + cellsize;
    }

    @Override
    public int encodeMicroCache(byte[] buffer) {
        HashMap<Long, Integer> idMap = new HashMap<Long, Integer>();
        for (int n = 0; n < size; n++) // loop over nodes
        {
            idMap.put(Long.valueOf(expandId(faid[n])), Integer.valueOf(n));
        }

        IntegerFifo3Pass linkCounts = new IntegerFifo3Pass(256);
        IntegerFifo3Pass transCounts = new IntegerFifo3Pass(256);
        IntegerFifo3Pass restrictionBits = new IntegerFifo3Pass(16);

        TagValueCoder wayTagCoder = new TagValueCoder();
        TagValueCoder nodeTagCoder = new TagValueCoder();
        NoisyDiffCoder nodeIdxDiff = new NoisyDiffCoder();
        NoisyDiffCoder nodeEleDiff = new NoisyDiffCoder();
        NoisyDiffCoder extLonDiff = new NoisyDiffCoder();
        NoisyDiffCoder extLatDiff = new NoisyDiffCoder();
        NoisyDiffCoder transEleDiff = new NoisyDiffCoder();

        int netdatasize = 0;

        for (int pass = 1; ; pass++) // 3 passes: counters, stat-collection, encoding
        {
            boolean dostats = pass == 3;
            boolean dodebug = debug && pass == 3;

            if (pass < 3)
                netdatasize = fapos[size - 1];

            StatCoderContext bc = new StatCoderContext(buffer);

            linkCounts.init();
            transCounts.init();
            restrictionBits.init();

            wayTagCoder.encodeDictionary(bc);
            if (dostats)
                bc.assignBits("wayTagDictionary");
            nodeTagCoder.encodeDictionary(bc);
            if (dostats)
                bc.assignBits("nodeTagDictionary");
            nodeIdxDiff.encodeDictionary(bc);
            nodeEleDiff.encodeDictionary(bc);
            extLonDiff.encodeDictionary(bc);
            extLatDiff.encodeDictionary(bc);
            transEleDiff.encodeDictionary(bc);
            if (dostats)
                bc.assignBits("noisebits");
            bc.encodeNoisyNumber(size, 5);
            if (dostats)
                bc.assignBits("nodecount");
            bc.encodeSortedArray(faid, 0, size, 0x20000000, 0);
            if (dostats)
                bc.assignBits("node-positions");
            bc.encodeNoisyNumber(netdatasize, 10); // net-size
            if (dostats)
                bc.assignBits("netdatasize");
            if (dodebug)
                System.out.println("*** encoding cache of size=" + size);
            int lastSelev = 0;

            for (int n = 0; n < size; n++) // loop over nodes
            {
                aboffset = startPos(n);
                aboffsetEnd = fapos[n];
                if (dodebug)
                    System.out.println("*** encoding node " + n + " from " + aboffset + " to " + aboffsetEnd);

                long id64 = expandId(faid[n]);
                int ilon = (int) (id64 >> 32);
                int ilat = (int) (id64 & 0xffffffff);

                if (aboffset == aboffsetEnd) {
                    bc.encodeVarBits(13); // empty node escape (delta files only)
                    continue;
                }

                // write turn restrictions
                while (readBoolean()) {
                    short exceptions = readShort(); // except bikes, psv, ...
                    if (exceptions != 0) {
                        bc.encodeVarBits(2); // 2 = tr exceptions
                        bc.encodeNoisyNumber(10, 5); // bit-count
                        bc.encodeBounded(1023, exceptions & 1023);
                    }
                    bc.encodeVarBits(1); // 1 = turn restriction
                    bc.encodeNoisyNumber(restrictionBits.getNext(), 5); // bit-count using look-ahead fifo
                    long b0 = bc.getWritingBitPosition();
                    bc.encodeBit(readBoolean()); // isPositive
                    bc.encodeNoisyDiff(readInt() - ilon, 10); // fromLon
                    bc.encodeNoisyDiff(readInt() - ilat, 10); // fromLat
                    bc.encodeNoisyDiff(readInt() - ilon, 10); // toLon
                    bc.encodeNoisyDiff(readInt() - ilat, 10); // toLat
                    restrictionBits.add((int) (bc.getWritingBitPosition() - b0));
                }
                bc.encodeVarBits(0); // end of extra data

                if (dostats)
                    bc.assignBits("extradata");

                int selev = readShort();
                nodeEleDiff.encodeSignedValue(selev - lastSelev);
                if (dostats)
                    bc.assignBits("nodeele");
                lastSelev = selev;
                nodeTagCoder.encodeTagValueSet(readVarBytes());
                if (dostats)
                    bc.assignBits("nodeTagIdx");
                int nlinks = linkCounts.getNext();
                if (dodebug)
                    System.out.println("*** nlinks=" + nlinks);
                bc.encodeNoisyNumber(nlinks, 1);
                if (dostats)
                    bc.assignBits("link-counts");

                nlinks = 0;
                while (hasMoreData()) // loop over links
                {
                    // read link data
                    int startPointer = aboffset;
                    int endPointer = getEndPointer();

                    int ilonlink = ilon + readVarLengthSigned();
                    int ilatlink = ilat + readVarLengthSigned();

                    int sizecode = readVarLengthUnsigned();
                    boolean isReverse = (sizecode & 1) != 0;
                    int descSize = sizecode >> 1;
                    byte[] description = null;
                    if (descSize > 0) {
                        description = new byte[descSize];
                        readFully(description);
                    }

                    long link64 = ((long) ilonlink) << 32 | ilatlink;
                    Integer idx = idMap.get(Long.valueOf(link64));
                    boolean isInternal = idx != null;

                    if (isReverse && isInternal) {
                        if (dodebug)
                            System.out.println("*** NOT encoding link reverse=" + isReverse + " internal=" + isInternal);
                        netdatasize -= aboffset - startPointer;
                        continue; // do not encode internal reverse links
                    }
                    if (dodebug)
                        System.out.println("*** encoding link reverse=" + isReverse + " internal=" + isInternal);
                    nlinks++;

                    if (isInternal) {
                        int nodeIdx = idx.intValue();
                        if (dodebug)
                            System.out.println("*** target nodeIdx=" + nodeIdx);
                        if (nodeIdx == n)
                            throw new RuntimeException("ups: self ref?");
                        nodeIdxDiff.encodeSignedValue(nodeIdx - n);
                        if (dostats)
                            bc.assignBits("nodeIdx");
                    } else {
                        nodeIdxDiff.encodeSignedValue(0);
                        bc.encodeBit(isReverse);
                        extLonDiff.encodeSignedValue(ilonlink - ilon);
                        extLatDiff.encodeSignedValue(ilatlink - ilat);
                        if (dostats)
                            bc.assignBits("externalNode");
                    }
                    wayTagCoder.encodeTagValueSet(description);
                    if (dostats)
                        bc.assignBits("wayDescIdx");

                    if (!isReverse) {
                        byte[] geometry = readDataUntil(endPointer);
                        // write transition nodes
                        int count = transCounts.getNext();
                        if (dodebug)
                            System.out.println("*** encoding geometry with count=" + count);
                        bc.encodeVarBits(count++);
                        if (dostats)
                            bc.assignBits("transcount");
                        int transcount = 0;
                        if (geometry != null) {
                            int dlon_remaining = ilonlink - ilon;
                            int dlat_remaining = ilatlink - ilat;

                            ByteDataReader r = new ByteDataReader(geometry);
                            while (r.hasMoreData()) {
                                transcount++;

                                int dlon = r.readVarLengthSigned();
                                int dlat = r.readVarLengthSigned();
                                bc.encodePredictedValue(dlon, dlon_remaining / count);
                                bc.encodePredictedValue(dlat, dlat_remaining / count);
                                dlon_remaining -= dlon;
                                dlat_remaining -= dlat;
                                if (count > 1)
                                    count--;
                                if (dostats)
                                    bc.assignBits("transpos");
                                transEleDiff.encodeSignedValue(r.readVarLengthSigned());
                                if (dostats)
                                    bc.assignBits("transele");
                            }
                        }
                        transCounts.add(transcount);
                    }
                }
                linkCounts.add(nlinks);
            }
            if (pass == 3) {
                return bc.closeAndGetEncodedLength();
            }
        }
    }
}
