package cgeo.geocaching.filters;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.StringFilter;
import cgeo.geocaching.filters.core.StringGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.functions.Action1;

import org.junit.Test;

/**
 * This class tests the string filters, using NAME filter as an example
 */
public class StringGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void contains() {
        testSingle(c -> c.setName("Testname"), f -> f.getStringFilter().setTextValue("est"), true);
        testSingle(c -> c.setName("Testname"), f -> f.getStringFilter().setTextValue("eFst"), false);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void doesNotContain() {
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("est");
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.DOES_NOT_CONTAIN);
        }, false);
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("eFst");
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.DOES_NOT_CONTAIN);
        }, true);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void startsWith() {
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("est");
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.STARTS_WITH);
        }, false);
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("name");
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.STARTS_WITH);
        }, false);
        testSingle(c -> c.setName("Testname"), f -> {
            f.getStringFilter().setTextValue("Test");
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.STARTS_WITH);
        }, true);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void pattern() {
        testPattern("Testname", "Test", false, false);
        testPattern("Testname", "T?st*", false, true);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void patternTreatsRegexAndSqlMetacharactersLiterally() {
        final String literal = "[]().+^$|\\_%'";
        testPattern(literal, literal, false, true);
        testPattern("[x]().+^$|\\_%'", literal, false, false);
        testPattern("cacheXname", "cache.name", false, false);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void patternWildcardsAndCaseAreEquivalentInLogicAndDb() {
        final String pattern = "prefix?[]().+^$|\\*_%'suffix";
        testPattern("PREFIXx[]().+^$|\\middle_%'SUFFIX", pattern, false, true);
        testPattern("prefix[]().+^$|\\middle_%'suffix", pattern, false, false);

        testPattern("a[bc]x", "A[BC]?", false, true);
        testPattern("a[bc]x", "A[BC]?", true, false);
        testPattern("A[BC]x", "A[BC]?", true, true);
        testPattern("a\nb\nc", "a?b*c", true, true);
    }

    private void testPattern(final String value, final String pattern, final boolean matchCase, final boolean expectedResult) {
        testSingle(c -> c.setName(value), f -> {
            f.getStringFilter().setTextValue(pattern);
            f.getStringFilter().setFilterType(StringFilter.StringFilterType.PATTERN);
            f.getStringFilter().setMatchCase(matchCase);
        }, expectedResult);
    }

    private void testSingle(final Action1<Geocache> cacheSetter, final Action1<StringGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.NAME, cacheSetter, filterSetter, expectedResult);
    }
}
