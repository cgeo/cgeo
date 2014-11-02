package cgeo.geocaching.connector.gc;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;

import java.util.Set;

import junit.framework.TestCase;

public class AutoZoomTest extends TestCase {

    public static void testZoom1() {
        final Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        final Geopoint topRight = new Geopoint(49.4, 8.4);

        int zoom = Tile.calcZoomLat(bottomLeft, topRight, 2);

        assertThat(Math.abs(new Tile(bottomLeft, zoom).getY() - new Tile(topRight, zoom).getY()) == 1).isTrue();
        assertThat(Math.abs(new Tile(bottomLeft, zoom + 1).getY() - new Tile(topRight, zoom + 1).getY()) > 1).isTrue();

        zoom = Tile.calcZoomLon(bottomLeft, topRight, 2);

        assertThat(new Tile(bottomLeft, zoom).getX() + 1 == new Tile(topRight, zoom).getX()).isTrue();
        assertThat(new Tile(bottomLeft, zoom + 1).getX() + 1 < new Tile(topRight, zoom + 1).getX()).isTrue();

    }

    public static void testZoom2() {
        final Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        final Geopoint topRight = new Geopoint(49.4, 8.4);

        int zoom = Tile.calcZoomLat(bottomLeft, topRight, 3);

        assertThat(Math.abs(new Tile(bottomLeft, zoom).getY() - new Tile(topRight, zoom).getY()) >= 2).isTrue();
        assertThat(Math.abs(new Tile(bottomLeft, zoom + 1).getY() - new Tile(topRight, zoom + 1).getY()) > 2).isTrue();

        zoom = Tile.calcZoomLon(bottomLeft, topRight, 3);

        assertThat(Math.abs(new Tile(bottomLeft, zoom).getX() - new Tile(topRight, zoom).getX()) >= 2).isTrue();
        assertThat(Math.abs(new Tile(bottomLeft, zoom + 1).getX() - new Tile(topRight, zoom + 1).getX()) > 2).isTrue();

    }

    public static void testTiles1x2() {
        final Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        final Geopoint topRight = new Geopoint(49.4, 8.4);

        final Set<Tile> tiles = Tile.getTilesForViewport(new Viewport(bottomLeft, topRight));

        assertThat(tiles).hasSize(2);
    }

    public static void testTiles2x3() {
        final Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        final Geopoint topRight = new Geopoint(49.4, 8.4);

        final Set<Tile> tiles = Tile.getTilesForViewport(new Viewport(bottomLeft, topRight), 3, Tile.ZOOMLEVEL_MIN);

        assertThat(tiles).hasSize(6);
    }

    public static void testTilesZoom13() {
        final Geopoint bottomLeft = new Geopoint(49.3, 8.3);
        final Geopoint topRight = new Geopoint(49.4, 8.4);

        final Set<Tile> tiles = Tile.getTilesForViewport(new Viewport(bottomLeft, topRight), 3, 13);

        assertThat(tiles).hasSize(16);
    }
}
