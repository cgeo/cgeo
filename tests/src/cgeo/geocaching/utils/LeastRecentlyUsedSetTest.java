package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Geocache;

import java.util.Set;

public class LeastRecentlyUsedSetTest extends AbstractLRUTest {

    public static void testLruMode() {
        final Set<String> set = new LeastRecentlyUsedSet<String>(5);
        set.add("one");
        set.add("two");
        set.add("three");
        // read does not change anything
        assertThat(set.contains("one")).isTrue();
        set.add("four");
        // re-put should update the order
        set.add("three");
        set.add("five");
        // read does not change anything
        assertThat(set.contains("one")).isTrue();
        set.add("six");
        set.add("seven");

        assertEquals("four, three, five, six, seven", colToStr(set));
    }

    public static void testRemoveEldestEntry() {
        final LeastRecentlyUsedSet<Geocache> caches = new LeastRecentlyUsedSet<Geocache>(10);
        final Geocache first = new Geocache();
        first.setGeocode("1");
        assertThat(caches.add(first)).isTrue();

        final Geocache second = new Geocache();
        second.setGeocode("2");
        assertThat(caches.add(second)).isTrue();

        assertThat(caches).hasSize(2);
        assertThat(caches.contains(first)).isTrue();
        assertThat(caches.contains(second)).isTrue();

        // adding first cache again does not change set
        assertThat(caches.add(first)).isFalse();
        assertThat(caches).hasSize(2);

        for (int i = 3; i <= 10; i++) {
            final Geocache cache = new Geocache();
            cache.setGeocode(Integer.toString(i));
            assertThat(caches.add(cache)).isTrue();
        }

        assertThat(caches).hasSize(10);
        assertThat(caches.contains(first)).isTrue();
        assertThat(caches.contains(second)).isTrue();

        final Geocache c11 = new Geocache();
        c11.setGeocode("11");
        assertThat(caches.add(c11)).isTrue();

        assertThat(caches).hasSize(10);

        // first was used again, there second is the oldest and has been overwritten by 11
        assertThat(caches.contains(first)).isTrue();
        assertThat(caches.contains(second)).isFalse();
    }
}