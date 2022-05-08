package cgeo.geocaching.connector.gc;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.models.ICoordinates;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.LeastRecentlyUsedSet;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import io.reactivex.rxjava3.core.Single;
import okhttp3.Response;

/**
 * All about tiles.
 *
 * @see <a href="http://msdn.microsoft.com/en-us/library/bb259689.aspx">MSDN</a>
 * @see <a
 * href="http://svn.openstreetmap.org/applications/viewer/jmapviewer/src/org/openstreetmap/gui/jmapviewer/OsmMercator.java">OSM</a>
 */
public class Tile {

    static final int TILE_SIZE = 256;
    static final int ZOOMLEVEL_MAX = 18;
    public static final int ZOOMLEVEL_MIN = 0;
    public static final int ZOOMLEVEL_MIN_PERSONALIZED = 12;

    private static final int[] NUMBER_OF_TILES = new int[ZOOMLEVEL_MAX - ZOOMLEVEL_MIN + 1];
    private static final int[] NUMBER_OF_PIXELS = new int[ZOOMLEVEL_MAX - ZOOMLEVEL_MIN + 1];

    static {
        for (int z = ZOOMLEVEL_MIN; z <= ZOOMLEVEL_MAX; z++) {
            NUMBER_OF_TILES[z] = 1 << z;
            NUMBER_OF_PIXELS[z] = TILE_SIZE << z;
        }
    }

    public static final TileCache cache = new TileCache();

    private final int tileX;
    private final int tileY;
    private final int zoomLevel;
    private final Viewport viewPort;

    public Tile(final Geopoint origin, final int zoomlevel) {
        this(calcX(origin, clippedZoomlevel(zoomlevel)), calcY(origin, clippedZoomlevel(zoomlevel)), clippedZoomlevel(zoomlevel));
    }

