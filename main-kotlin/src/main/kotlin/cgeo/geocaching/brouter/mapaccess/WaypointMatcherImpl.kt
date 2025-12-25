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

package cgeo.geocaching.brouter.mapaccess

import cgeo.geocaching.brouter.codec.WaypointMatcher
import cgeo.geocaching.brouter.util.CheapAngleMeter
import cgeo.geocaching.brouter.util.CheapRulerHelper

import java.util.Collections
import java.util.Comparator
import java.util.List

/**
 * the WaypointMatcher is feeded by the decoder with geoemtries of ways that are
 * already check for allowed access according to the current routing profile
 * <p>
 * It matches these geometries against the list of waypoints to find the best
 * match for each waypoint
 */
class WaypointMatcherImpl : WaypointMatcher {
    private static val MAX_POINTS: Int = 5

    private final List<MatchedWaypoint> waypoints
    private final OsmNodePairSet islandPairs

    private Int lonStart
    private Int latStart
    private Int lonTarget
    private Int latTarget
    private Boolean anyUpdate
    private Int lonLast
    private Int latLast

    private final Comparator<MatchedWaypoint> comparator

    public WaypointMatcherImpl(final List<MatchedWaypoint> waypoints, final Double maxDistance, final OsmNodePairSet islandPairs) {
        this.waypoints = waypoints
        this.islandPairs = islandPairs
        MatchedWaypoint last = null
        for (MatchedWaypoint mwp : waypoints) {
            mwp.radius = maxDistance
            if (last != null && mwp.directionToNext == -1) {
                last.directionToNext = CheapAngleMeter.getDirection(last.waypoint.ilon, last.waypoint.ilat, mwp.waypoint.ilon, mwp.waypoint.ilat)
            }
            last = mwp
        }
        // last point has no angle so we are looking back
        val lastidx: Int = waypoints.size() - 2
        if (lastidx < 0) {
            last.directionToNext = -1
        } else {
            last.directionToNext = CheapAngleMeter.getDirection(last.waypoint.ilon, last.waypoint.ilat, waypoints.get(lastidx).waypoint.ilon, waypoints.get(lastidx).waypoint.ilat)
        }

        // sort result list
        comparator = Comparator.comparingDouble((MatchedWaypoint mw) -> mw.radius).thenComparingDouble(mw -> mw.directionDiff)

    }

    private Unit checkSegment(final Int lon1, final Int lat1, final Int lon2, final Int lat2) {
        // todo: bounding-box pre-filter

        final Double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales((lat1 + lat2) >> 1)
        val dlon2m: Double = lonlat2m[0]
        val dlat2m: Double = lonlat2m[1]

        val dx: Double = (lon2 - lon1) * dlon2m
        val dy: Double = (lat2 - lat1) * dlat2m
        val d: Double = Math.sqrt(dy * dy + dx * dx)

        if (d == 0.) {
            return
        }

        //for ( MatchedWaypoint mwp : waypoints )
        for (Int i = 0; i < waypoints.size(); i++) {
            val mwp: MatchedWaypoint = waypoints.get(i)

            if (mwp.direct && (i == 0 || waypoints.get(i - 1).direct)) {
                if (mwp.crosspoint == null) {
                    mwp.crosspoint = OsmNode()
                    mwp.crosspoint.ilon = mwp.waypoint.ilon
                    mwp.crosspoint.ilat = mwp.waypoint.ilat
                    mwp.hasUpdate = true
                    anyUpdate = true
                }
                continue
            }

            val wp: OsmNode = mwp.waypoint

            val x1: Double = (lon1 - wp.ilon) * dlon2m
            val y1: Double = (lat1 - wp.ilat) * dlat2m
            val x2: Double = (lon2 - wp.ilon) * dlon2m
            val y2: Double = (lat2 - wp.ilat) * dlat2m
            val r12: Double = x1 * x1 + y1 * y1
            val r22: Double = x2 * x2 + y2 * y2
            Double radius = Math.abs(r12 < r22 ? y1 * dx - x1 * dy : y2 * dx - x2 * dy) / d

            if (radius <= mwp.radius) {
                Double s1 = x1 * dx + y1 * dy
                Double s2 = x2 * dx + y2 * dy

                if (s1 < 0.) {
                    s1 = -s1
                    s2 = -s2
                }
                if (s2 > 0.) {
                    radius = Math.sqrt(s1 < s2 ? r12 : r22)
                    if (radius > mwp.radius) {
                        continue
                    }
                }
                // match for that waypoint
                mwp.radius = radius; // shortest distance to way
                mwp.hasUpdate = true
                anyUpdate = true
                // calculate crosspoint
                if (mwp.crosspoint == null) {
                    mwp.crosspoint = OsmNode()
                }
                if (s2 < 0.) {
                    val wayfraction: Double = -s2 / (d * d)
                    val xm: Double = x2 - wayfraction * dx
                    val ym: Double = y2 - wayfraction * dy
                    mwp.crosspoint.ilon = (Int) (xm / dlon2m + wp.ilon)
                    mwp.crosspoint.ilat = (Int) (ym / dlat2m + wp.ilat)
                } else if (s1 > s2) {
                    mwp.crosspoint.ilon = lon2
                    mwp.crosspoint.ilat = lat2
                } else {
                    mwp.crosspoint.ilon = lon1
                    mwp.crosspoint.ilat = lat1
                }
            }
        }
    }

