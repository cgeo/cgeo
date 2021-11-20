package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.test.mock.GC3FJ5F;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.test.Compare;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class GCBaseTest extends TestCase {

    public static void testSearchFromMap() {
        final MockedCache mockedCache = new GC3FJ5F();

        final Set<String> geocodes = new HashSet<>();
        geocodes.add(mockedCache.getGeocode());

        final SearchResult result = GCMap.searchByGeocodes(GCConnector.getInstance(), geocodes);
        final Geocache parsedCache = result.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_ONLY);

        Compare.assertCompareCaches(mockedCache, parsedCache, false);
    }

}
