package cgeo.geocaching.filters.core;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class StringFilterTest {

    @Test
    public void patternTreatsRegexAndSqlMetacharactersLiterally() {
        final String literal = "[]().+^$|\\_%'";
        final StringFilter filter = createPatternFilter(literal, false);

        assertThat(filter.matches(literal)).isTrue();
        assertThat(filter.matches("[x]().+^$|\\_%'")).isFalse();
    }

    @Test
    public void patternUsesOnlyAsteriskAndQuestionMarkAsWildcards() {
        final StringFilter filter = createPatternFilter("prefix?[]().+^$|\\*suffix", false);

        assertThat(filter.matches("PREFIXx[]().+^$|\\middleSUFFIX")).isTrue();
        assertThat(filter.matches("prefix[]().+^$|\\middlesuffix")).isFalse();
    }

    @Test
    public void patternRespectsMatchCase() {
        assertThat(createPatternFilter("A[BC]?", false).matches("a[bc]x")).isTrue();
        assertThat(createPatternFilter("A[BC]?", true).matches("a[bc]x")).isFalse();
        assertThat(createPatternFilter("A[BC]?", true).matches("A[BC]x")).isTrue();
    }

    @Test
    public void patternWildcardsMatchLineBreaks() {
        assertThat(createPatternFilter("a?b*c", true).matches("a\nb\nc")).isTrue();
    }

    @Test
    public void patternBuildsEquivalentSqlForLiteralCharactersAndCase() {
        assertThat(createPatternFilter("[]().+^$|\\_%'", false).getRawLikeSqlExpression("name"))
                .isEqualTo("LOWER(name) GLOB '[[]]().+^$|\\_%'''");
        assertThat(createPatternFilter("A[BC]?", true).getRawLikeSqlExpression("name"))
                .isEqualTo("name GLOB 'A[[]BC]?'");
    }

    @Test
    public void patternHandlesRegexQuoteTerminatorLiterally() {
        assertThat(createPatternFilter("x\\Ey", true).matches("x\\Ey")).isTrue();
        assertThat(createPatternFilter("x\\Ey", true).matches("xy")).isFalse();
    }

    private static StringFilter createPatternFilter(final String pattern, final boolean matchCase) {
        final StringFilter filter = new StringFilter();
        filter.setTextValue(pattern);
        filter.setFilterType(StringFilter.StringFilterType.PATTERN);
        filter.setMatchCase(matchCase);
        return filter;
    }
}