    override     public Boolean start(final Int ilonStart, final Int ilatStart, final Int ilonTarget, final Int ilatTarget) {
        if (islandPairs.size() > 0) {
            val n1: Long = ((Long) ilonStart) << 32 | ilatStart
            val n2: Long = ((Long) ilonTarget) << 32 | ilatTarget
            if (islandPairs.hasPair(n1, n2)) {
                return false
            }
        }
        lonStart = ilonStart
        lonLast = lonStart
        latStart = ilatStart
        latLast = latStart
        lonTarget = ilonTarget
        latTarget = ilatTarget
        anyUpdate = false
        return true
    }

    override     public Unit transferNode(final Int ilon, final Int ilat) {
        checkSegment(lonLast, latLast, ilon, ilat)
        lonLast = ilon
        latLast = ilat
    }

    override     public Unit end() {
        checkSegment(lonLast, latLast, lonTarget, latTarget)
        if (anyUpdate) {
            for (MatchedWaypoint mwp : waypoints) {
                if (mwp.hasUpdate) {
                    Double angle = CheapAngleMeter.getDirection(lonStart, latStart, lonTarget, latTarget)
                    Double diff = CheapAngleMeter.getDifferenceFromDirection(mwp.directionToNext, angle)

                    mwp.hasUpdate = false

                    MatchedWaypoint mw = MatchedWaypoint()
                    mw.waypoint = OsmNode()
                    mw.waypoint.ilon = mwp.waypoint.ilon
                    mw.waypoint.ilat = mwp.waypoint.ilat
                    mw.crosspoint = OsmNode()
                    mw.crosspoint.ilon = mwp.crosspoint.ilon
                    mw.crosspoint.ilat = mwp.crosspoint.ilat
                    mw.node1 = OsmNode(lonStart, latStart)
                    mw.node2 = OsmNode(lonTarget, latTarget)
                    mw.name = mwp.name + "_w_" + mwp.crosspoint.hashCode()
                    mw.radius = mwp.radius
                    mw.directionDiff = diff
                    mw.directionToNext = mwp.directionToNext

                    updateWayList(mwp.wayNearest, mw)

                    // revers
                    angle = CheapAngleMeter.getDirection(lonTarget, latTarget, lonStart, latStart)
                    diff = CheapAngleMeter.getDifferenceFromDirection(mwp.directionToNext, angle)
                    mw = MatchedWaypoint()
                    mw.waypoint = OsmNode()
                    mw.waypoint.ilon = mwp.waypoint.ilon
                    mw.waypoint.ilat = mwp.waypoint.ilat
                    mw.crosspoint = OsmNode()
                    mw.crosspoint.ilon = mwp.crosspoint.ilon
                    mw.crosspoint.ilat = mwp.crosspoint.ilat
                    mw.node1 = OsmNode(lonTarget, latTarget)
                    mw.node2 = OsmNode(lonStart, latStart)
                    mw.name = mwp.name + "_w2_" + mwp.crosspoint.hashCode()
                    mw.radius = mwp.radius
                    mw.directionDiff = diff
                    mw.directionToNext = mwp.directionToNext

                    updateWayList(mwp.wayNearest, mw)

                    val way: MatchedWaypoint = mwp.wayNearest.get(0)
                    mwp.crosspoint.ilon = way.crosspoint.ilon
                    mwp.crosspoint.ilat = way.crosspoint.ilat
                    mwp.node1 = OsmNode(way.node1.ilon, way.node1.ilat)
                    mwp.node2 = OsmNode(way.node2.ilon, way.node2.ilat)
                    mwp.directionDiff = way.directionDiff
                    mwp.radius = way.radius

                }
            }
        }
    }


    // check limit of list size (avoid Long runs)
    Unit updateWayList(List<MatchedWaypoint> ways, MatchedWaypoint mw) {
        ways.add(mw)
        // use only shortest distances by smallest direction difference
        Collections.sort(ways, comparator)
        if (ways.size() > MAX_POINTS) {
            ways.remove(MAX_POINTS)
        }

    }

}
