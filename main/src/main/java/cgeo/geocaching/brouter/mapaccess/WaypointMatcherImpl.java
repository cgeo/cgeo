package cgeo.geocaching.brouter.mapaccess;

import cgeo.geocaching.brouter.codec.WaypointMatcher;
import cgeo.geocaching.brouter.util.CheapAngleMeter;
import cgeo.geocaching.brouter.util.CheapRulerHelper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * the WaypointMatcher is feeded by the decoder with geoemtries of ways that are
 * already check for allowed access according to the current routing profile
 * <p>
 * It matches these geometries against the list of waypoints to find the best
 * match for each waypoint
 */
public final class WaypointMatcherImpl implements WaypointMatcher {
    private static final int MAX_POINTS = 5;

    private final List<MatchedWaypoint> waypoints;
    private final OsmNodePairSet islandPairs;

    private int lonStart;
    private int latStart;
    private int lonTarget;
    private int latTarget;
    private boolean anyUpdate;
    private int lonLast;
    private int latLast;

    private final Comparator<MatchedWaypoint> comparator;

    public WaypointMatcherImpl(final List<MatchedWaypoint> waypoints, final double maxDistance, final OsmNodePairSet islandPairs) {
        this.waypoints = waypoints;
        this.islandPairs = islandPairs;
        MatchedWaypoint last = null;
        for (MatchedWaypoint mwp : waypoints) {
            mwp.radius = maxDistance;
            if (last != null && mwp.directionToNext == -1) {
                last.directionToNext = CheapAngleMeter.getDirection(last.waypoint.ilon, last.waypoint.ilat, mwp.waypoint.ilon, mwp.waypoint.ilat);
            }
            last = mwp;
        }
        // last point has no angle so we are looking back
        final int lastidx = waypoints.size() - 2;
        if (lastidx < 0) {
            last.directionToNext = -1;
        } else {
            last.directionToNext = CheapAngleMeter.getDirection(last.waypoint.ilon, last.waypoint.ilat, waypoints.get(lastidx).waypoint.ilon, waypoints.get(lastidx).waypoint.ilat);
        }

        // sort result list
        comparator = Comparator.comparingDouble((MatchedWaypoint mw) -> mw.radius).thenComparingDouble(mw -> mw.directionDiff);

    }

    private void checkSegment(final int lon1, final int lat1, final int lon2, final int lat2) {
        // todo: bounding-box pre-filter

        final double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales((lat1 + lat2) >> 1);
        final double dlon2m = lonlat2m[0];
        final double dlat2m = lonlat2m[1];

        final double dx = (lon2 - lon1) * dlon2m;
        final double dy = (lat2 - lat1) * dlat2m;
        final double d = Math.sqrt(dy * dy + dx * dx);

        if (d == 0.) {
            return;
        }

        //for ( MatchedWaypoint mwp : waypoints )
        for (int i = 0; i < waypoints.size(); i++) {
            final MatchedWaypoint mwp = waypoints.get(i);

            if (mwp.direct && (i == 0 || waypoints.get(i - 1).direct)) {
                if (mwp.crosspoint == null) {
                    mwp.crosspoint = new OsmNode();
                    mwp.crosspoint.ilon = mwp.waypoint.ilon;
                    mwp.crosspoint.ilat = mwp.waypoint.ilat;
                    mwp.hasUpdate = true;
                    anyUpdate = true;
                }
                continue;
            }

            final OsmNode wp = mwp.waypoint;

            final double x1 = (lon1 - wp.ilon) * dlon2m;
            final double y1 = (lat1 - wp.ilat) * dlat2m;
            final double x2 = (lon2 - wp.ilon) * dlon2m;
            final double y2 = (lat2 - wp.ilat) * dlat2m;
            final double r12 = x1 * x1 + y1 * y1;
            final double r22 = x2 * x2 + y2 * y2;
            double radius = Math.abs(r12 < r22 ? y1 * dx - x1 * dy : y2 * dx - x2 * dy) / d;

            if (radius <= mwp.radius) {
                double s1 = x1 * dx + y1 * dy;
                double s2 = x2 * dx + y2 * dy;

                if (s1 < 0.) {
                    s1 = -s1;
                    s2 = -s2;
                }
                if (s2 > 0.) {
                    radius = Math.sqrt(s1 < s2 ? r12 : r22);
                    if (radius > mwp.radius) {
                        continue;
                    }
                }
                // new match for that waypoint
                mwp.radius = radius; // shortest distance to way
                mwp.hasUpdate = true;
                anyUpdate = true;
                // calculate crosspoint
                if (mwp.crosspoint == null) {
                    mwp.crosspoint = new OsmNode();
                }
                if (s2 < 0.) {
                    final double wayfraction = -s2 / (d * d);
                    final double xm = x2 - wayfraction * dx;
                    final double ym = y2 - wayfraction * dy;
                    mwp.crosspoint.ilon = (int) (xm / dlon2m + wp.ilon);
                    mwp.crosspoint.ilat = (int) (ym / dlat2m + wp.ilat);
                } else if (s1 > s2) {
                    mwp.crosspoint.ilon = lon2;
                    mwp.crosspoint.ilat = lat2;
                } else {
                    mwp.crosspoint.ilon = lon1;
                    mwp.crosspoint.ilat = lat1;
                }
            }
        }
    }

