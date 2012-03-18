package cgeo.geocaching.connector.gc;

import cgeo.geocaching.Settings;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.network.Network;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;

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

    public static final double LATITUDE_MIN = -85.05112878;
    public static final double LATITUDE_MAX = 85.05112878;
    public static final double LONGITUDE_MIN = -180;
    public static final double LONGITUDE_MAX = 180;

    public static final int TILE_SIZE = 256;
    public static final int ZOOMLEVEL_MAX = 18;
    public static final int ZOOMLEVEL_MIN = 0;

    public static final int[] NUMBER_OF_TILES = new int[ZOOMLEVEL_MAX - ZOOMLEVEL_MIN + 1];
    public static final int[] NUMBER_OF_PIXELS = new int[ZOOMLEVEL_MAX - ZOOMLEVEL_MIN + 1];
    static {
        for (int z = ZOOMLEVEL_MIN; z <= ZOOMLEVEL_MAX; z++) {
            NUMBER_OF_TILES[z] = 1 << z;
            NUMBER_OF_PIXELS[z] = TILE_SIZE * 1 << z;
        }
    }

    private final int tileX;
    private final int tileY;
    private final int zoomlevel;

    public Tile(Geopoint origin, int zoomlevel) {
        assert zoomlevel >= ZOOMLEVEL_MIN && zoomlevel <= ZOOMLEVEL_MAX : "zoomlevel out of range";

        this.zoomlevel = Math.max(Math.min(zoomlevel, ZOOMLEVEL_MAX), ZOOMLEVEL_MIN);

        tileX = calcX(origin);
        tileY = calcY(origin);
    }

    public Tile(int tileX, int tileY, int zoomlevel) {
        assert zoomlevel >= ZOOMLEVEL_MIN && zoomlevel <= ZOOMLEVEL_MAX : "zoomlevel out of range";

        this.zoomlevel = zoomlevel;
        this.tileX = tileX;
        this.tileY = tileY;
    }

    public int getZoomlevel() {
        return zoomlevel;
    }

    /**
     * Calculate the tile for a Geopoint based on the Spherical Mercator.
     *
     * @see http://developers.cloudmade.com/projects/tiles/examples/convert-coordinates-to-tile-numbers
     */
    private int calcX(final Geopoint origin) {
        return (int) ((origin.getLongitude() + 180.0) / 360.0 * NUMBER_OF_TILES[this.zoomlevel]);
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

        // double latRad = Math.toRadians(origin.getLatitude());
        // return (int) ((1 - (Math.log(Math.tan(latRad) + (1 / Math.cos(latRad))) / Math.PI)) / 2 * numberOfTiles);

        // Optimization from Bing
        double sinLatRad = Math.sin(Math.toRadians(origin.getLatitude()));
        return (int) ((0.5 - Math.log((1 + sinLatRad) / (1 - sinLatRad)) / (4 * Math.PI)) * NUMBER_OF_TILES[this.zoomlevel]);
    }

    /**
     * Calculate latitude/longitude for a given x/y position in this tile.
     *
     * @see http://developers.cloudmade.com/projects/tiles/examples/convert-coordinates-to-tile-numbers
     */
    public Geopoint getCoord(UTFGridPosition pos) {

        double pixX = tileX * TILE_SIZE + pos.x * 4;
        double pixY = tileY * TILE_SIZE + pos.y * 4;

        double lonDeg = ((360.0 * pixX) / NUMBER_OF_PIXELS[this.zoomlevel]) - 180.0;
        double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * pixY / NUMBER_OF_PIXELS[this.zoomlevel])));
        return new Geopoint(Math.toDegrees(latRad), lonDeg);
    }

    @Override
    public String toString() {
        return String.format("(%d/%d), zoom=%d", tileX, tileY, zoomlevel);
    }

    /**
     * Calculates the maximum possible zoom level where the supplied points
     * are covered by adjacent tiles on the east/west axis.
     * The order of the points (left/right) is irrelevant.
     *
     * @param left
     *            First point
     * @param right
     *            Second point
     * @return
     */
    public static int calcZoomLon(final Geopoint left, final Geopoint right) {

        int zoom = (int) Math.floor(
                Math.log(360.0 / Math.abs(left.getLongitude() - right.getLongitude()))
                        / Math.log(2)
                );

        Tile tileLeft = new Tile(left, zoom);
        Tile tileRight = new Tile(right, zoom);

        if (tileLeft.tileX == tileRight.tileX) {
            zoom = zoom + 1;
        }

        return Math.min(zoom, ZOOMLEVEL_MAX);
    }

    /**
     * Calculates the maximum possible zoom level where the supplied points
     * are covered by adjacent tiles on the north/south axis.
     * The order of the points (bottom/top) is irrelevant.
     *
     * @param bottom
     *            First point
     * @param top
     *            Second point
     * @return
     */
    public static int calcZoomLat(final Geopoint bottom, final Geopoint top) {

        int zoom = (int) Math.ceil(
                Math.log(2 * Math.PI /
                        Math.abs(
                                asinh(tanGrad(bottom.getLatitude()))
                                        - asinh(tanGrad(top.getLatitude()))
                                )
                        ) / Math.log(2)
                );

        Tile tileBottom = new Tile(bottom, zoom);
        Tile tileTop = new Tile(top, zoom);

        if (Math.abs(tileBottom.tileY - tileTop.tileY) > 1) {
            zoom = zoom - 1;
        }

        return Math.min(zoom, ZOOMLEVEL_MAX);
    }

    private static double tanGrad(double angleGrad) {
        return Math.tan(angleGrad / 180.0 * Math.PI);
    }

    /**
     * Calculates the inverted hyperbolic sine
     * (after Bronstein, Semendjajew: Taschenbuch der Mathematik
     *
     * @param x
     * @return
     */
    private static double asinh(double x) {
        return Math.log(x + Math.sqrt(x * x + 1.0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tile)) {
            return false;
        }
        return (this.tileX == ((Tile) o).tileX)
                && (this.tileY == ((Tile) o).tileY)
                && (this.zoomlevel == ((Tile) o).zoomlevel);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /** Request JSON informations for a tile */
    public static String requestMapInfo(final String url, final String referer) {
        final HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        request.addHeader("Referer", referer);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        return Network.getResponseData(Network.request(request), false);
    }

    /** Request .png image for a tile. */
    public static Bitmap requestMapTile(final String url, final String referer) {
        final HttpGet request = new HttpGet(url);
        request.addHeader("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
        request.addHeader("Referer", referer);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        final HttpResponse response = Network.request(request);
        try {
            return response != null ? BitmapFactory.decodeStream(response.getEntity().getContent()) : null;
        } catch (IOException e) {
            Log.e(Settings.tag, "cgBase.requestMapTile() " + e.getMessage());
        }
        return null;
    }
}
