package cgeo.geocaching;

import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

public class ParserTest extends AbstractResourceInstrumentationTestCase {

    public void testOwnerDecoding() {
        cgCacheWrap caches = cgBase.parseCacheFromText(getFileContent(R.raw.gc1zxez), 0, null);
        assertEquals(1, caches.cacheList.size());
        final cgCache cache = caches.cacheList.get(0);
        assertEquals("Ms.Marple/Mr.Stringer", cache.getOwnerReal());
    }
}