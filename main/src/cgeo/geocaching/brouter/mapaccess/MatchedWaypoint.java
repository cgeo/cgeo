/**
 * Information on matched way point
 *
 * @author ab
 */
package cgeo.geocaching.brouter.mapaccess;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class MatchedWaypoint {
    public OsmNode node1;
    public OsmNode node2;
    public OsmNode crosspoint;
    public OsmNode waypoint;
    public String name;  // waypoint name used in error messages
    public double radius;  // distance in meter between waypoint and crosspoint

    public boolean hasUpdate;

    public static MatchedWaypoint readFromStream(final DataInput dis) throws IOException {
        final MatchedWaypoint mwp = new MatchedWaypoint();
        mwp.node1 = new OsmNode();
        mwp.node2 = new OsmNode();
        mwp.crosspoint = new OsmNode();
        mwp.waypoint = new OsmNode();

        mwp.node1.ilat = dis.readInt();
        mwp.node1.ilon = dis.readInt();
        mwp.node2.ilat = dis.readInt();
        mwp.node2.ilon = dis.readInt();
        mwp.crosspoint.ilat = dis.readInt();
        mwp.crosspoint.ilon = dis.readInt();
        mwp.waypoint.ilat = dis.readInt();
        mwp.waypoint.ilon = dis.readInt();
        mwp.radius = dis.readDouble();
        return mwp;
    }

    public void writeToStream(final DataOutput dos) throws IOException {
        dos.writeInt(node1.ilat);
        dos.writeInt(node1.ilon);
        dos.writeInt(node2.ilat);
        dos.writeInt(node2.ilon);
        dos.writeInt(crosspoint.ilat);
        dos.writeInt(crosspoint.ilon);
        dos.writeInt(waypoint.ilat);
        dos.writeInt(waypoint.ilon);
        dos.writeDouble(radius);
    }

}
