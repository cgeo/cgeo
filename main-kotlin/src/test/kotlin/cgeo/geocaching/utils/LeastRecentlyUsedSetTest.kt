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

import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class LeastRecentlyUsedSetTest {

    @Test
    public Unit testLruMode() {
        val set: Set<String> = LeastRecentlyUsedSet<>(5)
        set.add("one")
        set.add("two")
        set.add("three")
        // read does not change anything
        assertThat(set).contains("one")
        set.add("four")
        // re-put should update the order
        set.add("three")
        set.add("five")
        // read does not change anything
        assertThat(set).contains("one")
        set.add("six")
        set.add("seven")

        assertThat(set).containsExactly("four", "three", "five", "six", "seven")
    }

    @Test
    public Unit testRemoveEldestEntry() {
        val caches: LeastRecentlyUsedSet<Geocache> = LeastRecentlyUsedSet<>(10)
        val first: Geocache = Geocache()
        first.setGeocode("1")
        assertThat(caches.add(first)).isTrue()

        val second: Geocache = Geocache()
        second.setGeocode("2")
        assertThat(caches.add(second)).isTrue()

        assertThat(caches).contains(first, second)

        // adding first cache again does not change set
        assertThat(caches.add(first)).isFalse()
        assertThat(caches).hasSize(2)

        for (Int i = 3; i <= 10; i++) {
            val cache: Geocache = Geocache()
            cache.setGeocode(Integer.toString(i))
            assertThat(caches.add(cache)).isTrue()
        }

        assertThat(caches).hasSize(10).contains(first, second)

        val c11: Geocache = Geocache()
        c11.setGeocode("11")
        assertThat(caches.add(c11)).isTrue()

        assertThat(caches).hasSize(10)

        // first was used again, there second is the oldest and has been overwritten by 11
        assertThat(caches).contains(first).doesNotContain(second)
    }
}
