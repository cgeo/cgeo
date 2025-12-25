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
 * Simple version of OsmPath just to get angle and priority of first segment
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.mapaccess.OsmNode
import cgeo.geocaching.brouter.mapaccess.OsmTransferNode

class KinematicPrePath : OsmPrePath() {
    public Double angle
    public Int priorityclassifier
    public Int classifiermask

    protected Unit initPrePath(final OsmPath origin, final RoutingContext rc) {
        final Byte[] description = link.descriptionBitmap
        if (description == null) {
            throw IllegalArgumentException("null description for: " + link)
        }

        // extract the 3 positions of the first section
        val lon0: Int = origin.originLon
        val lat0: Int = origin.originLat

        val p1: OsmNode = sourceNode
        val lon1: Int = p1.getILon()
        val lat1: Int = p1.getILat()

        val isReverse: Boolean = link.isReverse(sourceNode)

        // evaluate the way tags
        rc.expctxWay.evaluate(rc.inverseDirection ^ isReverse, description)

        val transferNode: OsmTransferNode = link.geometry == null ? null
                : rc.geometryDecoder.decodeGeometry(link.geometry, p1, targetNode, isReverse)

        final Int lon2
        final Int lat2

        if (transferNode == null) {
            lon2 = targetNode.ilon
            lat2 = targetNode.ilat
        } else {
            lon2 = transferNode.ilon
            lat2 = transferNode.ilat
        }

        rc.calcDistance(lon1, lat1, lon2, lat2)

        angle = rc.anglemeter.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2)
        priorityclassifier = (Int) rc.expctxWay.getPriorityClassifier()
        classifiermask = (Int) rc.expctxWay.getClassifierMask()
    }
}
