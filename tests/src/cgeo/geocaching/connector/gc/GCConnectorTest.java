package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class GCConnectorTest extends AbstractResourceInstrumentationTestCase {

    public static void testGetViewport() {
        cgBase.login();

        String[] tokens = GCBase.getTokens();

        {
            final Viewport viewport = new Viewport(new Geopoint("N 52° 25.369 E 9° 35.499"), new Geopoint("N 52° 25.371 E 9° 35.501"));
            SearchResult searchResult = ConnectorFactory.searchByViewport(viewport, tokens);
            assertNotNull(searchResult);
            assertTrue(searchResult.getCount() > 130);
            assertTrue(searchResult.getGeocodes().contains("GC211WG"));
            // Spiel & Sport GC211WG N 52° 25.413 E 009° 36.049
        }

        {
            final Viewport viewport = new Viewport(new Geopoint("N 52° 24.000 E 9° 34.500"), new Geopoint("N 52° 26.000 E 9° 38.500"));
            SearchResult searchResult = ConnectorFactory.searchByViewport(viewport, tokens);
            assertNotNull(searchResult);
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
    }

    public void testparseMapPNG() {
        // createApplication();
        // cgBase.initialize(getApplication());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeStream(getInstrumentation().getContext().getResources().openRawResource(R.raw.tile14));
        assert bitmap.getWidth() == Tile.TILE_SIZE : "Wrong size";

        Log.d(Settings.tag, "Bitmap=" + bitmap.getWidth() + "x" + bitmap.getHeight());

        cgCache cache = new cgCache();

        // Tradi
        GCBase.parseMapPNG14(cache, bitmap, new UTFGridPosition(97 / 4, 136 / 4));
        assertEquals(CacheType.TRADITIONAL, cache.getType());
        // Mystery
        GCBase.parseMapPNG14(cache, bitmap, new UTFGridPosition(226 / 4, 104 / 4));
        assertEquals(CacheType.MYSTERY, cache.getType());
        // Multi
        GCBase.parseMapPNG14(cache, bitmap, new UTFGridPosition(54 / 4, 97 / 4));
        assertEquals(CacheType.MULTI, cache.getType());
        // Found
        GCBase.parseMapPNG14(cache, bitmap, new UTFGridPosition(119 / 4, 108 / 4));
        assertTrue(cache.isFound());
        cache.setFound(false); // reset

        bitmap = BitmapFactory.decodeStream(getInstrumentation().getContext().getResources().openRawResource(R.raw.tile13));

        // Tradi
        GCBase.parseMapPNG13(cache, bitmap, new UTFGridPosition(146 / 4, 225 / 4));
        assertEquals(CacheType.TRADITIONAL, cache.getType());
        // Mystery
        GCBase.parseMapPNG13(cache, bitmap, new UTFGridPosition(181 / 4, 116 / 4));
        assertEquals(CacheType.MYSTERY, cache.getType());
        // Multi
        GCBase.parseMapPNG13(cache, bitmap, new UTFGridPosition(118 / 4, 230 / 4));
        assertEquals(CacheType.MULTI, cache.getType());
        // Found - not available in parseMapPNG13
    }

}

