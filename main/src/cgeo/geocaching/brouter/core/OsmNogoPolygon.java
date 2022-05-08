/**********************************************************************************************
 Copyright (C) 2018 Norbert Truchsess norbert.truchsess@t-online.de

 The following methods are based on work of Dan Sunday published at:
 http://geomalgorithms.com/a03-_inclusion.html

 cn_PnPoly, wn_PnPoly, inSegment, intersect2D_2Segments
 **********************************************************************************************/
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.util.CheapRulerHelper;

import java.util.ArrayList;
import java.util.List;

public class OsmNogoPolygon extends OsmNodeNamed {
    public final List<Point> points = new ArrayList<>();
    public final boolean isClosed;

    public OsmNogoPolygon(final boolean closed) {
        this.isClosed = closed;
        this.isNogo = true;
        this.name = "";
    }

    public static boolean isOnLine(final long px, final long py, final long p0x, final long p0y, final long p1x, final long p1y) {
        final double v10x = px - p0x;
        final double v10y = py - p0y;
        final double v12x = p1x - p0x;
        final double v12y = p1y - p0y;

        if (v10x == 0) { // P0->P1 vertical?
            if (v10y == 0) { // P0 == P1?
                return true;
            }
            if (v12x != 0) { // P1->P2 not vertical?
                return false;
            }
            return (v12y / v10y) >= 1; // P1->P2 at least as long as P1->P0?
        }
        if (v10y == 0) { // P0->P1 horizontal?
            if (v12y != 0) { // P1->P2 not horizontal?
                return false;
            }
            // if ( P10x == 0 ) // P0 == P1? already tested
            return (v12x / v10x) >= 1; // P1->P2 at least as long as P1->P0?
        }
        final double kx = v12x / v10x;
        if (kx < 1) {
            return false;
        }
        return kx == v12y / v10y;
    }

    /**
     * inSegment(): determine if a point is inside a segment
     *
     * @param p     a point
     * @param segP0 starting point of segment
     * @param segP1 ending point of segment
     * @return 1 = P is inside S
     * 0 = P is not inside S
     */
    private static boolean inSegment(final Point p, final Point segP0, final Point segP1) {
        final int sp0x = segP0.x;
        final int sp1x = segP1.x;

        if (sp0x != sp1x) { // S is not vertical
            final int px = p.x;
            if (sp0x <= px && px <= sp1x) {
                return true;
            }
            return sp0x >= px && px >= sp1x;
        } else { // S is vertical, so test y coordinate
            final int sp0y = segP0.y;
            final int sp1y = segP1.y;
            final int py = p.y;

            if (sp0y <= py && py <= sp1y) {
                return true;
            }
            return sp0y >= py && py >= sp1y;
        }
    }

    /**
     * intersect2D_2Segments(): find the 2D intersection of 2 finite segments
     *
     * @param s1p0 start point of segment 1
     * @param s1p1 end point of segment 1
     * @param s2p0 start point of segment 2
     * @param s2p1 end point of segment 2
     * @return 0=disjoint (no intersect)
     * 1=intersect in unique point I0
     * 2=overlap in segment from I0 to I1
     */
    private static int intersect2D2Segments(final Point s1p0, final Point s1p1, final Point s2p0, final Point s2p1) {
        final long ux = s1p1.x - s1p0.x; // vector u = S1P1-S1P0 (segment 1)
        final long uy = s1p1.y - s1p0.y;
        final long vx = s2p1.x - s2p0.x; // vector v = S2P1-S2P0 (segment 2)
        final long vy = s2p1.y - s2p0.y;
        final long wx = s1p0.x - s2p0.x; // vector w = S1P0-S2P0 (from start of segment 2 to start of segment 1
        final long wy = s1p0.y - s2p0.y;

        final double d = ux * vy - uy * vx;

        // test if  they are parallel (includes either being a point)
        if (d == 0) {           // S1 and S2 are parallel
            if ((ux * wy - uy * wx) != 0 || (vx * wy - vy * wx) != 0) {
                return 0; // they are NOT collinear
            }

            // they are collinear or degenerate
            // check if they are degenerate  points
            final boolean du = (ux == 0) && (uy == 0);
            final boolean dv = (vx == 0) && (vy == 0);
            if (du && dv) {            // both segments are points
                return (wx == 0 && wy == 0) ? 0 : 1; // return 0 if they are distinct points
            }
            if (du) {                     // S1 is a single point
                return inSegment(s1p0, s2p0, s2p1) ? 1 : 0; // is it part of S2?
            }
            if (dv) {                    // S2 a single point
                return inSegment(s2p0, s1p0, s1p1) ? 1 : 0;  // is it part of S1?
            }
            // they are collinear segments - get  overlap (or not)
            double t0;                    // endpoints of S1 in eqn for S2
            double t1;
            final int w2x = s1p1.x - s2p0.x; // vector w2 = S1P1-S2P0 (from start of segment 2 to end of segment 1)
            final int w2y = s1p1.y - s2p0.y;
            if (vx != 0) {
                t0 = wx / vx;
                t1 = w2x / vx;
            } else {
                t0 = wy / vy;
                t1 = w2y / vy;
            }
            if (t0 > t1) {                   // must have t0 smaller than t1
                final double t = t0;     // swap if not
                t0 = t1;
                t1 = t;
            }
            if (t0 > 1 || t1 < 0) {
                return 0;      // NO overlap
            }
            t0 = t0 < 0 ? 0 : t0;               // clip to min 0
            t1 = t1 > 1 ? 1 : t1;               // clip to max 1

            return (t0 == t1) ? 1 : 2;        // return 1 if intersect is a point
        }

        // the segments are skew and may intersect in a point
        // get the intersect parameter for S1

        final double sI = (vx * wy - vy * wx) / d;
        if (sI < 0 || sI > 1) {              // no intersect with S1
            return 0;
        }

        // get the intersect parameter for S2
        final double tI = (ux * wy - uy * wx) / d;
        return (tI < 0 || tI > 1) ? 0 : 1; // return 0 if no intersect with S2
    }

