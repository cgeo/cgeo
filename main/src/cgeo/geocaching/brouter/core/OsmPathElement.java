package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.mapaccess.OsmNode;
import cgeo.geocaching.brouter.mapaccess.OsmPos;
import cgeo.geocaching.brouter.util.CheapRulerHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */

public class OsmPathElement implements OsmPos {
    public MessageData message = null; // description
    public int cost;
    public OsmPathElement origin;
    private int ilat; // latitude
    private int ilon; // longitude
    private short selev; // longitude

    protected OsmPathElement() {
    }

    // construct a path element from a path
    public static OsmPathElement create(final OsmPath path, final boolean countTraffic) {
        final OsmNode n = path.getTargetNode();
        final OsmPathElement pe = create(n.getILon(), n.getILat(), path.selev, path.originElement, countTraffic);
        pe.cost = path.cost;
        pe.message = path.message;
        return pe;
    }

    public static OsmPathElement create(final int ilon, final int ilat, final short selev, final OsmPathElement origin, final boolean countTraffic) {
        final OsmPathElement pe = countTraffic ? new OsmPathElementWithTraffic() : new OsmPathElement();
        pe.ilon = ilon;
        pe.ilat = ilat;
        pe.selev = selev;
        pe.origin = origin;
        return pe;
    }

    public static OsmPathElement readFromStream(final DataInput dis) throws IOException {
        final OsmPathElement pe = new OsmPathElement();
        pe.ilat = dis.readInt();
        pe.ilon = dis.readInt();
        pe.selev = dis.readShort();
        pe.cost = dis.readInt();
        return pe;
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

    public final float getTime() {
        return message == null ? 0.f : message.time;
    }

    public final void setTime(final float t) {
        if (message != null) {
            message.time = t;
        }
    }

    public final float getEnergy() {
        return message == null ? 0.f : message.energy;
    }

    public final void setEnergy(final float e) {
        if (message != null) {
            message.energy = e;
        }
    }

    public final long getIdFromPos() {
        return ((long) ilon) << 32 | ilat;
    }

    public final int calcDistance(final OsmPos p) {
        return (int) (CheapRulerHelper.distance(ilon, ilat, p.getILon(), p.getILat()) + 1.0);
    }

    public void addTraffic(final float traffic) {
        // default: do nothing
    }

    public String toString() {
        return ilon + "_" + ilat;
    }

    public void writeToStream(final DataOutput dos) throws IOException {
        dos.writeInt(ilat);
        dos.writeInt(ilon);
        dos.writeShort(selev);
        dos.writeInt(cost);
    }
}
