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
    public static final int ZOOMLEVEL_MAX = 18;
    public static final int ZOOMLEVEL_MIN = 0;

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

    /**
     * Calculate latitude/longitude for a given x/y position in this tile.
     *
     * @see http://developers.cloudmade.com/projects/tiles/examples/convert-coordinates-to-tile-numbers
     */
    public Geopoint getCoord(UTFGridPosition pos) {

        double pixX = tileX * TILE_SIZE + pos.x * 4;
        double pixY = tileY * TILE_SIZE + pos.y * 4;
        long numberOfPixels = TILE_SIZE * numberOfTiles;

        double lon = ((360.0 * pixX) / numberOfPixels) - 180.0;
        double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * pixY / numberOfPixels)));
        return new Geopoint(latRad * Geopoint.rad2deg, lon);
    }

    @Override
    public String toString() {
        return String.format("(%d/%d), zoom=%d", tileX, tileY, zoomlevel).toString();

    }

}
