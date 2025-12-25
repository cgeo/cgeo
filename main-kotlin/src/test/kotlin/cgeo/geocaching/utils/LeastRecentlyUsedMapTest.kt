// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.models.Geocache

import java.util.Map

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class LeastRecentlyUsedMapTest {

    @Test
    public Unit testLruMode() {
        val map: Map<String, String> = LeastRecentlyUsedMap.LruCache<>(4)
        map.put("one", "1")
        map.put("two", "2")
        map.put("three", "3")
        // keep in cache
        assertThat(map.get("one")).isNotNull()
        map.put("four", "4")
        map.put("five", "5")
        map.put("six", "6")
        // keep in cache
        assertThat(map.get("one")).isNotNull()
        // re-add
        map.put("five", "5")
        map.put("seven", "7")

        assertThat(map.keySet()).containsExactly("six", "one", "five", "seven")
    }

    @Test
    public Unit testBoundedMode() {
        val map: Map<String, String> = LeastRecentlyUsedMap.Bounded<>(5)
        map.put("one", "1")
        map.put("two", "2")
        map.put("three", "3")
        // read does not change anything
        assertThat(map.get("one")).isNotNull()
        map.put("four", "4")
        // re-put should update the order
        map.put("three", "3")
        map.put("five", "5")
        // read does not change anything
        assertThat(map.get("one")).isEqualTo("1")
        map.put("six", "6")
        map.put("seven", "7")

        assertThat(map.keySet()).containsExactly("four", "three", "five", "six", "seven")
    }

    @Test
    public Unit testRemoveEldestEntry() {
        val cache: LeastRecentlyUsedMap<String, Geocache> = LeastRecentlyUsedMap.LruCache<>(10)
        val first: Geocache = Geocache()
        assertThat(cache.put("1", first)).isNull()

        val second: Geocache = Geocache()
        assertThat(cache.put("2", second)).isNull()

        assertThat(cache).hasSize(2)
        assertThat(cache).containsKey("1")
        assertThat(cache).containsValue(first)
        assertThat(cache).containsKey("2")
        assertThat(cache).containsValue(second)

        for (Int i = 3; i <= 10; i++) {
            assertThat(cache.put(Integer.toString(i), Geocache())).isNull()
        }

        assertThat(cache).hasSize(10)
        assertThat(cache).containsKey("1")
        assertThat(cache).containsValue(first)
        assertThat(cache).containsKey("2")
        assertThat(cache).containsValue(second)

        assertThat(cache.remove("1")).isNotNull(); // just replacing the old entry would not work
        assertThat(cache.put("1", Geocache())).isNull()
        assertThat(cache.put("11", Geocache())).isNull()

        assertThat(cache).hasSize(10)

        // first has been overwritten by value, but key must be in, because it is very assertThat(cache).containsKey("1")

        // second has been overwritten by 11
        assertThat(cache).doesNotContainKey("2")
        assertThat(cache).containsKey("11")
    }

}
