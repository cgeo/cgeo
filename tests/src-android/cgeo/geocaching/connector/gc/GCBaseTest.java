package cgeo.geocaching.connector.gc;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.test.mock.GC2CJPFAndroid;
import cgeo.geocaching.test.mock.MockedCacheAndroid;
import cgeo.test.Compare;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class GCBaseTest extends TestCase {

    public static void testSearchFromMap() {
        final MockedCacheAndroid mockedCache = new GC2CJPFAndroid();

        final Set<String> geocodes = new HashSet<String>();
        geocodes.add(mockedCache.getGeocode());

        final SearchResult result = GCMap.searchByGeocodes(geocodes);
        final Geocache parsedCache = result.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_ONLY);

        Compare.assertCompareCaches(mockedCache, parsedCache, false);

    }
}
