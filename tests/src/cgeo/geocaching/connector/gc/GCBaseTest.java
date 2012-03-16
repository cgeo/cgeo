package cgeo.geocaching.connector.gc;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgBaseTest;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.test.mock.GC2CJPF;
import cgeo.geocaching.test.mock.MockedCache;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class GCBaseTest extends TestCase {

    public static void testSplitJSONKey() {
        assertKey("(1, 2)", 1, 2);
        assertKey("(12, 34)", 12, 34);
        assertKey("(1234,56)", 1234, 56);
        assertKey("(1234,  567)", 1234, 567);
    }

    private static void assertKey(String key, int x, int y) {
        UTFGridPosition pos = UTFGridPosition.fromString(key);
        assertEquals(x, pos.getX());
        assertEquals(y, pos.getY());
    }

    public static void testSearchByGeocodes() {

        MockedCache mockedCache = new GC2CJPF();

        Set<String> geocodes = new HashSet<String>();
        geocodes.add(mockedCache.getGeocode());

        SearchResult result = GCBase.searchByGeocodes(geocodes);
        cgCache parsedCache = result.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_ONLY);

        cgBaseTest.testCompareCaches(mockedCache, parsedCache, false);

    }
}
