package cgeo.geocaching.connector;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.gc.GCBase;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.test.mock.GC2JVEH;

import android.test.AndroidTestCase;

public class GCConnectorTest extends AndroidTestCase {

    @SuppressWarnings("null")
    public static void testGetViewport() {

        GC2JVEH cache = new GC2JVEH();
        final Viewport viewport = new Viewport(cache.getCoords(), 1.0, 1.0);
        SearchResult searchResult = GCBase.searchByViewport(viewport);
        assertTrue(searchResult != null);
        assertEquals(3, searchResult.getCount());
        assertTrue(searchResult.getGeocodes().contains("GC211WG"));
    }

    public static void testBaseCodings() {
        assertEquals(2045702, GCBase.newidToGCId("CpLB"));
        assertEquals("CpLB", GCBase.gcidToNewId(2045702));
        assertEquals(2045702, GCBase.gccodeToGCId("GC2MEGA"));
        assertEquals("GC2MEGA", GCBase.gcidToGCCode(2045702));

        assertEquals("GC211WG", GCBase.newidToGeocode("gEaR"));
    }

}
