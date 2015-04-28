package cgeo.geocaching.connector.gc;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.trackable.TravelBugConnector;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;

public class GCConnectorTest extends AbstractResourceInstrumentationTestCase {

    public static void testGetViewport() {
        // backup user settings
        final boolean excludeMine = Settings.isExcludeMyCaches();
        final CacheType cacheType = Settings.getCacheType();
        try {
            // set up settings required for test
            TestSettings.setExcludeMine(false);
            Settings.setCacheType(CacheType.ALL);
            GCLogin.getInstance().login();

            final MapTokens tokens = GCLogin.getInstance().getMapTokens();

            {
                final Viewport viewport = new Viewport(new Geopoint("N 52° 25.369 E 9° 35.499"), new Geopoint("N 52° 25.600 E 9° 36.200"));
                final SearchResult searchResult = ConnectorFactory.searchByViewport(viewport, tokens);
                assertThat(searchResult).isNotNull();
                assertThat(searchResult.isEmpty()).isFalse();
                assertThat(searchResult.getGeocodes()).contains("GC4ER5H");
                // 22.10.13: Changed from GC211WG (archived) to GC4ER5H  in same area
            }

            {
                final Viewport viewport = new Viewport(new Geopoint("N 52° 24.000 E 9° 34.500"), new Geopoint("N 52° 26.000 E 9° 38.500"));
                final SearchResult searchResult = ConnectorFactory.searchByViewport(viewport, tokens);
                assertThat(searchResult).isNotNull();
                assertThat(searchResult.getGeocodes()).contains("GC4ER5H");
            }
        } finally {
            // restore user settings
            TestSettings.setExcludeMine(excludeMine);
            Settings.setCacheType(cacheType);
        }
    }

    public static void testCanHandle() {
        assertThat(GCConnector.getInstance().canHandle("GC2MEGA")).isTrue();
        assertThat(GCConnector.getInstance().canHandle("OXZZZZZ")).isFalse();
        assertThat(GCConnector.getInstance().canHandle("gc77")).isTrue();
    }

    /**
     * functionality moved to {@link TravelBugConnector}
     */
    public static void testCanNotHandleTrackablesAnymore() {
        assertThat(GCConnector.getInstance().canHandle("TB3F651")).isFalse();
    }

    public static void testBaseCodings() {
        assertThat(GCConstants.gccodeToGCId("GC2MEGA")).isEqualTo(2045702);
    }

    /** Tile computation with different zoom levels */
    public static void testTile() {
        // http://coord.info/GC2CT8K = N 52° 30.462 E 013° 27.906
        assertTileAt(8804, 5374, new Tile(new Geopoint(52.5077, 13.4651), 14));

        // (8633, 5381); N 52° 24,516 E 009° 42,592
        assertTileAt(8633, 5381, new Tile(new Geopoint("N 52° 24,516 E 009° 42,592"), 14));

        // Hannover, GC22VTB UKM Memorial Tour
        assertTileAt(2159, 1346, new Tile(new Geopoint("N 52° 22.177 E 009° 45.385"), 12));

        // Seattle, GCK25B Groundspeak Headquarters
        assertTileAt(5248, 11440, new Tile(new Geopoint("N 47° 38.000 W 122° 20.000"), 15));

        // Sydney, GCXT2R Victoria Cross
        assertTileAt(7536, 4915, new Tile(new Geopoint("S 33° 50.326 E 151° 12.426"), 13));
    }

    private static void assertTileAt(final int x, final int y, final Tile tile) {
        assertThat(tile.getX()).isEqualTo(x);
        assertThat(tile.getY()).isEqualTo(y);
    }

    public static void testGetGeocodeFromUrl() {
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("some string")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/GC12ABC")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/GC12ABC")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.geocaching.com/geocache/GC12ABC_die-muhlen-im-schondratal-muhle-munchau")).isEqualTo("GC12ABC");
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://geocaching.com/geocache/GC12ABC_die-muhlen-im-schondratal-muhle-munchau")).isEqualTo("GC12ABC");

        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/TB1234")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/TB1234")).isNull();
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/WM1234")).isNull();

        // uppercase is managed in ConnectorFactory
        assertThat(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/gc77")).isEqualTo("gc77");
    }
}
