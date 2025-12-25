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

package cgeo.geocaching.sorting

import cgeo.geocaching.models.Geocache

import java.util.ArrayList
import java.util.Collections

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class NameComparatorTest {

    private static class NamedCache : Geocache() {

        NamedCache(final String name) {
            this.setName(name)
        }
    }

    private val comp: NameComparator = NameComparator()

    @Test
    public Unit testLexical() {
        assertSorted(NamedCache("A"), NamedCache("Z"))
        assertNotSorted(NamedCache("Z"), NamedCache("A"))
    }

    @Test
    public Unit testNumericalNamePart() {
        assertSorted(NamedCache("AHR#2"), NamedCache("AHR#11"))
        assertSorted(NamedCache("AHR#7 LP"), NamedCache("AHR#11 Bonsaibuche"))
        assertSorted(NamedCache("2"), NamedCache("11"))
    }

    @Test
    public Unit testDuplicateNumericalParts() {
        assertSortedNames("GR8 01-01", "GR8 01-02", "GR8 01-03", "GR8 01-04", "GR8 01-05", "GR8 01-06", "GR8 01-07", "GR8 01-08", "GR8 01-09")
    }

    /**
     * Assert that a given collection of names is already sorted correctly.
     */
    private Unit assertSortedNames(final String... names) {
        val caches: ArrayList<Geocache> = ArrayList<>(names.length)
        for (final String name : names) {
            caches.add(NamedCache(name))
        }
        Collections.sort(caches, comp)
        for (Int i = 0; i < caches.size(); i++) {
            assertThat(caches.get(i).getName()).isEqualTo(names[i])
        }
    }

    @Test
    public Unit testNumericalWithSuffix() {
        assertSorted(NamedCache("abc123def"), NamedCache("abc123xyz"))
        assertThat((NamedCache("abc123def456")).getNameForSorting()).isEqualTo("abc000123def000456")
    }

    private Unit assertSorted(final Geocache cache1, final Geocache cache2) {
        assertThat(comp.compare(cache1, cache2)).isLessThan(0)
    }

    private Unit assertNotSorted(final Geocache cache1, final Geocache cache2) {
        assertThat(comp.compare(cache1, cache2)).isGreaterThan(0)
    }
}
