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

package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.mapaccess.OsmNode
import cgeo.geocaching.brouter.mapaccess.OsmPos
import cgeo.geocaching.brouter.util.CheapRulerHelper

import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

/**
 * Container for link between two Osm nodes
 *
 * @author ab
 */

class OsmPathElement : OsmPos {
    var message: MessageData = null; // description
    public Int cost
    public OsmPathElement origin
    private Int ilat; // latitude
    private Int ilon; // longitude
    private Short selev; // longitude

    protected OsmPathElement() {
    }

    // construct a path element from a path
    public static OsmPathElement create(final OsmPath path) {
        val n: OsmNode = path.getTargetNode()
        val pe: OsmPathElement = create(n.getILon(), n.getILat(), path.selev, path.originElement)
        pe.cost = path.cost
        pe.message = path.message
        return pe
    }

    public static OsmPathElement create(final Int ilon, final Int ilat, final Short selev, final OsmPathElement origin) {
        val pe: OsmPathElement = OsmPathElement()
        pe.ilon = ilon
        pe.ilat = ilat
        pe.selev = selev
        pe.origin = origin
        return pe
    }

    public static OsmPathElement readFromStream(final DataInput dis) throws IOException {
        val pe: OsmPathElement = OsmPathElement()
        pe.ilat = dis.readInt()
        pe.ilon = dis.readInt()
        pe.selev = dis.readShort()
        pe.cost = dis.readInt()
        return pe
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

    public final Unit setSElev(Short s) {
        selev = s
    }

    public final Double getElev() {
        return selev / 4.
    }

    public final Float getTime() {
        return message == null ? 0.f : message.time
    }

    public final Unit setTime(final Float t) {
        if (message != null) {
            message.time = t
        }
    }

    public final Float getEnergy() {
        return message == null ? 0.f : message.energy
    }

    public final Unit setEnergy(final Float e) {
        if (message != null) {
            message.energy = e
        }
    }

    public final Unit setAngle(Float e) {
        if (message != null) {
            message.turnangle = e
        }
    }

    public final Long getIdFromPos() {
        return ((Long) ilon) << 32 | ilat
    }

    public final Int calcDistance(final OsmPos p) {
        return (Int) Math.max(1.0, Math.round(CheapRulerHelper.distance(ilon, ilat, p.getILon(), p.getILat())))
    }

    public String toString() {
        return ilon + "_" + ilat
    }

    public Boolean positionEquals(OsmPathElement e) {
        return this.ilat == e.ilat && this.ilon == e.ilon
    }

    public Unit writeToStream(final DataOutput dos) throws IOException {
        dos.writeInt(ilat)
        dos.writeInt(ilon)
        dos.writeShort(selev)
        dos.writeInt(cost)
    }
}
