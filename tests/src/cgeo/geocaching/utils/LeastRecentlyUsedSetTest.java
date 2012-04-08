package cgeo.geocaching.utils;

import cgeo.geocaching.cgCache;

import java.util.Set;

public class LeastRecentlyUsedSetTest extends AbstractLRUTest {

    public static void testLruMode() {
        final Set<String> set = new LeastRecentlyUsedSet<String>(5);
        set.add("one");
        set.add("two");
        set.add("three");
        // read does not change anything
        set.contains("one");
        set.add("four");
        // re-put should update the order
        set.add("three");
        set.add("five");
        // read does not change anything
        set.contains("one");
        set.add("six");
        set.add("seven");

        assertEquals("four, three, five, six, seven", colToStr(set));
    }

    public static void testRemoveEldestEntry() {
        final LeastRecentlyUsedSet<cgCache> caches = new LeastRecentlyUsedSet<cgCache>(10);
        final cgCache first = new cgCache();
        first.setGeocode("1");
        assertTrue(caches.add(first));

        final cgCache second = new cgCache();
        second.setGeocode("2");
        assertTrue(caches.add(second));

        assertEquals(2, caches.size());
        assertTrue(caches.contains(first));
        assertTrue(caches.contains(second));

        // adding first cache again does not change set
        assertFalse(caches.add(first));
        assertEquals(2, caches.size());

        for (int i = 3; i <= 10; i++) {
            final cgCache cache = new cgCache();
            cache.setGeocode(Integer.toString(i));
            assertTrue(caches.add(cache));
        }

        assertEquals(10, caches.size());
        assertTrue(caches.contains(first));
        assertTrue(caches.contains(second));

        final cgCache c11 = new cgCache();
        c11.setGeocode("11");
        assertTrue(caches.add(c11));

        assertEquals(10, caches.size());

        // first was used again, there second is the oldest and has been overwritten by 11
        assertTrue(caches.contains(first));
        assertFalse(caches.contains(second));
    }
}