    @Override
    public boolean start(final int ilonStart, final int ilatStart, final int ilonTarget, final int ilatTarget) {
        if (islandPairs.size() > 0) {
            final long n1 = ((long) ilonStart) << 32 | ilatStart;
            final long n2 = ((long) ilonTarget) << 32 | ilatTarget;
            if (islandPairs.hasPair(n1, n2)) {
                return false;
            }
        }
        lonStart = ilonStart;
        lonLast = lonStart;
        latStart = ilatStart;
        latLast = latStart;
        lonTarget = ilonTarget;
        latTarget = ilatTarget;
        anyUpdate = false;
        return true;
    }

    @Override
    public void transferNode(final int ilon, final int ilat) {
        checkSegment(lonLast, latLast, ilon, ilat);
        lonLast = ilon;
        latLast = ilat;
    }

    @Override
    public void end() {
        checkSegment(lonLast, latLast, lonTarget, latTarget);
        if (anyUpdate) {
            for (MatchedWaypoint mwp : waypoints) {
                if (mwp.hasUpdate) {
                    double angle = CheapAngleMeter.getDirection(lonStart, latStart, lonTarget, latTarget);
                    double diff = CheapAngleMeter.getDifferenceFromDirection(mwp.directionToNext, angle);

                    mwp.hasUpdate = false;

                    MatchedWaypoint mw = new MatchedWaypoint();
                    mw.waypoint = new OsmNode();
                    mw.waypoint.ilon = mwp.waypoint.ilon;
                    mw.waypoint.ilat = mwp.waypoint.ilat;
                    mw.crosspoint = new OsmNode();
                    mw.crosspoint.ilon = mwp.crosspoint.ilon;
                    mw.crosspoint.ilat = mwp.crosspoint.ilat;
                    mw.node1 = new OsmNode(lonStart, latStart);
                    mw.node2 = new OsmNode(lonTarget, latTarget);
                    mw.name = mwp.name + "_w_" + mwp.crosspoint.hashCode();
                    mw.radius = mwp.radius;
                    mw.directionDiff = diff;
                    mw.directionToNext = mwp.directionToNext;

                    updateWayList(mwp.wayNearest, mw);

                    // revers
                    angle = CheapAngleMeter.getDirection(lonTarget, latTarget, lonStart, latStart);
                    diff = CheapAngleMeter.getDifferenceFromDirection(mwp.directionToNext, angle);
                    mw = new MatchedWaypoint();
                    mw.waypoint = new OsmNode();
                    mw.waypoint.ilon = mwp.waypoint.ilon;
                    mw.waypoint.ilat = mwp.waypoint.ilat;
                    mw.crosspoint = new OsmNode();
                    mw.crosspoint.ilon = mwp.crosspoint.ilon;
                    mw.crosspoint.ilat = mwp.crosspoint.ilat;
                    mw.node1 = new OsmNode(lonTarget, latTarget);
                    mw.node2 = new OsmNode(lonStart, latStart);
                    mw.name = mwp.name + "_w2_" + mwp.crosspoint.hashCode();
                    mw.radius = mwp.radius;
                    mw.directionDiff = diff;
                    mw.directionToNext = mwp.directionToNext;

                    updateWayList(mwp.wayNearest, mw);

                    final MatchedWaypoint way = mwp.wayNearest.get(0);
                    mwp.crosspoint.ilon = way.crosspoint.ilon;
                    mwp.crosspoint.ilat = way.crosspoint.ilat;
                    mwp.node1 = new OsmNode(way.node1.ilon, way.node1.ilat);
                    mwp.node2 = new OsmNode(way.node2.ilon, way.node2.ilat);
                    mwp.directionDiff = way.directionDiff;
                    mwp.radius = way.radius;

                }
            }
        }
    }


    // check limit of list size (avoid long runs)
    void updateWayList(List<MatchedWaypoint> ways, MatchedWaypoint mw) {
        ways.add(mw);
        // use only shortest distances by smallest direction difference
        Collections.sort(ways, comparator);
        if (ways.size() > MAX_POINTS) {
            ways.remove(MAX_POINTS);
        }

    }

}
