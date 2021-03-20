/**
 * static helper class for handling datafiles
 *
 * @author ab
 */
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.mapaccess.OsmNode;


public final class SearchBoundary {

    int direction;
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
    public SearchBoundary(OsmNode n, int radius, int direction) {
        this.radius = radius;
        this.direction = direction;

        p = new OsmNode(n.ilon, n.ilat);

        int lon = (n.ilon / 5000000) * 5000000;
        int lat = (n.ilat / 5000000) * 5000000;

        minlon0 = lon - 5000000;
        minlat0 = lat - 5000000;
        maxlon0 = lon + 10000000;
        maxlat0 = lat + 10000000;

        minlon = lon - 1000000;
        minlat = lat - 1000000;
        maxlon = lon + 6000000;
        maxlat = lat + 6000000;
    }

    public static String getFileName(OsmNode n) {
        int lon = (n.ilon / 5000000) * 5000000;
        int lat = (n.ilat / 5000000) * 5000000;

        int dlon = lon / 1000000 - 180;
        int dlat = lat / 1000000 - 90;

        String slon = dlon < 0 ? "W" + (-dlon) : "E" + dlon;
        String slat = dlat < 0 ? "S" + (-dlat) : "N" + dlat;
        return slon + "_" + slat + ".trf";
    }

    public boolean isInBoundary(OsmNode n, int cost) {
        if (radius > 0) {
            return n.calcDistance(p) < radius;
        }
        if (cost == 0) {
            return n.ilon > minlon0 && n.ilon < maxlon0 && n.ilat > minlat0 && n.ilat < maxlat0;
        }
        return n.ilon > minlon && n.ilon < maxlon && n.ilat > minlat && n.ilat < maxlat;
    }

    public int getBoundaryDistance(OsmNode n) {
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
