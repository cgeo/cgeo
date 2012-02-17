package cgeo.geocaching.connector;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.test.mock.GC2JVEH;

import android.test.AndroidTestCase;

public class GCConnectorTest extends AndroidTestCase {

    @SuppressWarnings("null")
    public static void testGetViewport() {
        GC2JVEH cache = new GC2JVEH();
        final Viewport viewport = new Viewport(cache.getCoords(), 1.0, 1.0);
        SearchResult searchResult = GCConnectorImpl.searchByViewport(viewport);
        assertTrue(searchResult != null);
        assertTrue(searchResult.getGeocodes().contains(cache.getGeocode()));
    }

}
