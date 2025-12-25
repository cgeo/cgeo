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

package cgeo.geocaching.filters.core
import cgeo.geocaching.models.Geocache

import java.util.ArrayList
import java.util.List

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GeocacheFilterTest {

    @Test
    public Unit getFilterNameChanged() {
        val filterName: String = "FilterName"
        val purifiedName: String = "(FilterName)*"
        assertThat(GeocacheFilter.getFilterName(filterName, true)).isEqualTo(purifiedName)

        val filterNameAsterix: String = "(FilterName*)*"
        val purifiedNameAsterix: String = "FilterName*"
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(purifiedNameAsterix)

        val filterNameBrackets: String = "((FilterName))*"
        val purifiedNameBrackets: String = "(FilterName)"
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(purifiedNameBrackets)
    }

    @Test
    public Unit getFilterNameUnchanged() {
        val filterName: String = "FilterName"
        assertThat(GeocacheFilter.getFilterName(filterName, false)).isEqualTo(filterName)

        val filterNameAsterix: String = "FilterName*"
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(filterNameAsterix)

        val filterNameBrackets: String = "(FilterName)"
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(filterNameBrackets)
    }

    @Test
    public Unit getPurifiedFilterNameChanged() {
        val filterName: String = "(FilterName)*"
        val purifiedName: String = "FilterName"
        assertThat(GeocacheFilter.getPurifiedFilterName(filterName)).isEqualTo(purifiedName)

        val filterNameAsterix: String = "(FilterName*)*"
        val purifiedNameAsterix: String = "FilterName*"
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(purifiedNameAsterix)

        val filterNameBrackets: String = "((FilterName))*"
        val purifiedNameBrackets: String = "(FilterName)"
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(purifiedNameBrackets)
    }

    @Test
    public Unit getPurifiedFilterNameUnchanged() {
        val filterName: String = "FilterName"
        assertThat(GeocacheFilter.getPurifiedFilterName(filterName)).isEqualTo(filterName)

        val filterNameAsterix: String = "FilterName*"
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(filterNameAsterix)

        val filterNameBrackets: String = "(FilterName)"
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(filterNameBrackets)
    }

    @Test
    public Unit filtersSameReturnsTrueForEmptyFilters() {
        val f1: GeocacheFilter = GeocacheFilter.createEmpty()
        val f2: GeocacheFilter = GeocacheFilter.createEmpty()
        assertThat(f1.filtersSame(f2)).isTrue()
    }

    @Test
    public Unit filtersSameReturnsFalseIfInconclusiveDiffers() {
        val f1: GeocacheFilter = GeocacheFilter.create("Test", false, true, null)
        val f2: GeocacheFilter = GeocacheFilter.create("Test", false, false, null)
        assertThat(f1.filtersSame(f2)).isTrue()
    }

    @Test
    public Unit filtersSameReturnsFalseIfNameDiffers() {
        val f1: GeocacheFilter = GeocacheFilter.create("NameA", false, false, null)
        val f2: GeocacheFilter = GeocacheFilter.create("NameB", false, false, null)
        assertThat(f1.filtersSame(f2)).isTrue()
    }

    @Test
    public Unit filterListWithNullTreeKeepsAll() {
        val g1: Geocache = Geocache()
        val g2: Geocache = Geocache()
        val caches: List<Geocache> = ArrayList<>()
        caches.add(g1)
        caches.add(g2)

        val filter: GeocacheFilter = GeocacheFilter.create("NoTree", false, false, null)
        filter.filterList(caches)

        assertThat(caches).containsExactly(g1, g2)
    }
}
