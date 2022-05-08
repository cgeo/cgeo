/**
 * Container for an osm node
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.mapaccess.OsmNode;
import cgeo.geocaching.brouter.util.CheapRulerHelper;

public class OsmNodeNamed extends OsmNode {
    public String name;
    public double radius; // radius of nogopoint (in meters)
    public double nogoWeight;  // weight for nogopoint
    public boolean isNogo = false;

    public OsmNodeNamed() {
    }

    public OsmNodeNamed(final OsmNode n) {
        super(n.ilon, n.ilat);
    }

    public static OsmNodeNamed decodeNogo(final String s) {
        final OsmNodeNamed n = new OsmNodeNamed();
        final int idx1 = s.indexOf(',');
        n.ilon = Integer.parseInt(s.substring(0, idx1));
        final int idx2 = s.indexOf(',', idx1 + 1);
        n.ilat = Integer.parseInt(s.substring(idx1 + 1, idx2));
        final int idx3 = s.indexOf(',', idx2 + 1);
        if (idx3 == -1) {
            n.name = s.substring(idx2 + 1);
            n.nogoWeight = Double.NaN;
        } else {
            n.name = s.substring(idx2 + 1, idx3);
            n.nogoWeight = Double.parseDouble(s.substring(idx3 + 1));
        }
        n.isNogo = true;
        return n;
    }

    @Override
    public String toString() {
        if (Double.isNaN(nogoWeight)) {
            return ilon + "," + ilat + "," + name;
        } else {
            return ilon + "," + ilat + "," + name + "," + nogoWeight;
        }
    }

    public double distanceWithinRadius(int lon1, int lat1, int lon2, int lat2, final double totalSegmentLength) {
        final double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales((lat1 + lat2) >> 1);

        boolean isFirstPointWithinCircle = CheapRulerHelper.distance(lon1, lat1, ilon, ilat) < radius;
        boolean isLastPointWithinCircle = CheapRulerHelper.distance(lon2, lat2, ilon, ilat) < radius;
        // First point is within the circle
        if (isFirstPointWithinCircle) {
            // Last point is within the circle
            if (isLastPointWithinCircle) {
                return totalSegmentLength;
            }
            // Last point is not within the circle
            // Just swap points and go on with first first point not within the
            // circle now.
            // Swap longitudes
            int tmp = lon2;
            lon2 = lon1;
            lon1 = tmp;
            // Swap latitudes
            tmp = lat2;
            lat2 = lat1;
            lat1 = tmp;
            // Fix boolean values
            isLastPointWithinCircle = isFirstPointWithinCircle;
            isFirstPointWithinCircle = false;
        }
        // Distance between the initial point and projection of center of
        // the circle on the current segment.
        final double initialToProject = (
                (lon2 - lon1) * (ilon - lon1) * lonlat2m[0] * lonlat2m[0]
                        + (lat2 - lat1) * (ilat - lat1) * lonlat2m[1] * lonlat2m[1]
        ) / totalSegmentLength;
        // Distance between the initial point and the center of the circle.
        final double initialToCenter = CheapRulerHelper.distance(ilon, ilat, lon1, lat1);
        // Half length of the segment within the circle
        final double halfDistanceWithin = Math.sqrt(
                radius * radius - (
                        initialToCenter * initialToCenter -
                                initialToProject * initialToProject
                )
        );
        // Last point is within the circle
        if (isLastPointWithinCircle) {
            return halfDistanceWithin + (totalSegmentLength - initialToProject);
        }
        return 2 * halfDistanceWithin;
    }
}
