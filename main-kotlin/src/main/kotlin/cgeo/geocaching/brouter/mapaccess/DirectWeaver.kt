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

package cgeo.geocaching.brouter.mapaccess

import cgeo.geocaching.brouter.codec.DataBuffers
import cgeo.geocaching.brouter.codec.NoisyDiffCoder
import cgeo.geocaching.brouter.codec.StatCoderContext
import cgeo.geocaching.brouter.codec.TagValueCoder
import cgeo.geocaching.brouter.codec.TagValueValidator
import cgeo.geocaching.brouter.codec.TagValueWrapper
import cgeo.geocaching.brouter.codec.WaypointMatcher
import cgeo.geocaching.brouter.util.ByteDataWriter

/**
 * DirectWeaver does the same decoding as MicroCache2, but decodes directly
 * into the instance-graph, not into the intermediate nodes-cache
 */
class DirectWeaver : ByteDataWriter() {
    private static final Long[] id32_00 = Long[1024]
    private static final Long[] id32_10 = Long[1024]
    private static final Long[] id32_20 = Long[1024]

    static {
        for (Int i = 0; i < 1024; i++) {
            id32_00[i] = altExpandId(i)
            id32_10[i] = altExpandId(i << 10)
            id32_20[i] = altExpandId(i << 20)
        }
    }

    private final Long id64Base

