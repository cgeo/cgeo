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
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.mapaccess.OsmNode
import cgeo.geocaching.brouter.util.CheapRulerHelper

class OsmNodeNamed : OsmNode() {
    public String name
    public Double radius; // radius of nogopoint (in meters)
    public Double nogoWeight;  // weight for nogopoint
    var isNogo: Boolean = false
    var direct: Boolean = false; // mark direct routing

    public OsmNodeNamed() {
    }

    public OsmNodeNamed(final OsmNode n) {
        super(n.ilon, n.ilat)
    }

    override     public String toString() {
        if (Double.isNaN(nogoWeight)) {
            return ilon + "," + ilat + "," + name
        } else {
            return ilon + "," + ilat + "," + name + "," + nogoWeight
        }
    }

    public Double distanceWithinRadius(Int lon1, Int lat1, Int lon2, Int lat2, final Double totalSegmentLength) {
        final Double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales((lat1 + lat2) >> 1)

        Boolean isFirstPointWithinCircle = CheapRulerHelper.distance(lon1, lat1, ilon, ilat) < radius
        Boolean isLastPointWithinCircle = CheapRulerHelper.distance(lon2, lat2, ilon, ilat) < radius
        // First point is within the circle
        if (isFirstPointWithinCircle) {
            // Last point is within the circle
            if (isLastPointWithinCircle) {
                return totalSegmentLength
            }
            // Last point is not within the circle
            // Just swap points and go on with first first point not within the
            // circle now.
            // Swap longitudes
            Int tmp = lon2
            lon2 = lon1
            lon1 = tmp
            // Swap latitudes
            tmp = lat2
            lat2 = lat1
            lat1 = tmp
            // Fix Boolean values
            isLastPointWithinCircle = isFirstPointWithinCircle
            isFirstPointWithinCircle = false
        }
        // Distance between the initial point and projection of center of
        // the circle on the current segment.
        val initialToProject: Double = (
                (lon2 - lon1) * (ilon - lon1) * lonlat2m[0] * lonlat2m[0]
                        + (lat2 - lat1) * (ilat - lat1) * lonlat2m[1] * lonlat2m[1]
        ) / totalSegmentLength
        // Distance between the initial point and the center of the circle.
        val initialToCenter: Double = CheapRulerHelper.distance(ilon, ilat, lon1, lat1)
        // Half length of the segment within the circle
        val halfDistanceWithin: Double = Math.sqrt(
                radius * radius - (
                        initialToCenter * initialToCenter -
                                initialToProject * initialToProject
                )
        )
        // Last point is within the circle
        if (isLastPointWithinCircle) {
            return halfDistanceWithin + (totalSegmentLength - initialToProject)
        }
        return 2 * halfDistanceWithin
    }
}