    public final void addVertex(final int lon, final int lat) {
        points.add(new Point(lon, lat));
    }

    /**
     * calcBoundingCircle is inspired by the algorithm described on
     * http://geomalgorithms.com/a08-_containers.html
     * (fast computation of bounding circly in c). It is not as fast (the original
     * algorithm runs in linear time), as it may do more iterations but it takes
     * into account the coslat-factor being used for the linear approximation that
     * is also used in other places of brouter does change when moving the centerpoint
     * with each iteration.
     * This is done to ensure the calculated radius being used
     * in RoutingContext.calcDistance will actually contain the whole polygon.
     * <p>
     * For reasonable distributed vertices the implemented algorithm runs in O(n*ln(n)).
     * As this is only run once on initialization of OsmNogoPolygon this methods
     * overall usage of cpu is neglegible in comparism to the cpu-usage of the
     * actual routing algoritm.
     */
    public void calcBoundingCircle() {
        int cxmin = Integer.MAX_VALUE;
        int cxmax = Integer.MIN_VALUE;
        int cymin = Integer.MAX_VALUE;
        int cymax = Integer.MIN_VALUE;

        // first calculate a starting center point as center of boundingbox
        for (int i = 0; i < points.size(); i++) {
            final Point p = points.get(i);
            if (p.x < cxmin) {
                cxmin = p.x;
            }
            if (p.x > cxmax) {
                cxmax = p.x;
            }
            if (p.y < cymin) {
                cymin = p.y;
            }
            if (p.y > cymax) {
                cymax = p.y;
            }
        }

        int cx = (cxmax + cxmin) / 2; // center of circle
        int cy = (cymax + cymin) / 2;

        double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales(cy); // conversion-factors at the center of circle
        double dlon2m = lonlat2m[0];
        double dlat2m = lonlat2m[1];

        double rad = 0;  // radius

        double dmax = 0; // length of vector from center to point
        int iMax = -1;

        do {
            // now identify the point outside of the circle that has the greatest distance
            for (int i = 0; i < points.size(); i++) {
                final Point p = points.get(i);

                // to get precisely the same results as in RoutingContext.calcDistance()
                // it's crucial to use the factors of the center!
                final double x1 = (cx - p.x) * dlon2m;
                final double y1 = (cy - p.y) * dlat2m;
                final double dist = Math.sqrt(x1 * x1 + y1 * y1);
                if (dist <= rad) {
                    continue;
                }
                if (dist > dmax) {
                    // new maximum distance found
                    dmax = dist;
                    iMax = i;
                }
            }
            if (iMax < 0) {
                break; // leave loop when no point outside the circle is found any more.
            }
            final double dd = 0.5 * (1 - rad / dmax);

            final Point p = points.get(iMax); // calculate new radius to just include this point
            cx += (int) (dd * (p.x - cx) + 0.5); // shift center toward point
            cy += (int) (dd * (p.y - cy) + 0.5);

            // get new factors at shifted centerpoint
            lonlat2m = CheapRulerHelper.getLonLatToMeterScales(cy);
            dlon2m = lonlat2m[0];
            dlat2m = lonlat2m[1];

            final double x1 = (cx - p.x) * dlon2m;
            final double y1 = (cy - p.y) * dlat2m;
            rad = Math.sqrt(x1 * x1 + y1 * y1);
            dmax = rad;
            iMax = -1;
        }
        while (true);

        ilon = cx;
        ilat = cy;
        radius = rad * 1.001 + 1.0; // ensure the outside-of-enclosing-circle test in RoutingContext.calcDistance() is not passed by segments ending very close to the radius due to limited numerical precision
    }

