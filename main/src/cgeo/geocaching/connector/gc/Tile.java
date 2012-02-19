package cgeo.geocaching.connector.gc;

import cgeo.geocaching.geopoint.Geopoint;

/**
 * All about tiles.
 *
 * @author blafoo
 *
 * @see http://msdn.microsoft.com/en-us/library/bb259689.aspx
 * @see http
 *      ://svn.openstreetmap.org/applications/viewer/jmapviewer/src/org/openstreetmap/gui/jmapviewer/OsmMercator.java
 */
public class Tile {

    public static final int TILE_SIZE = 256;

    private final int tileX;
    private final int tileY;
    private final int zoomlevel;
    private final int numberOfTiles;

    public Tile(Geopoint origin, int zoomlevel) {
        this.zoomlevel = zoomlevel;
        numberOfTiles = 1 << zoomlevel;
        tileX = calcX(origin);
        tileY = calcY(origin);
    }

    public Tile(int tileX, int tileY, int zoomlevel) {
        this.zoomlevel = zoomlevel;
        numberOfTiles = 1 << zoomlevel;
        this.tileX = tileX;
        this.tileY = tileY;
    }

    public long getZoomlevel() {
        return zoomlevel;
    }

    /**
     * Calculate the tile for a Geopoint based on the Spherical Mercator.
     *
     * @see http://developers.cloudmade.com/projects/tiles/examples/convert-coordinates-to-tile-numbers
     */
    private int calcX(final Geopoint origin) {
        return (int) ((origin.getLongitude() + 180.0) / 360.0 * numberOfTiles);
    }

    public int getX() {
        return tileX;
    }

    public int getY() {
        return tileY;
    }

    /**
     * Calculate the tile for a Geopoint based on the Spherical Mercator.
     *
     * @see http://developers.cloudmade.com/projects/tiles/examples/convert-coordinates-to-tile-numbers
     */
    private int calcY(final Geopoint origin) {
        double lat_rad = Math.toRadians(origin.getLatitude());

        return (int) ((1 - (Math.log(Math.tan(lat_rad) + (1 / Math.cos(lat_rad))) / Math.PI)) / 2 * numberOfTiles);
    }

    /** Calculate latitude/longitude for a given x/y position in this tile. */
    public Geopoint getCoord(UTFGridPosition pos) {

        long numberOfPixels = TILE_SIZE * numberOfTiles;

        double pixX = tileX * TILE_SIZE + pos.x * 4;
        double pixY = tileY * TILE_SIZE + pos.y * 4;

        pixY += -1 * numberOfPixels / 2;
        double radius = numberOfPixels / (2.0 * Math.PI);
        double lat = (Math.PI / 2) - (2 * Math.atan(Math.exp(-1.0 * pixY / radius)));
        lat = -1 * Math.toDegrees(lat);

        double lon = ((360.0 * pixX) / numberOfPixels) - 180.0;
        return new Geopoint(lat, lon);
    }

}
