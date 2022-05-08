package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class NameComparatorTest {

    private static class NamedCache extends Geocache {

        NamedCache(final String name) {
            this.setName(name);
        }
    }

    private final NameComparator comp = new NameComparator();

    @Test
    public void testLexical() {
        assertSorted(new NamedCache("A"), new NamedCache("Z"));
        assertNotSorted(new NamedCache("Z"), new NamedCache("A"));
    }

    @Test
    public void testNumericalNamePart() {
        assertSorted(new NamedCache("AHR#2"), new NamedCache("AHR#11"));
        assertSorted(new NamedCache("AHR#7 LP"), new NamedCache("AHR#11 Bonsaibuche"));
        assertSorted(new NamedCache("2"), new NamedCache("11"));
    }

    @Test
    public void testDuplicateNumericalParts() {
        assertSortedNames("GR8 01-01", "GR8 01-02", "GR8 01-03", "GR8 01-04", "GR8 01-05", "GR8 01-06", "GR8 01-07", "GR8 01-08", "GR8 01-09");
    }

    /**
     * Assert that a given collection of names is already sorted correctly.
     */
    private void assertSortedNames(final String... names) {
        final ArrayList<Geocache> caches = new ArrayList<>(names.length);
        for (final String name : names) {
            caches.add(new NamedCache(name));
        }
        Collections.sort(caches, comp);
        for (int i = 0; i < caches.size(); i++) {
            assertThat(caches.get(i).getName()).isEqualTo(names[i]);
        }
    }

    @Test
    public void testNumericalWithSuffix() {
        assertSorted(new NamedCache("abc123def"), new NamedCache("abc123xyz"));
        assertThat((new NamedCache("abc123def456")).getNameForSorting()).isEqualTo("abc000123def000456");
    }

    private void assertSorted(final Geocache cache1, final Geocache cache2) {
        assertThat(comp.compare(cache1, cache2)).isLessThan(0);
    }

    private void assertNotSorted(final Geocache cache1, final Geocache cache2) {
        assertThat(comp.compare(cache1, cache2)).isGreaterThan(0);
    }
}
