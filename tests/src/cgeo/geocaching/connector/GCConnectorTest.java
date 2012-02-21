package cgeo.geocaching.connector;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.connector.gc.GCBase;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;

import android.test.AndroidTestCase;

public class GCConnectorTest extends AndroidTestCase {

    @SuppressWarnings("null")
    public static void testGetViewport() {
        cgBase.login();

        String sessionToken = GCBase.getSessionToken();

        {
            final Viewport viewport = new Viewport(new Geopoint("N 52° 25.369 E 9° 35.499"), new Geopoint("N 52° 25.371 E 9° 35.501"));
            SearchResult searchResult = GCBase.searchByViewport(viewport, 14, false, sessionToken);
            assertTrue(searchResult != null);
            assertEquals(7, searchResult.getCount());
            assertTrue(searchResult.getGeocodes().contains("GC211WG"));

            /*
             * Baumarktserie GC21F1W N 52° 25.370 E 009° 35.500
             * Rathaus GC1J1CT N 52° 25.590 E 009° 35.636
             * Spiel & Sport GC211WG N 52° 25.413 E 009° 36.049
             */
        }

        {
            final Viewport viewport = new Viewport(new Geopoint("N 52° 24.000 E 9° 34.500"), new Geopoint("N 52° 26.000 E 9° 38.500"));
            SearchResult searchResult = GCBase.searchByViewport(viewport, 14, false, sessionToken);
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

        // TODO Valentine zoomlevel != 14, Seatle, Rio, Sydney
    }

}

