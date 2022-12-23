/**
 * Simple version of OsmPath just to get angle and priority of first segment
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.mapaccess.OsmNode;
import cgeo.geocaching.brouter.mapaccess.OsmTransferNode;

final class KinematicPrePath extends OsmPrePath {
    public double angle;
    public int priorityclassifier;
    public int classifiermask;

    protected void initPrePath(final OsmPath origin, final RoutingContext rc) {
        final byte[] description = link.descriptionBitmap;
        if (description == null) {
            throw new IllegalArgumentException("null description for: " + link);
        }

        // extract the 3 positions of the first section
        final int lon0 = origin.originLon;
        final int lat0 = origin.originLat;

        final OsmNode p1 = sourceNode;
        final int lon1 = p1.getILon();
        final int lat1 = p1.getILat();

        final boolean isReverse = link.isReverse(sourceNode);

        // evaluate the way tags
        rc.expctxWay.evaluate(rc.inverseDirection ^ isReverse, description);

        final OsmTransferNode transferNode = link.geometry == null ? null
                : rc.geometryDecoder.decodeGeometry(link.geometry, p1, targetNode, isReverse);

        final int lon2;
        final int lat2;

        if (transferNode == null) {
            lon2 = targetNode.ilon;
            lat2 = targetNode.ilat;
        } else {
            lon2 = transferNode.ilon;
            lat2 = transferNode.ilat;
        }

        final int dist = rc.calcDistance(lon1, lat1, lon2, lat2);

        angle = rc.anglemeter.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2);
        priorityclassifier = (int) rc.expctxWay.getPriorityClassifier();
        classifiermask = (int) rc.expctxWay.getClassifierMask();
    }
}