    private Tile(final int tileX, final int tileY, final int zoomlevel) {

        this.zoomLevel = clippedZoomlevel(zoomlevel);

        this.tileX = tileX;
        this.tileY = tileY;

        viewPort = new Viewport(getCoord(new UTFGridPosition(0, 0)), getCoord(new UTFGridPosition(63, 63)));
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    private static int clippedZoomlevel(final int zoomlevel) {
        return Math.max(Math.min(zoomlevel, ZOOMLEVEL_MAX), ZOOMLEVEL_MIN);
    }

    /**
     * Calculate the tile for a Geopoint based on the Spherical Mercator.
     *
     * @see <a
     * href="http://developers.cloudmade.com/projects/tiles/examples/convert-coordinates-to-tile-numbers">Cloudmade</a>
     */
    private static int calcX(final Geopoint origin, final int zoomlevel) {
        // The cut of the fractional part instead of rounding to the nearest integer is intentional and part of the algorithm
        return (int) ((origin.getLongitude() + 180.0) / 360.0 * NUMBER_OF_TILES[zoomlevel]);
    }

    /**
     * Calculate the tile for a Geopoint based on the Spherical Mercator.
     */
    private static int calcY(final Geopoint origin, final int zoomlevel) {
        // Optimization from Bing
        final double sinLatRad = Math.sin(Math.toRadians(origin.getLatitude()));
        // The cut of the fractional part instead of rounding to the nearest integer is intentional and part of the algorithm
        return (int) ((0.5 - Math.log((1 + sinLatRad) / (1 - sinLatRad)) / (4 * Math.PI)) * NUMBER_OF_TILES[zoomlevel]);
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
     * href="http://developers.cloudmade.com/projects/tiles/examples/convert-coordinates-to-tile-numbers">Cloudmade</a>
     */
    @NonNull
    Geopoint getCoord(final UTFGridPosition pos) {

        final double pixX = tileX * TILE_SIZE + pos.x * 4;
        final double pixY = tileY * TILE_SIZE + pos.y * 4;

        final double lonDeg = ((360.0 * pixX) / NUMBER_OF_PIXELS[this.zoomLevel]) - 180.0;
        final double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * pixY / NUMBER_OF_PIXELS[this.zoomLevel])));
        return new Geopoint(Math.toDegrees(latRad), lonDeg);
    }

    @Override
    @NonNull
    public String toString() {
        return String.format(Locale.US, "(%d/%d), zoom=%d", tileX, tileY, zoomLevel);
    }

    /**
     * Calculates the maximum possible zoom level where the supplied points
     * are covered by at least by the supplied number of
     * adjacent tiles on the east/west axis.
     * This criterion can be exactly met for even numbers of tiles
     * while it may result in one more tile as requested for odd numbers
     * of tiles.
     *
     * The order of the points (left/right) is irrelevant.
     *
     * @param left  First point
     * @param right Second point
     */
    static int calcZoomLon(final Geopoint left, final Geopoint right, final int numberOfTiles) {

        int zoom = (int) Math.floor(
                Math.log(360.0 * numberOfTiles / (2.0 * Math.abs(left.getLongitude() - right.getLongitude())))
                        / Math.log(2)
        );

        final Tile tileLeft = new Tile(left, zoom);
        final Tile tileRight = new Tile(right, zoom);

        if (Math.abs(tileLeft.tileX - tileRight.tileX) < (numberOfTiles - 1)) {
            zoom += 1;
        }

        return Math.min(zoom, ZOOMLEVEL_MAX);
    }

    /**
     * Calculates the maximum possible zoom level where the supplied points
     * are covered by at least by the supplied number of
     * adjacent tiles on the north/south axis.
     * This criterion can be exactly met for even numbers of tiles
     * while it may result in one more tile as requested for odd numbers
     * of tiles.
     *
     * The order of the points (bottom/top) is irrelevant.
     *
     * @param bottom First point
     * @param top    Second point
     */
    static int calcZoomLat(final Geopoint bottom, final Geopoint top, final int numberOfTiles) {

        int zoom = (int) Math.ceil(
                Math.log(2.0 * Math.PI * numberOfTiles / (
                        Math.abs(
                                asinh(tanGrad(bottom.getLatitude()))
                                        - asinh(tanGrad(top.getLatitude()))
                        ) * 2.0)
                ) / Math.log(2)
        );

        final Tile tileBottom = new Tile(bottom, zoom);
        final Tile tileTop = new Tile(top, zoom);

        if (Math.abs(tileBottom.tileY - tileTop.tileY) > (numberOfTiles - 1)) {
            zoom -= 1;
        }

        return Math.min(zoom, ZOOMLEVEL_MAX);
    }

    private static double tanGrad(final double angleGrad) {
        return Math.tan(angleGrad / 180.0 * Math.PI);
    }

    /**
     * Calculates the inverted hyperbolic sine
     * (after Bronstein, Semendjajew: Taschenbuch der Mathematik)
     */
    private static double asinh(final double x) {
        return Math.log(x + Math.sqrt(x * x + 1.0));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tile)) {
            return false;
        }
        final Tile other = (Tile) o;
        return this.tileX == other.tileX
                && this.tileY == other.tileY
                && this.zoomLevel == other.zoomLevel;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Request JSON informations for a tile. Return as soon as the request has been made, before the answer has been
     * read.
     *
     * @return A single with one element, or an IOException
     */

    static Single<String> requestMapInfo(final String url, final Parameters params, final String referer) {
        try {
            final Response response = Network.getRequest(url, params, new Parameters("Referer", referer)).blockingGet();
            return Single.just(response).flatMap(Network.getResponseData);
        } catch (final Exception e) {
            return Single.error(e);
        }
    }

    public boolean containsPoint(@NonNull final ICoordinates point) {
        return viewPort.contains(point);
    }

    public Viewport getViewport() {
        return viewPort;
    }

    /**
     * Calculate needed tiles for the given viewport to cover it with
     * max 2x2 tiles
     */
    protected static Set<Tile> getTilesForViewport(final Viewport viewport) {
        return getTilesForViewport(viewport, 2, ZOOMLEVEL_MIN);
    }

    /**
     * Calculate needed tiles for the given viewport.
     * You can define the minimum number of tiles on the longer axis
     * and/or the minimum zoom level.
     */
    protected static Set<Tile> getTilesForViewport(final Viewport viewport, final int tilesOnAxis, final int minZoom) {
        final Set<Tile> tiles = new HashSet<>();
        final int zoom = Math.max(
                Math.min(calcZoomLon(viewport.bottomLeft, viewport.topRight, tilesOnAxis),
                        calcZoomLat(viewport.bottomLeft, viewport.topRight, tilesOnAxis)),
                minZoom);

        final Tile tileBottomLeft = new Tile(viewport.bottomLeft, zoom);
        final Tile tileTopRight = new Tile(viewport.topRight, zoom);

        final int xLow = Math.min(tileBottomLeft.getX(), tileTopRight.getX());
        final int xHigh = Math.max(tileBottomLeft.getX(), tileTopRight.getX());

        final int yLow = Math.min(tileBottomLeft.getY(), tileTopRight.getY());
        final int yHigh = Math.max(tileBottomLeft.getY(), tileTopRight.getY());

        for (int xNum = xLow; xNum <= xHigh; xNum++) {
            for (int yNum = yLow; yNum <= yHigh; yNum++) {
                tiles.add(new Tile(xNum, yNum, zoom));
            }
        }

        return tiles;
    }

    public static class TileCache extends LeastRecentlyUsedSet<Tile> {

        public TileCache() {
            super(64);
        }

        public void removeFromTileCache(@NonNull final ICoordinates point) {
            for (final Tile tile : new ArrayList<>(this)) {
                if (tile.containsPoint(point)) {
                    remove(tile);
                }
            }
        }
    }
}
