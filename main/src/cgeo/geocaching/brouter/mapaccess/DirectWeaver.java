package cgeo.geocaching.brouter.mapaccess;

import cgeo.geocaching.brouter.codec.DataBuffers;
import cgeo.geocaching.brouter.codec.NoisyDiffCoder;
import cgeo.geocaching.brouter.codec.StatCoderContext;
import cgeo.geocaching.brouter.codec.TagValueCoder;
import cgeo.geocaching.brouter.codec.TagValueValidator;
import cgeo.geocaching.brouter.codec.TagValueWrapper;
import cgeo.geocaching.brouter.codec.WaypointMatcher;
import cgeo.geocaching.brouter.util.ByteDataWriter;

/**
 * DirectWeaver does the same decoding as MicroCache2, but decodes directly
 * into the instance-graph, not into the intermediate nodes-cache
 */
public final class DirectWeaver extends ByteDataWriter {
    private static final long[] id32_00 = new long[1024];
    private static final long[] id32_10 = new long[1024];
    private static final long[] id32_20 = new long[1024];

    static {
        for (int i = 0; i < 1024; i++) {
            id32_00[i] = altExpandId(i);
            id32_10[i] = altExpandId(i << 10);
            id32_20[i] = altExpandId(i << 20);
        }
    }

    private final long id64Base;

