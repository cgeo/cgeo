package cgeo.geocaching.utils;

import cgeo.geocaching.models.Geocache;

import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LeastRecentlyUsedSetTest {

    @Test
    public void testLruMode() {
        final Set<String> set = new LeastRecentlyUsedSet<>(5);
        set.add("one");
        set.add("two");
        set.add("three");
        // read does not change anything
        assertThat(set).contains("one");
        set.add("four");
        // re-put should update the order
        set.add("three");
        set.add("five");
        // read does not change anything
        assertThat(set).contains("one");
        set.add("six");
        set.add("seven");

        assertThat(set).containsExactly("four", "three", "five", "six", "seven");
    }

    @Test
    public void testRemoveEldestEntry() {
        final LeastRecentlyUsedSet<Geocache> caches = new LeastRecentlyUsedSet<>(10);
        final Geocache first = new Geocache();
        first.setGeocode("1");
        assertThat(caches.add(first)).isTrue();

        final Geocache second = new Geocache();
        second.setGeocode("2");
        assertThat(caches.add(second)).isTrue();

        assertThat(caches).contains(first, second);

        // adding first cache again does not change set
        assertThat(caches.add(first)).isFalse();
        assertThat(caches).hasSize(2);

        for (int i = 3; i <= 10; i++) {
            final Geocache cache = new Geocache();
            cache.setGeocode(Integer.toString(i));
            assertThat(caches.add(cache)).isTrue();
        }

        assertThat(caches).hasSize(10).contains(first, second);

        final Geocache c11 = new Geocache();
        c11.setGeocode("11");
        assertThat(caches.add(c11)).isTrue();

        assertThat(caches).hasSize(10);

        // first was used again, there second is the oldest and has been overwritten by 11
        assertThat(caches).contains(first).doesNotContain(second);
    }
}
