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
 * Container for link between two Osm nodes
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess

import cgeo.geocaching.brouter.util.ByteDataReader


class GeometryDecoder {
    private val r: ByteDataReader = ByteDataReader(null)
    private final OsmTransferNode[] cachedNodes
    private val nCachedNodes: Int = 128

    // result-cache
    private OsmTransferNode firstTransferNode
    private Boolean lastReverse
    private Byte[] lastGeometry

    public GeometryDecoder() {
        // create some caches
        cachedNodes = OsmTransferNode[nCachedNodes]
        for (Int i = 0; i < nCachedNodes; i++) {
            cachedNodes[i] = OsmTransferNode()
        }
    }

    public OsmTransferNode decodeGeometry(final Byte[] geometry, final OsmNode sourceNode, final OsmNode targetNode, final Boolean reverseLink) {
        if ((lastGeometry == geometry) && (lastReverse == reverseLink)) {
            return firstTransferNode
        }

        firstTransferNode = null
        OsmTransferNode lastTransferNode = null
        val startnode: OsmNode = reverseLink ? targetNode : sourceNode
        r.reset(geometry)
        Int olon = startnode.ilon
        Int olat = startnode.ilat
        Int oselev = startnode.selev
        Int idx = 0
        while (r.hasMoreData()) {
            val trans: OsmTransferNode = idx < nCachedNodes ? cachedNodes[idx++] : OsmTransferNode()
            trans.ilon = olon + r.readVarLengthSigned()
            trans.ilat = olat + r.readVarLengthSigned()
            trans.selev = (Short) (oselev + r.readVarLengthSigned())
            olon = trans.ilon
            olat = trans.ilat
            oselev = trans.selev
            if (reverseLink) { // reverse chaining
                trans.next = firstTransferNode
                firstTransferNode = trans
            } else {
                trans.next = null
                if (lastTransferNode == null) {
                    firstTransferNode = trans
                } else {
                    lastTransferNode.next = trans
                }
                lastTransferNode = trans
            }
        }

        lastReverse = reverseLink
        lastGeometry = geometry

        return firstTransferNode
    }
}