    /**
     * tests whether a segment defined by lon and lat of two points does either
     * intersect the polygon or any of the endpoints (or both) are enclosed by
     * the polygon. For this test the winding-number algorithm is
     * being used. That means a point being within an overlapping region of the
     * polygon is also taken as being 'inside' the polygon.
     *
     * @param lon0 longitude of start point
     * @param lat0 latitude of start point
     * @param lon1 longitude of end point
     * @param lat1 latitude of start point
     * @return true if segment or any of it's points are 'inside' of polygon
     */
    public boolean intersects(final int lon0, final int lat0, final int lon1, final int lat1) {
        final Point p0 = new Point(lon0, lat0);
        final Point p1 = new Point(lon1, lat1);
        final int iLast = points.size() - 1;
        Point p2 = points.get(isClosed ? iLast : 0);
        for (int i = isClosed ? 0 : 1; i <= iLast; i++) {
            final Point p3 = points.get(i);
            // does it intersect with at least one of the polygon's segments?
            if (intersect2D2Segments(p0, p1, p2, p3) > 0) {
                return true;
            }
            p2 = p3;
        }
        return false;
    }

/* Copyright 2001 softSurfer, 2012 Dan Sunday, 2018 Norbert Truchsess
   This code may be freely used and modified for any purpose providing that
   this copyright notice is included with it. SoftSurfer makes no warranty for
   this code, and cannot be held liable for any real or imagined damage
   resulting from its use. Users of this code must verify correctness for
   their application. */

    public boolean isOnPolyline(final long px, final long py) {
        final int iLast = points.size() - 1;
        Point p1 = points.get(0);
        for (int i = 1; i <= iLast; i++) {
            final Point p2 = points.get(i);
            if (OsmNogoPolygon.isOnLine(px, py, p1.x, p1.y, p2.x, p2.y)) {
                return true;
            }
            p1 = p2;
        }
        return false;
    }

    /**
     * winding number test for a point in a polygon
     *
     * @param px longitude of the point to check
     * @param py latitude of the point to check
     * @return a boolean whether the point is within the polygon or not.
     */
    public boolean isWithin(final long px, final long py) {
        int wn = 0; // the winding number counter

        // loop through all edges of the polygon
        final int iLast = points.size() - 1;
        final Point p0 = points.get(isClosed ? iLast : 0);
        long p0x = p0.x; // need to use long to avoid overflow in products
        long p0y = p0.y;

        for (int i = isClosed ? 0 : 1; i <= iLast; i++) { // edge from v[i] to v[i+1]
            final Point p1 = points.get(i);

            final long p1x = p1.x;
            final long p1y = p1.y;

            if (OsmNogoPolygon.isOnLine(px, py, p0x, p0y, p1x, p1y)) {
                return true;
            }

            final long intersect = (p1x - p0x) * (py - p0y) - (px - p0x) * (p1y - p0y);
            if (p0y <= py) {  // start y <= p.y
                // an upward crossing, p left of edge
                // have a valid up intersect
                if (p1y > py && intersect > 0) {
                    ++wn;
                }
            } else { // start y > p.y (no test needed)
                // a downward crossing, p right of edge
                // have a valid down intersect
                if (p1y <= py && intersect < 0) {
                    --wn;
                }
            }
            p0x = p1x;
            p0y = p1y;
        }
        return wn != 0;
    }

/* Copyright 2001 softSurfer, 2012 Dan Sunday, 2018 Norbert Truchsess
   This code may be freely used and modified for any purpose providing that
   this copyright notice is included with it. SoftSurfer makes no warranty for
   this code, and cannot be held liable for any real or imagined damage
   resulting from its use. Users of this code must verify correctness for
   their application. */

