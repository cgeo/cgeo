package cgeo.geocaching.utils;

import cgeo.geocaching.cgCache;

import java.util.Map;

public class LeastRecentlyUsedMapTest extends AbstractLRUTest {

    public static void testLruMode() {
        final Map<String, String> map = new LeastRecentlyUsedMap.LruCache<String, String>(4);
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        // keep in cache
        map.get("one");
        map.put("four", "4");
        map.put("five", "5");
        map.put("six", "6");
        // keep in cache
        map.get("one");
        // re-add
        map.put("five", "5");
        map.put("seven", "7");

        assertEquals("six, one, five, seven", colToStr(map.keySet()));
    }

    public static void testBoundedMode() {
        final Map<String, String> map = new LeastRecentlyUsedMap.Bounded<String, String>(5);
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        // read does not change anything
        map.get("one");
        map.put("four", "4");
        // re-put should update the order
        map.put("three", "3");
        map.put("five", "5");
        // read does not change anything
        map.get("one");
        map.put("six", "6");
        map.put("seven", "7");

        assertEquals("four, three, five, six, seven", colToStr(map.keySet()));
    }

    public static void testRemoveEldestEntry() {
        final LeastRecentlyUsedMap<String, cgCache> cache = new LeastRecentlyUsedMap.LruCache<String, cgCache>(10);
        final cgCache first = new cgCache();
        assertNull(cache.put("1", first));

        final cgCache second = new cgCache();
        assertNull(cache.put("2", second));

        assertEquals(2, cache.size());
        assertTrue(cache.containsKey("1"));
        assertTrue(cache.containsValue(first));
        assertTrue(cache.containsKey("2"));
        assertTrue(cache.containsValue(second));

        for (int i = 3; i <= 10; i++) {
            assertNull(cache.put(Integer.toString(i), new cgCache()));
        }

        assertEquals(10, cache.size());
        assertTrue(cache.containsKey("1"));
        assertTrue(cache.containsValue(first));
        assertTrue(cache.containsKey("2"));
        assertTrue(cache.containsValue(second));

        assertNotNull(cache.remove("1")); // just replacing the old entry would not work
        assertNull(cache.put("1", new cgCache()));
        assertNull(cache.put("11", new cgCache()));

        assertEquals(10, cache.size());

        // first has been overwritten by new value, but key must be in, because it is very new
        assertTrue(cache.containsKey("1"));

        // second has been overwritten by 11
        assertFalse(cache.containsKey("2"));
        assertTrue(cache.containsKey("11"));
    }

}
