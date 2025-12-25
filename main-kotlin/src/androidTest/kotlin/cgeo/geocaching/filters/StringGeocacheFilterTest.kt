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

import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.StringFilter
import cgeo.geocaching.filters.core.StringGeocacheFilter
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.functions.Action1

import org.junit.Test

/**
 * This class tests the string filters, using NAME filter as an example
 */
class StringGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit contains() {
        testSingle(c -> c.setName("Testname"), f -> f.getStringFilter().setTextValue("est"), true)
        testSingle(c -> c.setName("Testname"), f -> f.getStringFilter().setTextValue("eFst"), false)
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit doesNotContain() {
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("est")
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.DOES_NOT_CONTAIN)
        }, false)
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("eFst")
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.DOES_NOT_CONTAIN)
        }, true)
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit startsWith() {
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("est")
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.STARTS_WITH)
        }, false)
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("name")
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.STARTS_WITH)
        }, false)
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("Test")
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.STARTS_WITH)
        }, true)
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit pattern() {
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("Test")
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.PATTERN)
        }, false)
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("T?st*")
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.PATTERN)
        }, true)
    }

    private Unit testSingle(final Action1<Geocache> cacheSetter, final Action1<StringGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.NAME, cacheSetter, filterSetter, expectedResult)
    }
}