    /**
     * Compute the length of the segment within the polygon.
     *
     * @param lon1 Integer longitude of the first point of the segment.
     * @param lat1 Integer latitude of the first point of the segment.
     * @param lon2 Integer longitude of the last point of the segment.
     * @param lat2 Integer latitude of the last point of the segment.
     * @return The length, in meters, of the portion of the segment which is
     * included in the polygon.
     */
    public double distanceWithinPolygon(final int lon1, final int lat1, final int lon2, final int lat2) {
        double distance = 0.;

        // Extremities of the segments
        final Point p1 = new Point(lon1, lat1);
        final Point p2 = new Point(lon2, lat2);

        Point previousIntersectionOnSegment = null;
        if (isWithin(lon1, lat1)) {
            // Start point of the segment is within the polygon, this is the first
            // "intersection".
            previousIntersectionOnSegment = p1;
        }

        // Loop over edges of the polygon to find intersections
        final int iLast = points.size() - 1;
        for (int i = isClosed ? 0 : 1, j = isClosed ? iLast : 0; i <= iLast; j = i++) {
            final Point edgePoint1 = points.get(j);
            final Point edgePoint2 = points.get(i);
            final int intersectsEdge = intersect2D2Segments(p1, p2, edgePoint1, edgePoint2);

            if (isClosed && intersectsEdge == 1) {
                // Intersects with a (closed) polygon edge on a single point
                // Distance is zero when crossing a polyline.
                // Let's find this intersection point
                final int xdiffSegment = lon1 - lon2;
                final int xdiffEdge = edgePoint1.x - edgePoint2.x;
                final int ydiffSegment = lat1 - lat2;
                final int ydiffEdge = edgePoint1.y - edgePoint2.y;
                final int div = xdiffSegment * ydiffEdge - xdiffEdge * ydiffSegment;
                final long dSegment = (long) lon1 * (long) lat2 - (long) lon2 * (long) lat1;
                final long dEdge = (long) edgePoint1.x * (long) edgePoint2.y - (long) edgePoint2.x * (long) edgePoint1.y;
                // Coordinates of the intersection
                final Point intersection = new Point(
                        (int) ((dSegment * xdiffEdge - dEdge * xdiffSegment) / div),
                        (int) ((dSegment * ydiffEdge - dEdge * ydiffSegment) / div)
                );
                if (
                        previousIntersectionOnSegment != null
                                && isWithin(
                                (intersection.x + previousIntersectionOnSegment.x) >> 1,
                                (intersection.y + previousIntersectionOnSegment.y) >> 1
                        )
                ) {
                    // There was a previous match within the polygon and this part of the
                    // segment is within the polygon.
                    distance += CheapRulerHelper.distance(
                            previousIntersectionOnSegment.x, previousIntersectionOnSegment.y,
                            intersection.x, intersection.y
                    );
                }
                previousIntersectionOnSegment = intersection;
            } else if (intersectsEdge == 2) {
                // Segment and edge overlaps
                // FIXME: Could probably be done in a smarter way
                distance += Math.min(
                        CheapRulerHelper.distance(p1.x, p1.y, p2.x, p2.y),
                        Math.min(
                                CheapRulerHelper.distance(edgePoint1.x, edgePoint1.y, edgePoint2.x, edgePoint2.y),
                                Math.min(
                                        CheapRulerHelper.distance(p1.x, p1.y, edgePoint2.x, edgePoint2.y),
                                        CheapRulerHelper.distance(edgePoint1.x, edgePoint1.y, p2.x, p2.y)
                                )
                        )
                );
                // FIXME: We could store intersection.
                previousIntersectionOnSegment = null;
            }
        }

        if (
                previousIntersectionOnSegment != null
                        && isWithin(lon2, lat2)
        ) {
            // Last point is within the polygon, add the remaining missing distance.
            distance += CheapRulerHelper.distance(
                    previousIntersectionOnSegment.x, previousIntersectionOnSegment.y,
                    lon2, lat2
            );
        }
        return distance;
    }

/* Copyright 2001 softSurfer, 2012 Dan Sunday, 2018 Norbert Truchsess
   This code may be freely used and modified for any purpose providing that
   this copyright notice is included with it. SoftSurfer makes no warranty for
   this code, and cannot be held liable for any real or imagined damage
   resulting from its use. Users of this code must verify correctness for
   their application. */

    public static final class Point {
        public final int y;
        public final int x;

        Point(final int lon, final int lat) {
            x = lon;
            y = lat;
        }
    }
}
