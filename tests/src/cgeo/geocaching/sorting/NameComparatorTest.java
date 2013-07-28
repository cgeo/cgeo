package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collections;

public class NameComparatorTest extends AndroidTestCase {

    private static class NamedCache extends Geocache {

        public NamedCache(final String name) {
            this.setName(name);
        }
    }

    private NameComparator comp = new NameComparator();

    public void testLexical() {
        assertSorted(new NamedCache("A"), new NamedCache("Z"));
        assertNotSorted(new NamedCache("Z"), new NamedCache("A"));
    }

    public void testNumericalNamePart() {
        assertSorted(new NamedCache("AHR#2"), new NamedCache("AHR#11"));
        assertSorted(new NamedCache("AHR#7 LP"), new NamedCache("AHR#11 Bonsaibuche"));
        assertSorted(new NamedCache("2"), new NamedCache("11"));
    }

    public void testDuplicateNumericalParts() {
        assertSortedNames("GR8 01-01", "GR8 01-02", "GR8 01-03", "GR8 01-04", "GR8 01-05", "GR8 01-06", "GR8 01-07", "GR8 01-08", "GR8 01-09");
    }

    /**
     * Assert that a given collection of names is already sorted correctly.
     *
     * @param names
     */
    private void assertSortedNames(String... names) {
        ArrayList<Geocache> caches = new ArrayList<Geocache>(names.length);
        for (String name : names) {
            caches.add(new NamedCache(name));
        }
        Collections.sort(caches, comp);
        for (int i = 0; i < caches.size(); i++) {
            assertEquals(names[i], caches.get(i).getName());
        }
    }

    public void testNumericalWithSuffix() {
        assertSorted(new NamedCache("abc123def"), new NamedCache("abc123xyz"));
        assertEquals("abc000123def000456", (new NamedCache("abc123def456")).getNameForSorting());
    }

    private void assertSorted(final Geocache cache1, final Geocache cache2) {
        assertTrue(comp.compare(cache1, cache2) < 0);
    }

    private void assertNotSorted(final Geocache cache1, final Geocache cache2) {
        assertTrue(comp.compare(cache1, cache2) > 0);
    }
}
