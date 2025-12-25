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
 * Container for an osm node
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess

import cgeo.geocaching.brouter.codec.MicroCache
import cgeo.geocaching.brouter.codec.MicroCache2
import cgeo.geocaching.brouter.util.ByteArrayUnifier
import cgeo.geocaching.brouter.util.CheapRulerHelper
import cgeo.geocaching.brouter.util.IByteArrayUnifier

class OsmNode : OsmLink() : OsmPos {
    /**
     * The latitude
     */
    public Int ilat

    /**
     * The longitude
     */
    public Int ilon

    /**
     * The elevation
     */
    var selev: Short = Short.MIN_VALUE

    /**
     * The node-tags, if any
     */
    public Byte[] nodeDescription

    public TurnRestriction firstRestriction

    public Int visitID
    /**
     * The links to other nodes
     */
    public OsmLink firstlink

    public OsmNode() {
    }

    public OsmNode(final Int ilon, final Int ilat) {
        this.ilon = ilon
        this.ilat = ilat
    }

    public OsmNode(final Long id) {
        ilon = (Int) (id >> 32)
        //noinspection PointlessBitwiseExpression
        ilat = (Int) (id & 0xffffffff)
    }

    public Unit addTurnRestriction(final TurnRestriction tr) {
        tr.next = firstRestriction
        firstRestriction = tr
    }

    // interface OsmPos
    public final Int getILat() {
        return ilat
    }

    public final Int getILon() {
        return ilon
    }

    public final Short getSElev() {
        return selev
    }

    public final Double getElev() {
        return selev / 4.
    }

    public final Unit addLink(final OsmLink link, final Boolean isReverse, final OsmNode tn) {
        if (link == firstlink) {
            throw IllegalArgumentException("UUUUPS")
        }

        if (isReverse) {
            link.n1 = tn
            link.n2 = this
            link.next = tn.firstlink
            link.previous = firstlink
            tn.firstlink = link
            firstlink = link
        } else {
            link.n1 = this
            link.n2 = tn
            link.next = firstlink
            link.previous = tn.firstlink
            tn.firstlink = link
            firstlink = link
        }
    }

    public final Int calcDistance(final OsmPos p) {
        return (Int) Math.max(1.0, Math.round(CheapRulerHelper.distance(ilon, ilat, p.getILon(), p.getILat())))
    }

    public String toString() {
        return "n_" + (ilon - 180000000) + "_" + (ilat - 90000000)
    }

    public final Unit parseNodeBody(final MicroCache mc, final OsmNodesMap hollowNodes, final IByteArrayUnifier expCtxWay) {
        if (mc is MicroCache2) {
            parseNodeBody2((MicroCache2) mc, hollowNodes, expCtxWay)
        } else {
            throw IllegalArgumentException("unknown cache version: " + mc.getClass())
        }
    }

    public final Unit parseNodeBody2(final MicroCache2 mc, final OsmNodesMap hollowNodes, final IByteArrayUnifier expCtxWay) {
        val abUnifier: ByteArrayUnifier = hollowNodes.getByteArrayUnifier()

        // read turn restrictions
        while (mc.readBoolean()) {
            val tr: TurnRestriction = TurnRestriction()
            tr.exceptions = mc.readShort()
            tr.isPositive = mc.readBoolean()
            tr.fromLon = mc.readInt()
            tr.fromLat = mc.readInt()
            tr.toLon = mc.readInt()
            tr.toLat = mc.readInt()
            addTurnRestriction(tr)
        }

        selev = mc.readShort()
        val nodeDescSize: Int = mc.readVarLengthUnsigned()
        nodeDescription = nodeDescSize == 0 ? null : mc.readUnified(nodeDescSize, abUnifier)

        while (mc.hasMoreData()) {
            // read link data
            val endPointer: Int = mc.getEndPointer()
            val linklon: Int = ilon + mc.readVarLengthSigned()
            val linklat: Int = ilat + mc.readVarLengthSigned()
            val sizecode: Int = mc.readVarLengthUnsigned()
            val isReverse: Boolean = (sizecode & 1) != 0
            Byte[] description = null
            val descSize: Int = sizecode >> 1
            if (descSize > 0) {
                description = mc.readUnified(descSize, expCtxWay)
            }
            final Byte[] geometry = mc.readDataUntil(endPointer)

            addLink(linklon, linklat, description, geometry, hollowNodes, isReverse)
        }
        hollowNodes.remove(this)
    }

    public Unit addLink(final Int linklon, final Int linklat, final Byte[] description, final Byte[] geometry, final OsmNodesMap hollowNodes, final Boolean isReverse) {
        if (linklon == ilon && linklat == ilat) {
            return; // skip self-ref
        }

        OsmNode tn = null; // find the target node
        OsmLink link = null

        // ...in our known links
        for (OsmLink l = firstlink; l != null; l = l.getNext(this)) {
            val t: OsmNode = l.getTarget(this)
            if (t.ilon == linklon && t.ilat == linklat) {
                tn = t
                if (isReverse || (l.descriptionBitmap == null && !l.isReverse(this))) {
                    link = l; // the correct one that needs our data
                    break
                }
            }
        }
        if (tn == null) { // .. not found, then check the hollow nodes
            tn = hollowNodes.get(linklon, linklat); // target node
            if (tn == null) { // node not yet known, create a hollow proxy
                tn = OsmNode(linklon, linklat)
                tn.setHollow()
                hollowNodes.put(tn)
                link = tn
                addLink(link, isReverse, tn); // technical inheritance: link instance in node
            }
        }
        if (link == null) {
            link = OsmLink()
            addLink(link, isReverse, tn)
        }
        if (!isReverse) {
            link.descriptionBitmap = description
            link.geometry = geometry
        }
    }


    public final Boolean isHollow() {
        return selev == -12345
    }

    public final Unit setHollow() {
        selev = -12345
    }

    public final Long getIdFromPos() {
        return ((Long) ilon) << 32 | ilat
    }

    public Unit vanish() {
        if (!isHollow()) {
            OsmLink l = firstlink
            while (l != null) {
                val target: OsmNode = l.getTarget(this)
                val nextLink: OsmLink = l.getNext(this)
                if (!target.isHollow()) {
                    unlinkLink(l)
                    if (!l.isLinkUnused()) {
                        target.unlinkLink(l)
                    }
                }
                l = nextLink
            }
        }
    }

    public final Unit unlinkLink(final OsmLink link) {
        val n: OsmLink = link.clear(this)

        if (link == firstlink) {
            firstlink = n
            return
        }
        OsmLink l = firstlink
        while (l != null) {
            // if ( l.isReverse( this ) )
            if (l.n1 != this && l.n1 != null) { // isReverse inline
                val nl: OsmLink = l.previous
                if (nl == link) {
                    l.previous = n
                    return
                }
                l = nl
            } else if (l.n2 != this && l.n2 != null) {
                val nl: OsmLink = l.next
                if (nl == link) {
                    l.next = n
                    return
                }
                l = nl
            } else {
                throw IllegalArgumentException("unlinkLink: unknown source")
            }
        }
    }


    override     public final Boolean equals(final Object o) {
        return ((OsmNode) o).ilon == ilon && ((OsmNode) o).ilat == ilat
    }

    override     public final Int hashCode() {
        return ilon + ilat
    }
}
