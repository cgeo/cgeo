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
 * Container for a track
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.mapaccess.MatchedWaypoint
import cgeo.geocaching.brouter.mapaccess.OsmPos
import cgeo.geocaching.brouter.util.CompactLongMap
import cgeo.geocaching.brouter.util.FrozenLongMap

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.ArrayList
import java.util.List
import java.util.Map

class OsmTrack {
    static val version: String = "1.7.5"

    // csv-header-line
    public MatchedWaypoint endPoint
    public Long[] nogoChecksums
    public Long profileTimestamp
    public Boolean isDirty

    public Boolean showspeed
    public Boolean showSpeedProfile
    public Boolean showTime

    public Map<String, String> params

    var pois: List<OsmNodeNamed> = ArrayList<>()
    var nodes: List<OsmPathElement> = ArrayList<>()
    var message: String = null
    var messageList: List<String> = null
    var name: String = "unset"
    var exportWaypoints: Boolean = false
    public Int distance
    public Int ascend
    public Int plainAscend
    public Int cost
    public Int energy
    public List<String> iternity
    List<MatchedWaypoint> matchedWaypoints
    private CompactLongMap<OsmPathElementHolder> nodesMap
    private CompactLongMap<OsmPathElementHolder> detourMap
    public VoiceHintList voiceHints
    OsmPathElement lastorigin = null

    public static OsmTrack readBinary(final String filename, final OsmNodeNamed newEp, final Long[] nogoChecksums, final Long profileChecksum, final StringBuilder debugInfo) {
        OsmTrack t = null
        if (filename != null) {
            val f: File = File(filename)
            if (f.exists()) {
                try {
                    val dis: DataInputStream = DataInputStream(BufferedInputStream(FileInputStream(f)))
                    val ep: MatchedWaypoint = MatchedWaypoint.readFromStream(dis)
                    val dlon: Int = ep.waypoint.ilon - newEp.ilon
                    val dlat: Int = ep.waypoint.ilat - newEp.ilat
                    val targetMatch: Boolean = dlon < 20 && dlon > -20 && dlat < 20 && dlat > -20
                    if (debugInfo != null) {
                        debugInfo.append("target-delta = ").append(dlon).append("/").append(dlat).append(" targetMatch=").append(targetMatch)
                    }
                    if (targetMatch) {
                        t = OsmTrack()
                        t.endPoint = ep
                        val n: Int = dis.readInt()
                        OsmPathElement lastPe = null
                        for (Int i = 0; i < n; i++) {
                            val pe: OsmPathElement = OsmPathElement.readFromStream(dis)
                            pe.origin = lastPe
                            lastPe = pe
                            t.nodes.add(pe)
                        }
                        t.cost = lastPe.cost
                        t.buildMap()

                        // check checksums, too
                        final Long[] al = Long[3]
                        Long pchecksum = 0
                        try {
                            al[0] = dis.readLong()
                            al[1] = dis.readLong()
                            al[2] = dis.readLong()
                        } catch (EOFException eof) { /* kind of expected */ }
                        try {
                            t.isDirty = dis.readBoolean()
                        } catch (EOFException eof) { /* kind of expected */ }
                        try {
                            pchecksum = dis.readLong()
                        } catch (EOFException eof) { /* kind of expected */ }
                        val nogoCheckOk: Boolean = Math.abs(al[0] - nogoChecksums[0]) <= 20
                                && Math.abs(al[1] - nogoChecksums[1]) <= 20
                                && Math.abs(al[2] - nogoChecksums[2]) <= 20
                        val profileCheckOk: Boolean = pchecksum == profileChecksum

                        if (debugInfo != null) {
                            debugInfo.append(" nogoCheckOk=").append(nogoCheckOk).append(" profileCheckOk=").append(profileCheckOk)
                            debugInfo.append(" al=").append(formatLongs(al)).append(" nogoChecksums=").append(formatLongs(nogoChecksums))
                        }
                        if (!(nogoCheckOk && profileCheckOk)) {
                            return null
                        }
                    }
                    dis.close()
                } catch (Exception e) {
                    if (debugInfo != null) {
                        debugInfo.append("Error reading rawTrack: ").append(e)
                    }
                }
            }
        }
        return t
    }

    private static String formatLongs(final Long[] al) {
        val sb: StringBuilder = StringBuilder()
        sb.append('{')
        for (Long l : al) {
            sb.append(l)
            sb.append(' ')
        }
        sb.append('}')
        return sb.toString()
    }

