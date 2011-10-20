package cgeo.geocaching;

import cgeo.geocaching.test.R;

import android.test.InstrumentationTestCase;

import java.io.InputStream;
import java.util.Scanner;

public class ParserTest extends InstrumentationTestCase {

    private String getFileContent(int resourceId) {
        InputStream ins = getInstrumentation().getContext().getResources().openRawResource(resourceId);
        return new Scanner(ins).useDelimiter("\\A").next();
    }

    public void testOwnerDecoding() {
        cgCacheWrap caches = cgBase.parseCache(getFileContent(R.raw.gc1zxez), 0, null);
        assertEquals(1, caches.cacheList.size());
        final cgCache cache = caches.cacheList.get(0);
        assertEquals("Ms.Marple/Mr.Stringer", cache.ownerReal);
    }
}