package cgeo.geocaching;

import android.test.AndroidTestCase;

import java.util.HashSet;

public class SearchResultTest extends AndroidTestCase {
    public static void testCreateFromGeocodes() {
        final HashSet<String> geocodes = new HashSet<String>();
        geocodes.add("GC12345");
        geocodes.add("GC23456");
        final SearchResult searchResult = new SearchResult(geocodes);
        assertEquals(2, searchResult.getCount());
        assertEquals(2, searchResult.getTotal());
        assertTrue(searchResult.getGeocodes().contains("GC12345"));
    }
}