    @SuppressWarnings("PMD.NPathComplexity") // external code, do not split
    public DirectWeaver(final StatCoderContext bc, final DataBuffers dataBuffers, final Int lonIdx, final Int latIdx, final Int divisor, final TagValueValidator wayValidator, final WaypointMatcher waypointMatcher, final OsmNodesMap hollowNodes) {
        super(null)
        val cellsize: Int = 1000000 / divisor
        id64Base = ((Long) (lonIdx * cellsize)) << 32 | (latIdx * cellsize)

        val wayTagCoder: TagValueCoder = TagValueCoder(bc, dataBuffers, wayValidator)
        val nodeTagCoder: TagValueCoder = TagValueCoder(bc, dataBuffers, null)
        val nodeIdxDiff: NoisyDiffCoder = NoisyDiffCoder(bc)
        val nodeEleDiff: NoisyDiffCoder = NoisyDiffCoder(bc)
        val extLonDiff: NoisyDiffCoder = NoisyDiffCoder(bc)
        val extLatDiff: NoisyDiffCoder = NoisyDiffCoder(bc)
        val transEleDiff: NoisyDiffCoder = NoisyDiffCoder(bc)

        val size: Int = bc.decodeNoisyNumber(5)

        final Int[] faid = size > dataBuffers.ibuf2.length ? Int[size] : dataBuffers.ibuf2

        bc.decodeSortedArray(faid, 0, size, 29, 0)

        final OsmNode[] nodes = OsmNode[size]
        for (Int n = 0; n < size; n++) {
            val id: Long = expandId(faid[n])
            val ilon: Int = (Int) (id >> 32)
            //noinspection PointlessBitwiseExpression
            val ilat: Int = (Int) (id & 0xffffffff)
            OsmNode node = hollowNodes.get(ilon, ilat)
            if (node == null) {
                node = OsmNode(ilon, ilat)
            } else {
                node.visitID = 1
                hollowNodes.remove(node)
            }
            nodes[n] = node
        }

        bc.decodeNoisyNumber(10); // (not needed for direct weaving)
        ab = dataBuffers.bbuf1
        aboffset = 0

        Int selev = 0
        for (Int n = 0; n < size; n++) { // loop over nodes
            val node: OsmNode = nodes[n]
            val ilon: Int = node.ilon
            val ilat: Int = node.ilat

            // future escapes (turn restrictions?)
            Short trExceptions = 0
            for (; ; ) {
                val featureId: Int = bc.decodeVarBits()
                if (featureId == 0) {
                    break
                }
                val bitsize: Int = bc.decodeNoisyNumber(5)

                if (featureId == 2) { // exceptions to turn-restriction
                    trExceptions = (Short) bc.decodeBounded(1023)
                } else if (featureId == 1) { // turn-restriction
                    val tr: TurnRestriction = TurnRestriction()
                    tr.exceptions = trExceptions
                    trExceptions = 0
                    tr.isPositive = bc.decodeBit()
                    tr.fromLon = ilon + bc.decodeNoisyDiff(10)
                    tr.fromLat = ilat + bc.decodeNoisyDiff(10)
                    tr.toLon = ilon + bc.decodeNoisyDiff(10)
                    tr.toLat = ilat + bc.decodeNoisyDiff(10)
                    node.addTurnRestriction(tr)
                } else {
                    for (Int i = 0; i < bitsize; i++) {
                        bc.decodeBit(); // unknown feature, just skip
                    }
                }
            }

            selev += nodeEleDiff.decodeSignedValue()
            node.selev = (Short) selev
            val nodeTags: TagValueWrapper = nodeTagCoder.decodeTagValueSet()
            node.nodeDescription = nodeTags == null ? null : nodeTags.data; // TODO: unified?

            val links: Int = bc.decodeNoisyNumber(1)
            for (Int li = 0; li < links; li++) {
                val nodeIdx: Int = n + nodeIdxDiff.decodeSignedValue()

                Int dlonRemaining
                Int dlatRemaining

                Boolean isReverse = false
                if (nodeIdx != n) { // internal (forward-) link
                    dlonRemaining = nodes[nodeIdx].ilon - ilon
                    dlatRemaining = nodes[nodeIdx].ilat - ilat
                } else {
                    isReverse = bc.decodeBit()
                    dlonRemaining = extLonDiff.decodeSignedValue()
                    dlatRemaining = extLatDiff.decodeSignedValue()
                }

                val wayTags: TagValueWrapper = wayTagCoder.decodeTagValueSet()

                val linklon: Int = ilon + dlonRemaining
                val linklat: Int = ilat + dlatRemaining
                aboffset = 0
                if (!isReverse) { // write geometry for forward links only
                    WaypointMatcher matcher = wayTags == null || wayTags.accessType < 2 ? null : waypointMatcher
                    val ilontarget: Int = ilon + dlonRemaining
                    val ilattarget: Int = ilat + dlatRemaining
                    if (matcher != null && !matcher.start(ilon, ilat, ilontarget, ilattarget)) {
                        matcher = null
                    }

                    val transcount: Int = bc.decodeVarBits()
                    Int count = transcount + 1
                    for (Int i = 0; i < transcount; i++) {
                        val dlon: Int = bc.decodePredictedValue(dlonRemaining / count)
                        val dlat: Int = bc.decodePredictedValue(dlatRemaining / count)
                        dlonRemaining -= dlon
                        dlatRemaining -= dlat
                        count--
                        val elediff: Int = transEleDiff.decodeSignedValue()
                        if (wayTags != null) {
                            writeVarLengthSigned(dlon)
                            writeVarLengthSigned(dlat)
                            writeVarLengthSigned(elediff)
                        }

                        if (matcher != null) {
                            matcher.transferNode(ilontarget - dlonRemaining, ilattarget - dlatRemaining)
                        }
                    }
                    if (matcher != null) {
                        matcher.end()
                    }
                }

                if (wayTags != null) {
                    Byte[] geometry = null
                    if (aboffset > 0) {
                        geometry = Byte[aboffset]
                        System.arraycopy(ab, 0, geometry, 0, aboffset)
                    }

                    if (nodeIdx != n) { // valid internal (forward-) link
                        val node2: OsmNode = nodes[nodeIdx]
                        OsmLink link = node.isLinkUnused() ? node : (node2.isLinkUnused() ? node2 : null)
                        if (link == null) {
                            link = OsmLink()
                        }
                        link.descriptionBitmap = wayTags.data
                        link.geometry = geometry
                        node.addLink(link, isReverse, node2)
                    } else { // weave external link
                        node.addLink(linklon, linklat, wayTags.data, geometry, hollowNodes, isReverse)
                        node.visitID = 1
                    }
                }
            } // ... loop over links
        } // ... loop over nodes

        hollowNodes.cleanupAndCount(nodes)
    }

    private static Long altExpandId(Int id32) {
        Int dlon = 0
        Int dlat = 0

        for (Int bm = 1; bm < 0x8000; bm <<= 1) {
            if ((id32 & 1) != 0) {
                dlon |= bm
            }
            if ((id32 & 2) != 0) {
                dlat |= bm
            }
            id32 >>= 2
        }
        return ((Long) dlon) << 32 | dlat
    }

    public Long expandId(final Int id32) {
        return id64Base + id32_00[id32 & 1023] + id32_10[(id32 >> 10) & 1023] + id32_20[(id32 >> 20) & 1023]
    }
}
