package cgeo.geocaching.connector.gc;

import junit.framework.TestCase;

public class GCBaseTest extends TestCase {

    /* TODO: temporary disabled, see PR #13119
    public static void testSearchFromMap() {
        final MockedCache mockedCache = new GC40();

        final Set<String> geocodes = new HashSet<>();
        geocodes.add(mockedCache.getGeocode());

        final SearchResult result = GCMap.searchByGeocodes(GCConnector.getInstance(), geocodes);
        final Geocache parsedCache = result.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_ONLY);

        Compare.assertCompareCaches(mockedCache, parsedCache, false);
    }
    */

}
