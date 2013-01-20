package cgeo.geocaching.connector.gc;

import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;

import java.util.Set;

import junit.framework.TestCase;

public class AutoZoomTest extends TestCase {

    public static void testZoom1() {
        Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        Geopoint topRight = new Geopoint(49.4, 8.4);

        int zoom = Tile.calcZoomLat(bottomLeft, topRight, 2);

        assertTrue(Math.abs(new Tile(bottomLeft, zoom).getY() - new Tile(topRight, zoom).getY()) == 1);
        assertTrue(Math.abs(new Tile(bottomLeft, zoom + 1).getY() - new Tile(topRight, zoom + 1).getY()) > 1);

        zoom = Tile.calcZoomLon(bottomLeft, topRight, 2);

        assertTrue(new Tile(bottomLeft, zoom).getX() + 1 == new Tile(topRight, zoom).getX());
        assertTrue(new Tile(bottomLeft, zoom + 1).getX() + 1 < new Tile(topRight, zoom + 1).getX());

    }

    public static void testZoom2() {
        Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        Geopoint topRight = new Geopoint(49.4, 8.4);

        int zoom = Tile.calcZoomLat(bottomLeft, topRight, 3);

        assertTrue(Math.abs(new Tile(bottomLeft, zoom).getY() - new Tile(topRight, zoom).getY()) >= 2);
        assertTrue(Math.abs(new Tile(bottomLeft, zoom + 1).getY() - new Tile(topRight, zoom + 1).getY()) > 2);

        zoom = Tile.calcZoomLon(bottomLeft, topRight, 3);

        assertTrue(Math.abs(new Tile(bottomLeft, zoom).getX() - new Tile(topRight, zoom).getX()) >= 2);
        assertTrue(Math.abs(new Tile(bottomLeft, zoom + 1).getX() - new Tile(topRight, zoom + 1).getX()) > 2);

    }

    public static void testTiles1x2() {
        Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        Geopoint topRight = new Geopoint(49.4, 8.4);

        Set<Tile> tiles = Tile.getTilesForViewport(new Viewport(bottomLeft, topRight));

        assertEquals(2, tiles.size());
    }

    public static void testTiles2x3() {
        Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        Geopoint topRight = new Geopoint(49.4, 8.4);

        Set<Tile> tiles = Tile.getTilesForViewport(new Viewport(bottomLeft, topRight), 3, Tile.ZOOMLEVEL_MIN);

        assertEquals(6, tiles.size());
    }

    public static void testTilesZoom13() {
        Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        Geopoint topRight = new Geopoint(49.4, 8.4);

        Set<Tile> tiles = Tile.getTilesForViewport(new Viewport(bottomLeft, topRight), 3, 13);

        assertEquals(16, tiles.size());
    }
}
