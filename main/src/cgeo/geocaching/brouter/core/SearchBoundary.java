/**
 * static helper class for handling datafiles
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.mapaccess.OsmNode;


public final class SearchBoundary {

    public int direction;
    private final int minlon0;
    private final int minlat0;
    private final int maxlon0;
    private final int maxlat0;
    private final int minlon;
    private final int minlat;
    private final int maxlon;
    private final int maxlat;
    private final int radius;
    private final OsmNode p;

    /**
     * @param radius Search radius in meters.
     */
    public SearchBoundary(final OsmNode n, final int radius, final int direction) {
        this.radius = radius;
        this.direction = direction;

        p = new OsmNode(n.ilon, n.ilat);

        final int lon = (n.ilon / 5000000) * 5000000;
        final int lat = (n.ilat / 5000000) * 5000000;

        minlon0 = lon - 5000000;
        minlat0 = lat - 5000000;
        maxlon0 = lon + 10000000;
        maxlat0 = lat + 10000000;

        minlon = lon - 1000000;
        minlat = lat - 1000000;
        maxlon = lon + 6000000;
        maxlat = lat + 6000000;
    }

    public boolean isInBoundary(final OsmNode n, final int cost) {
        if (radius > 0) {
            return n.calcDistance(p) < radius;
        }
        if (cost == 0) {
            return n.ilon > minlon0 && n.ilon < maxlon0 && n.ilat > minlat0 && n.ilat < maxlat0;
        }
        return n.ilon > minlon && n.ilon < maxlon && n.ilat > minlat && n.ilat < maxlat;
    }

    public int getBoundaryDistance(final OsmNode n) {
        switch (direction) {
            case 0:
                return n.calcDistance(new OsmNode(n.ilon, minlat));
            case 1:
                return n.calcDistance(new OsmNode(minlon, n.ilat));
            case 2:
                return n.calcDistance(new OsmNode(n.ilon, maxlat));
            case 3:
                return n.calcDistance(new OsmNode(maxlon, n.ilat));
            default:
                throw new IllegalArgumentException("undefined direction: " + direction);
        }
    }

}
