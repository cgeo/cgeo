package cgeo.geocaching.brouter.codec;

/**
 * a waypoint matcher gets way geometries
 * from the decoder to find the closest
 * matches to the waypoints
 */
public interface WaypointMatcher {
    boolean start(int ilonStart, int ilatStart, int ilonTarget, int ilatTarget);

    void transferNode(int ilon, int ilat);

    void end();
}
