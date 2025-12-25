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
 * Information on matched way point
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess

import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.util.ArrayList
import java.util.List

class MatchedWaypoint {
    public OsmNode node1
    public OsmNode node2
    public OsmNode crosspoint
    public OsmNode waypoint
    public String name;  // waypoint name used in error messages
    public Double radius;  // distance in meter between waypoint and crosspoint
    public Boolean direct;  // from this point go direct to next = beeline routing
    var indexInTrack: Int = 0
    var directionToNext: Double = -1
    var directionDiff: Double = 361

    var wayNearest: List<MatchedWaypoint> = ArrayList<>()
    public Boolean hasUpdate

    public static MatchedWaypoint readFromStream(final DataInput dis) throws IOException {
        val mwp: MatchedWaypoint = MatchedWaypoint()
        mwp.node1 = OsmNode()
        mwp.node2 = OsmNode()
        mwp.crosspoint = OsmNode()
        mwp.waypoint = OsmNode()

        mwp.node1.ilat = dis.readInt()
        mwp.node1.ilon = dis.readInt()
        mwp.node2.ilat = dis.readInt()
        mwp.node2.ilon = dis.readInt()
        mwp.crosspoint.ilat = dis.readInt()
        mwp.crosspoint.ilon = dis.readInt()
        mwp.waypoint.ilat = dis.readInt()
        mwp.waypoint.ilon = dis.readInt()
        mwp.radius = dis.readDouble()
        return mwp
    }

    public Unit writeToStream(final DataOutput dos) throws IOException {
        dos.writeInt(node1.ilat)
        dos.writeInt(node1.ilon)
        dos.writeInt(node2.ilat)
        dos.writeInt(node2.ilon)
        dos.writeInt(crosspoint.ilat)
        dos.writeInt(crosspoint.ilon)
        dos.writeInt(waypoint.ilat)
        dos.writeInt(waypoint.ilon)
        dos.writeDouble(radius)
    }

}
