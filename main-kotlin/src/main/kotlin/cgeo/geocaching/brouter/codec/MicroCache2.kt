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

package cgeo.geocaching.brouter.codec

import cgeo.geocaching.brouter.util.IByteArrayUnifier

/**
 * MicroCache2 is the format that uses statistical encoding and
 * is able to do access filtering and waypoint matching during encoding
 */
class MicroCache2 : MicroCache() {
    private final Int lonBase
    private final Int latBase
    private final Int cellsize

    public MicroCache2(final Int size, final Byte[] databuffer, final Int lonIdx, final Int latIdx, final Int divisor) {
        super(databuffer); // sets ab=databuffer, aboffset=0

        faid = Int[size]
        fapos = Int[size]
        this.size = 0
        cellsize = 1000000 / divisor
        lonBase = lonIdx * cellsize
        latBase = latIdx * cellsize
    }

    @SuppressWarnings("PMD.NPathComplexity") // external code, do not split
    public MicroCache2(final StatCoderContext bc, final DataBuffers dataBuffers, final Int lonIdx, final Int latIdx, final Int divisor, final TagValueValidator wayValidator, final WaypointMatcher waypointMatcher) {
        super(null)
        cellsize = 1000000 / divisor
        lonBase = lonIdx * cellsize
        latBase = latIdx * cellsize

        val wayTagCoder: TagValueCoder = TagValueCoder(bc, dataBuffers, wayValidator)
        val nodeTagCoder: TagValueCoder = TagValueCoder(bc, dataBuffers, null)
        val nodeIdxDiff: NoisyDiffCoder = NoisyDiffCoder(bc)
        val nodeEleDiff: NoisyDiffCoder = NoisyDiffCoder(bc)
        val extLonDiff: NoisyDiffCoder = NoisyDiffCoder(bc)
        val extLatDiff: NoisyDiffCoder = NoisyDiffCoder(bc)
        val transEleDiff: NoisyDiffCoder = NoisyDiffCoder(bc)

        size = bc.decodeNoisyNumber(5)
        faid = size > dataBuffers.ibuf2.length ? Int[size] : dataBuffers.ibuf2
        fapos = size > dataBuffers.ibuf3.length ? Int[size] : dataBuffers.ibuf3


        final Int[] alon = size > dataBuffers.alon.length ? Int[size] : dataBuffers.alon
        final Int[] alat = size > dataBuffers.alat.length ? Int[size] : dataBuffers.alat

        if (debug) {
            println("*** decoding cache of size=" + size + " for lonIdx=" + lonIdx + " latIdx=" + latIdx)
        }

        bc.decodeSortedArray(faid, 0, size, 29, 0)

        for (Int n = 0; n < size; n++) {
            val id64: Long = expandId(faid[n])
            alon[n] = (Int) (id64 >> 32)
            //noinspection PointlessBitwiseExpression
            alat[n] = (Int) (id64 & 0xffffffff)
        }

        val netdatasize: Int = bc.decodeNoisyNumber(10)
        ab = netdatasize > dataBuffers.bbuf1.length ? Byte[netdatasize] : dataBuffers.bbuf1
        aboffset = 0

        final Int[] validBits = Int[(size + 31) >> 5]

        Int finaldatasize = 0

        val reverseLinks: LinkedListContainer = LinkedListContainer(size, dataBuffers.ibuf1)

        Int selev = 0
        for (Int n = 0; n < size; n++) { // loop over nodes
            val ilon: Int = alon[n]
            val ilat: Int = alat[n]

            // future escapes (turn restrictions?)
            Short trExceptions = 0
            Int featureId = bc.decodeVarBits()
            if (featureId == 13) {
                fapos[n] = aboffset
                validBits[n >> 5] |= 1 << n; // mark dummy-node valid
                continue; // empty node escape (delta files only)
            }
            while (featureId != 0) {
                val bitsize: Int = bc.decodeNoisyNumber(5)

                if (featureId == 2) { // exceptions to turn-restriction
                    trExceptions = (Short) bc.decodeBounded(1023)
                } else if (featureId == 1) { // turn-restriction
                    writeBoolean(true)
                    writeShort(trExceptions); // exceptions from previous feature
                    trExceptions = 0

                    writeBoolean(bc.decodeBit()); // isPositive
                    writeInt(ilon + bc.decodeNoisyDiff(10)); // fromLon
                    writeInt(ilat + bc.decodeNoisyDiff(10)); // fromLat
                    writeInt(ilon + bc.decodeNoisyDiff(10)); // toLon
                    writeInt(ilat + bc.decodeNoisyDiff(10)); // toLat
                } else {
                    for (Int i = 0; i < bitsize; i++) {
                        bc.decodeBit(); // unknown feature, just skip
                    }
                }
                featureId = bc.decodeVarBits()
            }
            writeBoolean(false)

            selev += nodeEleDiff.decodeSignedValue()
            writeShort((Short) selev)
            val nodeTags: TagValueWrapper = nodeTagCoder.decodeTagValueSet()
            writeVarBytes(nodeTags == null ? null : nodeTags.data)

            val links: Int = bc.decodeNoisyNumber(1)
            if (debug) {
                println("***   decoding node " + ilon + "/" + ilat + " with links=" + links)
            }
            for (Int li = 0; li < links; li++) {
                Int sizeoffset = 0
                val nodeIdx: Int = n + nodeIdxDiff.decodeSignedValue()

                Int dlonRemaining
                Int dlatRemaining

                Boolean isReverse = false
                if (nodeIdx != n) { // internal (forward-) link
                    dlonRemaining = alon[nodeIdx] - ilon
                    dlatRemaining = alat[nodeIdx] - ilat
                } else {
                    isReverse = bc.decodeBit()
                    dlonRemaining = extLonDiff.decodeSignedValue()
                    dlatRemaining = extLatDiff.decodeSignedValue()
                }
                if (debug) {
                    println("***     decoding link to " + (ilon + dlonRemaining) + "/" + (ilat + dlatRemaining) + " extern=" + (nodeIdx == n))
                }

                val wayTags: TagValueWrapper = wayTagCoder.decodeTagValueSet()

                val linkValid: Boolean = wayTags != null || wayValidator == null
                if (linkValid) {
                    val startPointer: Int = aboffset
                    sizeoffset = writeSizePlaceHolder()

                    writeVarLengthSigned(dlonRemaining)
                    writeVarLengthSigned(dlatRemaining)

                    validBits[n >> 5] |= 1 << n; // mark source-node valid
                    if (nodeIdx != n) { // valid internal (forward-) link
                        reverseLinks.addDataElement(nodeIdx, n); // register reverse link
                        finaldatasize += 1 + aboffset - startPointer; // reserve place for reverse
                        validBits[nodeIdx >> 5] |= 1 << nodeIdx; // mark target-node valid
                    }
                    writeModeAndDesc(isReverse, wayTags == null ? null : wayTags.data)
                }

                if (!isReverse) { // write geometry for forward links only
                    WaypointMatcher matcher = wayTags == null || wayTags.accessType < 2 ? null : waypointMatcher
                    val ilontarget: Int = ilon + dlonRemaining
                    val ilattarget: Int = ilat + dlatRemaining
                    if (matcher != null && !matcher.start(ilon, ilat, ilontarget, ilattarget)) {
                        matcher = null
                    }

                    val transcount: Int = bc.decodeVarBits()
                    if (debug) {
                        println("***       decoding geometry with count=" + transcount)
                    }
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
                if (linkValid) {
                    injectSize(sizeoffset)
                }
            }
            fapos[n] = aboffset
        }

        // calculate final data size
        Int finalsize = 0
        Int startpos = 0
        for (Int i = 0; i < size; i++) {
            val endpos: Int = fapos[i]
            if ((validBits[i >> 5] & (1 << i)) != 0) {
                finaldatasize += endpos - startpos
                finalsize++
            }
            startpos = endpos
        }
        // append the reverse links at the end of each node
        final Byte[] abOld = ab
        final Int[] faidOld = faid
        final Int[] faposOld = fapos
        val sizeOld: Int = size
        ab = Byte[finaldatasize]
        faid = Int[finalsize]
        fapos = Int[finalsize]
        aboffset = 0
        size = 0

        startpos = 0
        for (Int n = 0; n < sizeOld; n++) {
            val endpos: Int = faposOld[n]
            if ((validBits[n >> 5] & (1 << n)) != 0) {
                val len: Int = endpos - startpos
                System.arraycopy(abOld, startpos, ab, aboffset, len)
                if (debug) {
                    println("*** copied " + len + " bytes from " + aboffset + " for node " + n)
                }
                aboffset += len

                val cnt: Int = reverseLinks.initList(n)
                if (debug) {
                    println("*** appending " + cnt + " reverse links for node " + n)
                }

                for (Int ri = 0; ri < cnt; ri++) {
                    val nodeIdx: Int = reverseLinks.getDataElement()
                    val sizeoffset: Int = writeSizePlaceHolder()
                    writeVarLengthSigned(alon[nodeIdx] - alon[n])
                    writeVarLengthSigned(alat[nodeIdx] - alat[n])
                    writeModeAndDesc(true, null)
                    injectSize(sizeoffset)
                }
                faid[size] = faidOld[n]
                fapos[size] = aboffset
                size++
            }
            startpos = endpos
        }
        init(size)
    }

    public Byte[] readUnified(final Int len, final IByteArrayUnifier u) {
        final Byte[] b = u.unify(ab, aboffset, len)
        aboffset += len
        return b
    }

    override     public Long expandId(Int id32) {
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

        val lon32: Int = lonBase + dlon
        val lat32: Int = latBase + dlat

        return ((Long) lon32) << 32 | lat32
    }

    override     public Int shrinkId(final Long id64) {
        val lon32: Int = (Int) (id64 >> 32)
        //noinspection PointlessBitwiseExpression
        val lat32: Int = (Int) (id64 & 0xffffffff)
        val dlon: Int = lon32 - lonBase
        val dlat: Int = lat32 - latBase
        Int id32 = 0

        for (Int bm = 0x4000; bm > 0; bm >>= 1) {
            id32 <<= 2
            if ((dlon & bm) != 0) {
                id32 |= 1
            }
            if ((dlat & bm) != 0) {
                id32 |= 2
            }
        }
        return id32
    }

}