    public DirectWeaver(final StatCoderContext bc, final DataBuffers dataBuffers, final int lonIdx, final int latIdx, final int divisor, final TagValueValidator wayValidator, final WaypointMatcher waypointMatcher, final OsmNodesMap hollowNodes) throws Exception {
        super(null);
        final int cellsize = 1000000 / divisor;
        id64Base = ((long) (lonIdx * cellsize)) << 32 | (latIdx * cellsize);

        final TagValueCoder wayTagCoder = new TagValueCoder(bc, dataBuffers, wayValidator);
        final TagValueCoder nodeTagCoder = new TagValueCoder(bc, dataBuffers, null);
        final NoisyDiffCoder nodeIdxDiff = new NoisyDiffCoder(bc);
        final NoisyDiffCoder nodeEleDiff = new NoisyDiffCoder(bc);
        final NoisyDiffCoder extLonDiff = new NoisyDiffCoder(bc);
        final NoisyDiffCoder extLatDiff = new NoisyDiffCoder(bc);
        final NoisyDiffCoder transEleDiff = new NoisyDiffCoder(bc);

        final int size = bc.decodeNoisyNumber(5);

        final int[] faid = size > dataBuffers.ibuf2.length ? new int[size] : dataBuffers.ibuf2;

        bc.decodeSortedArray(faid, 0, size, 29, 0);

        final OsmNode[] nodes = new OsmNode[size];
        for (int n = 0; n < size; n++) {
            final long id = expandId(faid[n]);
            final int ilon = (int) (id >> 32);
            final int ilat = (int) (id & 0xffffffff);
            OsmNode node = hollowNodes.get(ilon, ilat);
            if (node == null) {
                node = new OsmNode(ilon, ilat);
            } else {
                node.visitID = 1;
                hollowNodes.remove(node);
            }
            nodes[n] = node;
        }

        final int netdatasize = bc.decodeNoisyNumber(10); // (not needed for direct weaving)
        ab = dataBuffers.bbuf1;
        aboffset = 0;

        int selev = 0;
        for (int n = 0; n < size; n++) { // loop over nodes
            final OsmNode node = nodes[n];
            final int ilon = node.ilon;
            final int ilat = node.ilat;

            // future escapes (turn restrictions?)
            short trExceptions = 0;
            for (; ; ) {
                final int featureId = bc.decodeVarBits();
                if (featureId == 0) {
                    break;
                }
                final int bitsize = bc.decodeNoisyNumber(5);

                if (featureId == 2) { // exceptions to turn-restriction
                    trExceptions = (short) bc.decodeBounded(1023);
                } else if (featureId == 1) { // turn-restriction
                    final TurnRestriction tr = new TurnRestriction();
                    tr.exceptions = trExceptions;
                    trExceptions = 0;
                    tr.isPositive = bc.decodeBit();
                    tr.fromLon = ilon + bc.decodeNoisyDiff(10);
                    tr.fromLat = ilat + bc.decodeNoisyDiff(10);
                    tr.toLon = ilon + bc.decodeNoisyDiff(10);
                    tr.toLat = ilat + bc.decodeNoisyDiff(10);
                    node.addTurnRestriction(tr);
                } else {
                    for (int i = 0; i < bitsize; i++) {
                        bc.decodeBit(); // unknown feature, just skip
                    }
                }
            }

            selev += nodeEleDiff.decodeSignedValue();
            node.selev = (short) selev;
            final TagValueWrapper nodeTags = nodeTagCoder.decodeTagValueSet();
            node.nodeDescription = nodeTags == null ? null : nodeTags.data; // TODO: unified?

            final int links = bc.decodeNoisyNumber(1);
            for (int li = 0; li < links; li++) {
                final int nodeIdx = n + nodeIdxDiff.decodeSignedValue();

                int dlonRemaining;
                int dlatRemaining;

                boolean isReverse = false;
                if (nodeIdx != n) { // internal (forward-) link
                    dlonRemaining = nodes[nodeIdx].ilon - ilon;
                    dlatRemaining = nodes[nodeIdx].ilat - ilat;
                } else {
                    isReverse = bc.decodeBit();
                    dlonRemaining = extLonDiff.decodeSignedValue();
                    dlatRemaining = extLatDiff.decodeSignedValue();
                }

                final TagValueWrapper wayTags = wayTagCoder.decodeTagValueSet();

                final int linklon = ilon + dlonRemaining;
                final int linklat = ilat + dlatRemaining;
                aboffset = 0;
                if (!isReverse) { // write geometry for forward links only
                    WaypointMatcher matcher = wayTags == null || wayTags.accessType < 2 ? null : waypointMatcher;
                    final int ilontarget = ilon + dlonRemaining;
                    final int ilattarget = ilat + dlatRemaining;
                    if (matcher != null && !matcher.start(ilon, ilat, ilontarget, ilattarget)) {
                        matcher = null;
                    }

                    final int transcount = bc.decodeVarBits();
                    int count = transcount + 1;
                    for (int i = 0; i < transcount; i++) {
                        final int dlon = bc.decodePredictedValue(dlonRemaining / count);
                        final int dlat = bc.decodePredictedValue(dlatRemaining / count);
                        dlonRemaining -= dlon;
                        dlatRemaining -= dlat;
                        count--;
                        final int elediff = transEleDiff.decodeSignedValue();
                        if (wayTags != null) {
                            writeVarLengthSigned(dlon);
                            writeVarLengthSigned(dlat);
                            writeVarLengthSigned(elediff);
                        }

                        if (matcher != null) {
                            matcher.transferNode(ilontarget - dlonRemaining, ilattarget - dlatRemaining);
                        }
                    }
                    if (matcher != null) {
                        matcher.end();
                    }
                }

                if (wayTags != null) {
                    byte[] geometry = null;
                    if (aboffset > 0) {
                        geometry = new byte[aboffset];
                        System.arraycopy(ab, 0, geometry, 0, aboffset);
                    }

                    if (nodeIdx != n) { // valid internal (forward-) link
                        final OsmNode node2 = nodes[nodeIdx];
                        OsmLink link = node.isLinkUnused() ? node : (node2.isLinkUnused() ? node2 : null);
                        if (link == null) {
                            link = new OsmLink();
                        }
                        link.descriptionBitmap = wayTags.data;
                        link.geometry = geometry;
                        node.addLink(link, isReverse, node2);
                    } else { // weave external link
                        node.addLink(linklon, linklat, wayTags.data, geometry, hollowNodes, isReverse);
                        node.visitID = 1;
                    }
                }
            } // ... loop over links
        } // ... loop over nodes

        hollowNodes.cleanupAndCount(nodes);
    }

    private static long altExpandId(int id32) {
        int dlon = 0;
        int dlat = 0;

        for (int bm = 1; bm < 0x8000; bm <<= 1) {
            if ((id32 & 1) != 0) {
                dlon |= bm;
            }
            if ((id32 & 2) != 0) {
                dlat |= bm;
            }
            id32 >>= 2;
        }
        return ((long) dlon) << 32 | dlat;
    }

    public long expandId(final int id32) {
        return id64Base + id32_00[id32 & 1023] + id32_10[(id32 >> 10) & 1023] + id32_20[(id32 >> 20) & 1023];
    }
}
