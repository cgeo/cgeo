package cgeo.geocaching.utils;

import cgeo.geocaching.models.Geocache;

import java.util.Map;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LeastRecentlyUsedMapTest {

    @Test
    public void testLruMode() {
        final Map<String, String> map = new LeastRecentlyUsedMap.LruCache<>(4);
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        // keep in cache
        assertThat(map.get("one")).isNotNull();
        map.put("four", "4");
        map.put("five", "5");
        map.put("six", "6");
        // keep in cache
        assertThat(map.get("one")).isNotNull();
        // re-add
        map.put("five", "5");
        map.put("seven", "7");

        assertThat(map.keySet()).containsExactly("six", "one", "five", "seven");
    }

    @Test
    public void testBoundedMode() {
        final Map<String, String> map = new LeastRecentlyUsedMap.Bounded<>(5);
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        // read does not change anything
        assertThat(map.get("one")).isNotNull();
        map.put("four", "4");
        // re-put should update the order
        map.put("three", "3");
        map.put("five", "5");
        // read does not change anything
        assertThat(map.get("one")).isEqualTo("1");
        map.put("six", "6");
        map.put("seven", "7");

        assertThat(map.keySet()).containsExactly("four", "three", "five", "six", "seven");
    }

    @Test
    public void testRemoveEldestEntry() {
        final LeastRecentlyUsedMap<String, Geocache> cache = new LeastRecentlyUsedMap.LruCache<>(10);
        final Geocache first = new Geocache();
        assertThat(cache.put("1", first)).isNull();

        final Geocache second = new Geocache();
        assertThat(cache.put("2", second)).isNull();

        assertThat(cache).hasSize(2);
        assertThat(cache).containsKey("1");
        assertThat(cache).containsValue(first);
        assertThat(cache).containsKey("2");
        assertThat(cache).containsValue(second);

        for (int i = 3; i <= 10; i++) {
            assertThat(cache.put(Integer.toString(i), new Geocache())).isNull();
        }

        assertThat(cache).hasSize(10);
        assertThat(cache).containsKey("1");
        assertThat(cache).containsValue(first);
        assertThat(cache).containsKey("2");
        assertThat(cache).containsValue(second);

        assertThat(cache.remove("1")).isNotNull(); // just replacing the old entry would not work
        assertThat(cache.put("1", new Geocache())).isNull();
        assertThat(cache.put("11", new Geocache())).isNull();

        assertThat(cache).hasSize(10);

        // first has been overwritten by new value, but key must be in, because it is very new
        assertThat(cache).containsKey("1");

        // second has been overwritten by 11
        assertThat(cache).doesNotContainKey("2");
        assertThat(cache).containsKey("11");
    }

}
