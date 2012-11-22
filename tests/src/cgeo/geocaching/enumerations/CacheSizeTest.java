package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

import java.util.Locale;

public class CacheSizeTest extends AndroidTestCase {

    public static void testOrder() {
        assertTrue(CacheSize.MICRO.comparable < CacheSize.SMALL.comparable);
        assertTrue(CacheSize.SMALL.comparable < CacheSize.REGULAR.comparable);
        assertTrue(CacheSize.REGULAR.comparable < CacheSize.LARGE.comparable);
    }

    public static void testGetById() {
        assertEquals(CacheSize.UNKNOWN, CacheSize.getById(""));
        assertEquals(CacheSize.UNKNOWN, CacheSize.getById(null));
        assertEquals(CacheSize.UNKNOWN, CacheSize.getById("random garbage"));
        assertEquals(CacheSize.LARGE, CacheSize.getById("large"));
        assertEquals(CacheSize.LARGE, CacheSize.getById("LARGE"));
    }

    public static void testGetByIdComplete() {
        for (CacheSize size : CacheSize.values()) {
            assertEquals(size, CacheSize.getById(size.id));
            assertEquals(size, CacheSize.getById(size.id.toLowerCase(Locale.US)));
            assertEquals(size, CacheSize.getById(size.id.toUpperCase(Locale.US)));
        }
    }
}
