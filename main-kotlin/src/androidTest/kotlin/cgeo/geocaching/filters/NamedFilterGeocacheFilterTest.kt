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

package cgeo.geocaching.filters

import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.NamedFilterGeocacheFilter
import cgeo.geocaching.filters.core.TypeGeocacheFilter
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.functions.Action1

import java.util.Arrays

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class NamedFilterGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit simple() {
        singleNamedFilter(c -> c.setType(CacheType.TRADITIONAL), f -> f.setNamedFilter(createSimpleNamedTypeFilter("test", CacheType.TRADITIONAL)), true)
        singleNamedFilter(c -> c.setType(CacheType.MYSTERY), f -> f.setNamedFilter(createSimpleNamedTypeFilter("test", CacheType.TRADITIONAL)), false)
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit nonNamedFilter() {
        //when no named filter is configured, then everything is passed through
        singleNamedFilter(c -> c.setType(CacheType.MYSTERY), f -> f.setNamedFilter(createSimpleNamedTypeFilter(null, CacheType.TRADITIONAL)), true)
    }

    @Test
    public Unit preventEndlessLoopOnNesting() {
        //create a named filter which references itself
        val filterInside: NamedFilterGeocacheFilter = NamedFilterGeocacheFilter()
        val named: GeocacheFilter = GeocacheFilter.create("named", true, false, filterInside)
        filterInside.setNamedFilter(named)

        //assert that filter executes w/o producing infinite loop and that it doesn't filter any cache
        val cache: Geocache = Geocache()
        cache.setType(CacheType.TRADITIONAL)
        assertThat(named.filter(cache)).isTrue()
    }


    private Unit singleNamedFilter(final Action1<Geocache> cacheSetter, final Action1<NamedFilterGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.NAMED_FILTER, cacheSetter, filterSetter, expectedResult)
    }

    private GeocacheFilter createSimpleNamedTypeFilter(final String name, final CacheType ... types) {
        val tree: TypeGeocacheFilter = TypeGeocacheFilter()
        tree.setValues(Arrays.asList(types))
        return GeocacheFilter.create(name, true, false, tree)
    }

}
