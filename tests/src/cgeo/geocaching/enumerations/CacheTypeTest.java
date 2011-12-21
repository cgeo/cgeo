package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class CacheTypeTest extends AndroidTestCase {

    public static void testGetById() {
        assertEquals(CacheType.UNKNOWN, CacheType.getById(""));
        assertEquals(CacheType.UNKNOWN, CacheType.getById(null));
        assertEquals(CacheType.UNKNOWN, CacheType.getById("random garbage"));
    }

}
