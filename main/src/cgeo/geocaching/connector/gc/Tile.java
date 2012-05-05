package cgeo.geocaching.connector.gc;

import cgeo.geocaching.ICoordinates;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.LeastRecentlyUsedMap;
import cgeo.geocaching.utils.Log;

import org.apache.http.HttpResponse;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * All about tiles.
 *
 * @author blafoo
 *
 * @see <a href="http://msdn.microsoft.com/en-us/library/bb259689.aspx">MSDN</a>
 * @see <a
 *      href="http://svn.openstreetmap.org/applications/viewer/jmapviewer/src/org/openstreetmap/gui/jmapviewer/OsmMercator.java">OSM</a>
 */
public class Tile {

    public static final double LATITUDE_MIN = -85.05112878;
    public static final double LATITUDE_MAX = 85.05112878;
    public static final double LONGITUDE_MIN = -180;
    public static final double LONGITUDE_MAX = 180;

    public static final int TILE_SIZE = 256;
    public static final int ZOOMLEVEL_MAX = 18;
    public static final int ZOOMLEVEL_MIN = 0;

    static final int[] NUMBER_OF_TILES = new int[ZOOMLEVEL_MAX - ZOOMLEVEL_MIN + 1];
    static final int[] NUMBER_OF_PIXELS = new int[ZOOMLEVEL_MAX - ZOOMLEVEL_MIN + 1];
    static {
        for (int z = ZOOMLEVEL_MIN; z <= ZOOMLEVEL_MAX; z++) {
            NUMBER_OF_TILES[z] = 1 << z;
            NUMBER_OF_PIXELS[z] = TILE_SIZE * 1 << z;
        }
    }

    private final int tileX;
    private final int tileY;
    private final int zoomlevel;
    private final Viewport viewPort;

    public Tile(Geopoint origin, int zoomlevel) {

        this.zoomlevel = Math.max(Math.min(zoomlevel, ZOOMLEVEL_MAX), ZOOMLEVEL_MIN);

        tileX = calcX(origin);
        tileY = calcY(origin);

        viewPort = new Viewport(getCoord(new UTFGridPosition(0, 0)), getCoord(new UTFGridPosition(63, 63)));
    }

    public int getZoomlevel() {
        return zoomlevel;
    }

    /**
     * Calculate the tile for a Geopoint based on the Spherical Mercator.
     *
     * @see <a
     *      href="http://developers.cloudmade.com/projects/tiles/examples/convert-coordinates-to-tile-numbers">Cloudmade</a>
     */
    private int calcX(final Geopoint origin) {
        return (int) ((origin.getLongitude() + 180.0) / 360.0 * NUMBER_OF_TILES[this.zoomlevel]);
    }

    /**
     * Calculate the tile for a Geopoint based on the Spherical Mercator.
     *
     */
    private int calcY(final Geopoint origin) {

        // double latRad = Math.toRadians(origin.getLatitude());
        // return (int) ((1 - (Math.log(Math.tan(latRad) + (1 / Math.cos(latRad))) / Math.PI)) / 2 * numberOfTiles);

        // Optimization from Bing
        double sinLatRad = Math.sin(Math.toRadians(origin.getLatitude()));
        return (int) ((0.5 - Math.log((1 + sinLatRad) / (1 - sinLatRad)) / (4 * Math.PI)) * NUMBER_OF_TILES[this.zoomlevel]);
    }

    public int getX() {
        return tileX;
    }

    public int getY() {
        return tileY;
    }

    /**
     * Calculate latitude/longitude for a given x/y position in this tile.
     * 
     * @see <a
     *      href="http://developers.cloudmade.com/projects/tiles/examples/convert-coordinates-to-tile-numbers">Cloudmade</a>
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
            zoom += 1;
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
                Math.log(2.0 * Math.PI /
                        Math.abs(
                                asinh(tanGrad(bottom.getLatitude()))
                                        - asinh(tanGrad(top.getLatitude()))
                                )
                        ) / Math.log(2)
                );

        Tile tileBottom = new Tile(bottom, zoom);
        Tile tileTop = new Tile(top, zoom);

        if (Math.abs(tileBottom.tileY - tileTop.tileY) > 1) {
            zoom -= 1;
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
    public static String requestMapInfo(final String url, final Parameters params, final String referer) {
        return Network.getResponseData(Network.getRequest(url, params, new Parameters("Referer", referer)));
    }

    /** Request .png image for a tile. */
    public static Bitmap requestMapTile(final Parameters params) {
        final HttpResponse response = Network.getRequest(GCConstants.URL_MAP_TILE, params, new Parameters("Referer", GCConstants.URL_LIVE_MAP));
        try {
            return response != null ? BitmapFactory.decodeStream(response.getEntity().getContent()) : null;
        } catch (IOException e) {
            Log.e("cgBase.requestMapTile() " + e.getMessage());
        }
        return null;
    }

    public boolean containsPoint(final ICoordinates point) {
        return viewPort.contains(point);
    }

    /**
     * Calculate needed tiles for the given viewport
     *
     * @param viewport
     * @return
     */
    protected static Set<Tile> getTilesForViewport(final Viewport viewport) {
        Set<Tile> tiles = new HashSet<Tile>();
        int zoom = Math.min(Tile.calcZoomLon(viewport.bottomLeft, viewport.topRight),
                Tile.calcZoomLat(viewport.bottomLeft, viewport.topRight));
        tiles.add(new Tile(viewport.bottomLeft, zoom));
        tiles.add(new Tile(new Geopoint(viewport.getLatitudeMin(), viewport.getLongitudeMax()), zoom));
        tiles.add(new Tile(new Geopoint(viewport.getLatitudeMax(), viewport.getLongitudeMin()), zoom));
        tiles.add(new Tile(viewport.topRight, zoom));
        return tiles;
    }

    public static class Cache {
        private final static LeastRecentlyUsedMap<Integer, Tile> tileCache = new LeastRecentlyUsedMap.LruCache<Integer, Tile>(64);

        public static void removeFromTileCache(final ICoordinates point) {
            if (point != null) {
                Collection<Tile> tiles = new ArrayList<Tile>(tileCache.values());
                for (Tile tile : tiles) {
                    if (tile.containsPoint(point)) {
                        tileCache.remove(tile.hashCode());
                    }
                }
            }
        }

        public static boolean contains(final Tile tile) {
            return tileCache.containsKey(tile.hashCode());
        }

        public static void add(final Tile tile) {
            tileCache.put(tile.hashCode(), tile);
        }
    }

}
