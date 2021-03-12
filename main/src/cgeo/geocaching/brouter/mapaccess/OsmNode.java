/**
 * Container for an osm node
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import cgeo.geocaching.brouter.codec.MicroCache;
import cgeo.geocaching.brouter.codec.MicroCache2;
import cgeo.geocaching.brouter.util.ByteArrayUnifier;
import cgeo.geocaching.brouter.util.CheapRulerHelper;
import cgeo.geocaching.brouter.util.IByteArrayUnifier;

public class OsmNode extends OsmLink implements OsmPos {
    /**
     * The latitude
     */
    public int ilat;

    /**
     * The longitude
     */
    public int ilon;

    /**
     * The elevation
     */
    public short selev;

    /**
     * The node-tags, if any
     */
    public byte[] nodeDescription;

    public TurnRestriction firstRestriction;

    public int visitID;
    /**
     * The links to other nodes
     */
    public OsmLink firstlink;

    public OsmNode() {
    }

    public OsmNode(final int ilon, final int ilat) {
        this.ilon = ilon;
        this.ilat = ilat;
    }

    public OsmNode(final long id) {
        ilon = (int) (id >> 32);
        ilat = (int) (id & 0xffffffff);
    }

    public void addTurnRestriction(final TurnRestriction tr) {
        tr.next = firstRestriction;
        firstRestriction = tr;
    }

    // interface OsmPos
    public final int getILat() {
        return ilat;
    }

    public final int getILon() {
        return ilon;
    }

    public final short getSElev() {
        return selev;
    }

    public final double getElev() {
        return selev / 4.;
    }

    public final void addLink(final OsmLink link, final boolean isReverse, final OsmNode tn) {
        if (link == firstlink) {
            throw new IllegalArgumentException("UUUUPS");
        }

        if (isReverse) {
            link.n1 = tn;
            link.n2 = this;
            link.next = tn.firstlink;
            link.previous = firstlink;
            tn.firstlink = link;
            firstlink = link;
        } else {
            link.n1 = this;
            link.n2 = tn;
            link.next = firstlink;
            link.previous = tn.firstlink;
            tn.firstlink = link;
            firstlink = link;
        }
    }

    public final int calcDistance(final OsmPos p) {
        return (int) (CheapRulerHelper.distance(ilon, ilat, p.getILon(), p.getILat()) + 1.0);
    }

    public String toString() {
        return "n_" + (ilon - 180000000) + "_" + (ilat - 90000000);
    }

    public final void parseNodeBody(final MicroCache mc, final OsmNodesMap hollowNodes, final IByteArrayUnifier expCtxWay) {
        if (mc instanceof MicroCache2) {
            parseNodeBody2((MicroCache2) mc, hollowNodes, expCtxWay);
        } else {
            throw new IllegalArgumentException("unknown cache version: " + mc.getClass());
        }
    }

    public final void parseNodeBody2(final MicroCache2 mc, final OsmNodesMap hollowNodes, final IByteArrayUnifier expCtxWay) {
        final ByteArrayUnifier abUnifier = hollowNodes.getByteArrayUnifier();

        // read turn restrictions
        while (mc.readBoolean()) {
            final TurnRestriction tr = new TurnRestriction();
            tr.exceptions = mc.readShort();
            tr.isPositive = mc.readBoolean();
            tr.fromLon = mc.readInt();
            tr.fromLat = mc.readInt();
            tr.toLon = mc.readInt();
            tr.toLat = mc.readInt();
            addTurnRestriction(tr);
        }

        selev = mc.readShort();
        final int nodeDescSize = mc.readVarLengthUnsigned();
        nodeDescription = nodeDescSize == 0 ? null : mc.readUnified(nodeDescSize, abUnifier);

        while (mc.hasMoreData()) {
            // read link data
            final int endPointer = mc.getEndPointer();
            final int linklon = ilon + mc.readVarLengthSigned();
            final int linklat = ilat + mc.readVarLengthSigned();
            final int sizecode = mc.readVarLengthUnsigned();
            final boolean isReverse = (sizecode & 1) != 0;
            byte[] description = null;
            final int descSize = sizecode >> 1;
            if (descSize > 0) {
                description = mc.readUnified(descSize, expCtxWay);
            }
            final byte[] geometry = mc.readDataUntil(endPointer);

            addLink(linklon, linklat, description, geometry, hollowNodes, isReverse);
        }
        hollowNodes.remove(this);
    }

    public void addLink(final int linklon, final int linklat, final byte[] description, final byte[] geometry, final OsmNodesMap hollowNodes, final boolean isReverse) {
        if (linklon == ilon && linklat == ilat) {
            return; // skip self-ref
        }

        OsmNode tn = null; // find the target node
        OsmLink link = null;

        // ...in our known links
        for (OsmLink l = firstlink; l != null; l = l.getNext(this)) {
            final OsmNode t = l.getTarget(this);
            if (t.ilon == linklon && t.ilat == linklat) {
                tn = t;
                if (isReverse || (l.descriptionBitmap == null && !l.isReverse(this))) {
                    link = l; // the correct one that needs our data
                    break;
                }
            }
        }
        if (tn == null) { // .. not found, then check the hollow nodes
            tn = hollowNodes.get(linklon, linklat); // target node
            if (tn == null) { // node not yet known, create a new hollow proxy
                tn = new OsmNode(linklon, linklat);
                tn.setHollow();
                hollowNodes.put(tn);
                link = tn;
                addLink(link, isReverse, tn); // technical inheritance: link instance in node
            }
        }
        if (link == null) {
            link = new OsmLink();
            addLink(link, isReverse, tn);
        }
        if (!isReverse) {
            link.descriptionBitmap = description;
            link.geometry = geometry;
        }
    }


    public final boolean isHollow() {
        return selev == -12345;
    }

    public final void setHollow() {
        selev = -12345;
    }

    public final long getIdFromPos() {
        return ((long) ilon) << 32 | ilat;
    }

    public void vanish() {
        if (!isHollow()) {
            OsmLink l = firstlink;
            while (l != null) {
                final OsmNode target = l.getTarget(this);
                final OsmLink nextLink = l.getNext(this);
                if (!target.isHollow()) {
                    unlinkLink(l);
                    if (!l.isLinkUnused()) {
                        target.unlinkLink(l);
                    }
                }
                l = nextLink;
            }
        }
    }

    public final void unlinkLink(final OsmLink link) {
        final OsmLink n = link.clear(this);

        if (link == firstlink) {
            firstlink = n;
            return;
        }
        OsmLink l = firstlink;
        while (l != null) {
            // if ( l.isReverse( this ) )
            if (l.n1 != this && l.n1 != null) { // isReverse inline
                final OsmLink nl = l.previous;
                if (nl == link) {
                    l.previous = n;
                    return;
                }
                l = nl;
            } else if (l.n2 != this && l.n2 != null) {
                final OsmLink nl = l.next;
                if (nl == link) {
                    l.next = n;
                    return;
                }
                l = nl;
            } else {
                throw new IllegalArgumentException("unlinkLink: unknown source");
            }
        }
    }


    @Override
    public final boolean equals(final Object o) {
        return ((OsmNode) o).ilon == ilon && ((OsmNode) o).ilat == ilat;
    }

    @Override
    public final int hashCode() {
        return ilon + ilat;
    }
}
