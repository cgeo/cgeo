package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;

public class GCConnectorTest extends AbstractResourceInstrumentationTestCase {

    public static void testGetViewport() {
        // backup user settings
        final boolean excludeMine = Settings.isExcludeMyCaches();
        final CacheType cacheType = Settings.getCacheType();
        try {
            // set up settings required for test
            Settings.setExcludeMine(false);
            Settings.setCacheType(CacheType.ALL);
            Login.login();

            String[] tokens = Login.getMapTokens();

            {
                final Viewport viewport = new Viewport(new Geopoint("N 52° 25.369 E 9° 35.499"), new Geopoint("N 52° 25.600 E 9° 36.200"));
                SearchResult searchResult = ConnectorFactory.searchByViewport(viewport, tokens);
                assertNotNull(searchResult);
                assertFalse(searchResult.isEmpty());
                assertTrue(searchResult.getGeocodes().contains("GC211WG"));
                // Spiel & Sport GC211WG N 52° 25.413 E 009° 36.049
            }

            {
                final Viewport viewport = new Viewport(new Geopoint("N 52° 24.000 E 9° 34.500"), new Geopoint("N 52° 26.000 E 9° 38.500"));
                SearchResult searchResult = ConnectorFactory.searchByViewport(viewport, tokens);
                assertNotNull(searchResult);
                assertTrue(searchResult.getGeocodes().contains("GC211WG"));
            }
        } finally {
            // restore user settings
            Settings.setExcludeMine(excludeMine);
            Settings.setCacheType(cacheType);
        }
    }

    public static void testCanHandle() {
        assertTrue(GCConnector.getInstance().canHandle("GC2MEGA"));
        assertTrue(GCConnector.getInstance().canHandle("TB3F651"));
        assertFalse(GCConnector.getInstance().canHandle("OXZZZZZ"));
    }

    public static void testBaseCodings() {
        assertEquals(2045702, GCConstants.gccodeToGCId("GC2MEGA"));
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

    private static void assertTileAt(int x, int y, final Tile tile) {
        assertEquals(x, tile.getX());
        assertEquals(y, tile.getY());
    }

    public static void testGetGeocodeFromUrl() {
        assertNull(GCConnector.getInstance().getGeocodeFromUrl("some string"));
        assertEquals("GC12ABC", GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/GC12ABC"));
    }
}