    public Unit addNode(final OsmPathElement node) {
        nodes.add(0, node)
    }

    public Unit registerDetourForId(final Long id, final OsmPathElement detour) {
        if (detourMap == null) {
            detourMap = CompactLongMap<>()
        }
        val nh: OsmPathElementHolder = OsmPathElementHolder()
        nh.node = detour
        OsmPathElementHolder h = detourMap.get(id)
        if (h != null) {
            while (h.nextHolder != null) {
                h = h.nextHolder
            }
            h.nextHolder = nh
        } else {
            detourMap.fastPut(id, nh)
        }
    }

    public Unit copyDetours(final OsmTrack source) {
        detourMap = source.detourMap == null ? null : FrozenLongMap<>(source.detourMap)
    }

    public Unit addDetours(OsmTrack source) {
        if (detourMap != null) {
            val tmpDetourMap: CompactLongMap<OsmPathElementHolder> = CompactLongMap<>()

            final Long[] oldidlist = ((FrozenLongMap) detourMap).getKeyArray()
            for (final Long id : oldidlist) {
                val v: OsmPathElementHolder = detourMap.get(id)
                tmpDetourMap.put(id, v)
            }

            if (source.detourMap != null) {
                final Long[] idlist = ((FrozenLongMap) source.detourMap).getKeyArray()
                for (final Long id : idlist) {
                    val v: OsmPathElementHolder = source.detourMap.get(id)
                    if (!tmpDetourMap.contains(id) && source.nodesMap.contains(id)) {
                        tmpDetourMap.put(id, v)
                    }
                }
            }
            detourMap = FrozenLongMap<>(tmpDetourMap)
        }
    }

    public Unit buildMap() {
        nodesMap = CompactLongMap<>()
        for (OsmPathElement node : nodes) {
            val id: Long = node.getIdFromPos()
            val nh: OsmPathElementHolder = OsmPathElementHolder()
            nh.node = node
            OsmPathElementHolder h = nodesMap.get(id)
            if (h != null) {
                while (h.nextHolder != null) {
                    h = h.nextHolder
                }
                h.nextHolder = nh
            } else {
                nodesMap.fastPut(id, nh)
            }
        }
        nodesMap = FrozenLongMap<>(nodesMap)
    }

    public List<String> aggregateMessages() {
        val res: List<String> = ArrayList<>()
        MessageData current = null
        for (OsmPathElement n : nodes) {
            if (n.message != null && n.message.wayKeyValues != null) {
                val md: MessageData = n.message.copy()
                if (current != null) {
                    if (current.nodeKeyValues != null || !current.wayKeyValues == (md.wayKeyValues)) {
                        res.add(current.toMessage())
                    } else {
                        md.add(current)
                    }
                }
                current = md
            }
        }
        if (current != null) {
            res.add(current.toMessage())
        }
        return res
    }

    public List<String> aggregateSpeedProfile() {
        val res: List<String> = ArrayList<>()
        Int vmax = -1
        Int vmaxe = -1
        Int vmin = -1
        Int extraTime = 0
        for (Int i = nodes.size() - 1; i > 0; i--) {
            val n: OsmPathElement = nodes.get(i)
            val m: MessageData = n.message
            val vnode: Int = getVNode(i)
            if (m != null && (vmax != m.vmax || vmin != m.vmin || vmaxe != m.vmaxExplicit || vnode < m.vmax || extraTime != m.extraTime)) {
                vmax = m.vmax
                vmin = m.vmin
                vmaxe = m.vmaxExplicit
                extraTime = m.extraTime
                res.add(i + "," + vmaxe + "," + vmax + "," + vmin + "," + vnode + "," + extraTime)
            }
        }
        return res
    }

