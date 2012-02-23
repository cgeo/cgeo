package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;

import android.test.AndroidTestCase;

public class GCConnectorTest extends AndroidTestCase {

    @SuppressWarnings("null")
    public static void testGetViewport() {
        cgBase.login();

        String[] tokens = GCBase.getTokens();

        {
            final Viewport viewport = new Viewport(new Geopoint("N 52° 25.369 E 9° 35.499"), new Geopoint("N 52° 25.371 E 9° 35.501"));
            SearchResult searchResult = GCBase.searchByViewport(viewport, tokens);
            assertTrue(searchResult != null);
            assertEquals(7, searchResult.getCount());
            assertTrue(searchResult.getGeocodes().contains("GC211WG"));
            // Spiel & Sport GC211WG N 52° 25.413 E 009° 36.049
        }

        {
            final Viewport viewport = new Viewport(new Geopoint("N 52° 24.000 E 9° 34.500"), new Geopoint("N 52° 26.000 E 9° 38.500"));
            SearchResult searchResult = GCBase.searchByViewport(viewport, tokens);
            assertTrue(searchResult != null);
            assertTrue(searchResult.getGeocodes().contains("GC211WG"));
        }
    }

    public static void testBaseCodings() {
        assertEquals(2045702, GCBase.newidToGCId("CpLB"));
        assertEquals("CpLB", GCBase.gcidToNewId(2045702));
        assertEquals(2045702, GCBase.gccodeToGCId("GC2MEGA"));
        assertEquals("GC2MEGA", GCBase.gcidToGCCode(2045702));

        assertEquals("GC211WG", GCBase.newidToGeocode("gEaR"));
    }

    /** Tile computation with different zoom levels */
    public static void testTile() {
        {
            // http://coord.info/GC2CT8K = N 52° 30.462 E 013° 27.906
            Tile tile = new Tile(new Geopoint(52.5077, 13.4651), 14);
            assertEquals(8804, tile.getX());
            assertEquals(5374, tile.getY());
        }
        {
            // (8633, 5381); N 52° 24,516 E 009° 42,592
            Tile tile = new Tile(new Geopoint("N 52° 24,516 E 009° 42,592"), 14);
            assertEquals(8633, tile.getX());
            assertEquals(5381, tile.getY());
        }
        {
            // Hannover, GC22VTB UKM Memorial Tour
            Tile tile = new Tile(new Geopoint("N 52° 22.177 E 009° 45.385"), 12);
            assertEquals(2159, tile.getX());
            assertEquals(1346, tile.getY());
        }
        {
            // Seatle, GCK25B Groundspeak Headquarters
            Tile tile = new Tile(new Geopoint("N 47° 38.000 W 122° 20.000"), 15);
            assertEquals(5248, tile.getX());
            assertEquals(11440, tile.getY());
        }
        {
            // Sydney, GCXT2R Victoria Cross
            Tile tile = new Tile(new Geopoint("S 33° 50.326 E 151° 12.426"), 13);
            assertEquals(7536, tile.getX());
            assertEquals(4915, tile.getY());
        }

        // TODO ebenfalls nutzen in searchByViewport und KOs vergleichen
    }


}

