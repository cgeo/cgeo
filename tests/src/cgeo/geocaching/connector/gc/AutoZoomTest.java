package cgeo.geocaching.connector.gc;

import cgeo.geocaching.geopoint.Geopoint;

import junit.framework.TestCase;

public class AutoZoomTest extends TestCase {

    public static void testZoom1() {
        Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        Geopoint topRight = new Geopoint(49.4, 8.4);

        int zoom = Tile.calcZoomLat(bottomLeft, topRight);

        assertTrue(Math.abs(new Tile(bottomLeft, zoom).getY() - new Tile(topRight, zoom).getY()) == 1);
        assertTrue(Math.abs(new Tile(bottomLeft, zoom + 1).getY() - new Tile(topRight, zoom + 1).getY()) > 1);

        zoom = Tile.calcZoomLon(bottomLeft, topRight);

        assertTrue(new Tile(bottomLeft, zoom).getX() + 1 == new Tile(topRight, zoom).getX());
        assertTrue(new Tile(bottomLeft, zoom + 1).getX() + 1 < new Tile(topRight, zoom + 1).getX());

    }

}
