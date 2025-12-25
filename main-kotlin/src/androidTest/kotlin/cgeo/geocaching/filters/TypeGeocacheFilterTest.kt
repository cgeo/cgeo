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
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.TypeGeocacheFilter
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.functions.Action1

import java.util.Arrays
import java.util.Collections
import java.util.HashSet

import org.junit.Test

class TypeGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit simpleTradi() {
        singleType(c -> c.setType(CacheType.TRADITIONAL), f -> f.setValues(HashSet<>(Collections.singletonList(CacheType.TRADITIONAL))), true)
        singleType(c -> c.setType(CacheType.TRADITIONAL), f -> f.setValues(HashSet<>(Collections.singletonList(CacheType.MULTI))), false)
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit allNone() {
        singleType(c -> c.setType(CacheType.TRADITIONAL), null, true)
        singleType(c -> c.setType(CacheType.TRADITIONAL), f -> f.setValues(HashSet<>(Arrays.asList(CacheType.values()))), true)
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit grouping() {
        singleType(c -> c.setType(CacheType.MEGA_EVENT), f -> f.setValues(HashSet<>(Collections.singletonList(CacheType.COMMUN_CELEBRATION))), true)
    }

    private Unit singleType(final Action1<Geocache> cacheSetter, final Action1<TypeGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.TYPE, cacheSetter, filterSetter, expectedResult)
    }

}