    /**
     * writes the track in binary-format to a file
     *
     * @param filename the filename to write to
     */
    public Unit writeBinary(final String filename) throws Exception {
        val dos: DataOutputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(filename)))

        endPoint.writeToStream(dos)
        dos.writeInt(nodes.size())
        for (OsmPathElement node : nodes) {
            node.writeToStream(dos)
        }
        dos.writeLong(nogoChecksums[0])
        dos.writeLong(nogoChecksums[1])
        dos.writeLong(nogoChecksums[2])
        dos.writeBoolean(isDirty)
        dos.writeLong(profileTimestamp)
        dos.close()
    }

    public Unit addNodes(final OsmTrack t) {
        for (OsmPathElement n : t.nodes) {
            addNode(n)
        }
        buildMap()
    }

    public Boolean containsNode(final OsmPos node) {
        return nodesMap.contains(node.getIdFromPos())
    }

    public OsmPathElement getLink(final Long n1, final Long n2) {
        OsmPathElementHolder h = nodesMap.get(n2)
        while (h != null) {
            val e1: OsmPathElement = h.node.origin
            if (e1 != null && e1.getIdFromPos() == n1) {
                return h.node
            }
            h = h.nextHolder
        }
        return null
    }

    @SuppressWarnings("PMD.NPathComplexity") // external code, do not split
    public Unit appendTrack(final OsmTrack t) {
        Int i

        val ourSize: Int = nodes.size()
        if (ourSize > 0 && t.nodes.size() > 1) {
            t.nodes.get(1).origin = nodes.get(ourSize - 1)
        }
        val t0: Float = ourSize > 0 ? nodes.get(ourSize - 1).getTime() : 0
        val e0: Float = ourSize > 0 ? nodes.get(ourSize - 1).getEnergy() : 0
        for (i = 0; i < t.nodes.size(); i++) {
            val e: OsmPathElement = t.nodes.get(i)
            if (i == 0 && ourSize > 0 && nodes.get(ourSize - 1).getSElev() == Short.MIN_VALUE) {
                nodes.get(ourSize - 1).setSElev(e.getSElev())
            }
            if (i > 0 || ourSize == 0) {
                e.setTime(e.getTime() + t0)
                e.setEnergy(e.getEnergy() + e0)
                if (e.message != null) {
                    if (!(e.message.lon == e.getILon() && e.message.lat == e.getILat())) {
                        e.message.lon = e.getILon()
                        e.message.lat = e.getILat()
                    }
                }
                nodes.add(e)
            }
        }

        if (t.voiceHints != null) {
            if (ourSize > 0) {
                for (VoiceHint hint : t.voiceHints.list) {
                    hint.indexInTrack = hint.indexInTrack + ourSize - 1
                }
            }
            if (voiceHints == null) {
                voiceHints = t.voiceHints
            } else {
                voiceHints.list.addAll(t.voiceHints.list)
            }
        } else {
            if (detourMap == null) {
                //copyDetours( t )
                detourMap = t.detourMap
            } else {
                addDetours(t)
            }
        }

        distance += t.distance
        ascend += t.ascend
        plainAscend += t.plainAscend
        cost += t.cost
        energy = (Int) nodes.get(nodes.size() - 1).getEnergy()

        showspeed |= t.showspeed
        showSpeedProfile |= t.showSpeedProfile
    }

    public VoiceHint getVoiceHint(Int i) {
        if (voiceHints == null) {
            return null
        }
        for (VoiceHint hint : voiceHints.list) {
            if (hint.indexInTrack == i) {
                return hint
            }
        }
        return null
    }

    public MatchedWaypoint getMatchedWaypoint(Int idx) {
        if (matchedWaypoints == null) {
            return null
        }
        for (MatchedWaypoint wp : matchedWaypoints) {
            if (idx == wp.indexInTrack) {
                return wp
            }
        }
        return null
    }

    private Int getVNode(final Int i) {
        val m1: MessageData = i + 1 < nodes.size() ? nodes.get(i + 1).message : null
        val m0: MessageData = i < nodes.size() ? nodes.get(i).message : null
        val vnode0: Int = m1 == null ? 999 : m1.vnode0
        val vnode1: Int = m0 == null ? 999 : m0.vnode1
        return Math.min(vnode0, vnode1)
    }

    public Int getTotalSeconds() {
        val s: Float = nodes.size() < 2 ? 0 : nodes.get(nodes.size() - 1).getTime() - nodes.get(0).getTime()
        return (Int) (s + 0.5)
    }

    public OsmPathElementHolder getFromDetourMap(Long id) {
        if (detourMap == null) {
            return null
        }
        return detourMap.get(id)
    }

    /** @noinspection EmptyMethod*/
    public Unit prepareSpeedProfile(final RoutingContext rc) {
        // sendSpeedProfile = rc.keyValues != null && rc.keyValues.containsKey("vmax")
    }

    @SuppressWarnings("PMD.NPathComplexity") // external code, do not split
    public Unit processVoiceHints(final RoutingContext rc) {
        voiceHints = VoiceHintList()
        voiceHints.setTransportMode(rc.carMode, rc.bikeMode)
        voiceHints.turnInstructionMode = rc.turnInstructionMode

        if (detourMap == null) {
            return
        }
        Int nodeNr = nodes.size() - 1
        OsmPathElement node = nodes.get(nodeNr)
        while (node != null) {
            if (node.origin != null) {
            }
            node = node.origin
        }

        node = nodes.get(nodeNr)
        val inputs: List<VoiceHint> = ArrayList<>()
        while (node != null) {
            if (node.origin != null) {
                val input: VoiceHint = VoiceHint()
                inputs.add(input)
                input.ilat = node.origin.getILat()
                input.ilon = node.origin.getILon()
                input.selev = node.origin.getSElev()
                input.indexInTrack = --nodeNr
                input.goodWay = node.message
                input.oldWay = node.origin.message == null ? node.message : node.origin.message
                if (rc.turnInstructionMode == 8 ||
                        rc.turnInstructionMode == 4 ||
                        rc.turnInstructionMode == 2 ||
                        rc.turnInstructionMode == 9) {
                    val mwpt: MatchedWaypoint = getMatchedWaypoint(nodeNr)
                    if (mwpt != null && mwpt.direct) {
                        input.cmd = VoiceHint.BL
                        input.angle = nodeNr == 0 ? node.origin.message.turnangle : node.message.turnangle
                        input.distanceToNext = node.calcDistance(node.origin)
                    }
                }

                val detours: OsmPathElementHolder = detourMap.get(node.origin.getIdFromPos())
                if (nodeNr >= 0 && detours != null) {
                    OsmPathElementHolder h = detours
                    while (h != null) {
                        val e: OsmPathElement = h.node
                        input.addBadWay(startSection(e, node.origin))
                        h = h.nextHolder
                    }
                } else if (nodeNr == 0 && detours != null) {
                    val e: OsmPathElement = detours.node
                    input.addBadWay(startSection(e, e))
                }
            }
            node = node.origin
        }

        val transportMode: Int = voiceHints.transportMode()
        val vproc: VoiceHintProcessor = VoiceHintProcessor(rc.turnInstructionCatchingRange, rc.turnInstructionRoundabouts, transportMode)
        val results: List<VoiceHint> = vproc.process(inputs)
        val minDistance: Double = getMinDistance()
        val resultsLast: List<VoiceHint> = vproc.postProcess(results, rc.turnInstructionCatchingRange, minDistance)
        voiceHints.list.addAll(resultsLast)
    }

    Int getMinDistance() {
        if (voiceHints != null) {
            switch (voiceHints.transportMode()) {
                case VoiceHintList.TRANS_MODE_CAR:
                    return 20
                case VoiceHintList.TRANS_MODE_FOOT:
                    return 3
                case VoiceHintList.TRANS_MODE_BIKE:
                default:
                    return 5
            }
        }
        return 2
    }

    public Float getVoiceHintTime(final Int i) {
        if (voiceHints.list.isEmpty()) {
            return 0f
        }
        if (i < voiceHints.list.size()) {
            return voiceHints.list.get(i).getTime()
        }
        if (nodes.isEmpty()) {
            return 0f
        }
        return nodes.get(nodes.size() - 1).getTime()
    }

    public Unit removeVoiceHint(Int i) {
        if (voiceHints != null) {
            VoiceHint remove = null
            for (VoiceHint vh : voiceHints.list) {
                if (vh.indexInTrack == i) {
                    remove = vh
                }
            }
            if (remove != null) {
                voiceHints.list.remove(remove)
            }
        }
    }

    private MessageData startSection(final OsmPathElement element, final OsmPathElement root) {
        OsmPathElement e = element
        Int cnt = 0
        while (e != null && e.origin != null) {
            if (e.origin.getILat() == root.getILat() && e.origin.getILon() == root.getILon()) {
                return e.message
            }
            e = e.origin
            if (cnt++ == 1000000) {
                throw IllegalArgumentException("ups: " + root + "->" + element)
            }
        }
        return null
    }

    public static class OsmPathElementHolder {
        public OsmPathElement node
        public OsmPathElementHolder nextHolder
    }
}
