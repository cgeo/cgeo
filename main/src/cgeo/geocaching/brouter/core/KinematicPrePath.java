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

    protected void initPrePath(OsmPath origin, RoutingContext rc) {
        byte[] description = link.descriptionBitmap;
        if (description == null)
            throw new IllegalArgumentException("null description for: " + link);

        // extract the 3 positions of the first section
        int lon0 = origin.originLon;
        int lat0 = origin.originLat;

        OsmNode p1 = sourceNode;
        int lon1 = p1.getILon();
        int lat1 = p1.getILat();

        boolean isReverse = link.isReverse(sourceNode);

        // evaluate the way tags
        rc.expctxWay.evaluate(rc.inverseDirection ^ isReverse, description);

        OsmTransferNode transferNode = link.geometry == null ? null
            : rc.geometryDecoder.decodeGeometry(link.geometry, p1, targetNode, isReverse);

        int lon2;
        int lat2;

        if (transferNode == null) {
            lon2 = targetNode.ilon;
            lat2 = targetNode.ilat;
        } else {
            lon2 = transferNode.ilon;
            lat2 = transferNode.ilat;
        }

        int dist = rc.calcDistance(lon1, lat1, lon2, lat2);

        angle = rc.anglemeter.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2);
        priorityclassifier = (int) rc.expctxWay.getPriorityClassifier();
        classifiermask = (int) rc.expctxWay.getClassifierMask();
    }
}
