package cgeo.geocaching.filters;

import cgeo.geocaching.filters.core.DifficultyGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.functions.Action1;

import java.util.Arrays;

import org.junit.Test;

/**
 * This class tests the number range filters, using DIFFICULTY filter as an example
 */
public class NumberRangeGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void simple() {
        assertSingle(c -> c.setDifficulty(2f), f -> f.setMinMaxRange(2f, 2f), true);
        assertSingle(c -> c.setDifficulty(2f), f -> f.setMinMaxRange(1.5f, 3f), true);
        assertSingle(c -> c.setDifficulty(2f), f -> f.setMinMaxRange(2.5f, 4f), false);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void allNone() {
        assertSingle(c -> c.setDifficulty(0f), null, true);
        assertSingle(c -> c.setDifficulty(0f), f -> f.setMinMaxRange(null, 5f), true);
        assertSingle(c -> c.setDifficulty(0f), f -> f.setMinMaxRange(1f, 5f), false);
        assertSingle(c -> c.setDifficulty(0f), f -> f.setMinMaxRange(1.5f, 5f), false);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void rangeFromValues() {
        assertSingle(c -> c.setDifficulty(5f), f -> f.setRangeFromValues(Arrays.asList(2f, 5f)), true);
        assertSingle(c -> c.setDifficulty(5f), f -> f.setRangeFromValues(Arrays.asList(2f, 3f)), false);
        assertSingle(c -> c.setDifficulty(5f), f -> f.setRangeFromValues(Arrays.asList(2f, 3f), 2f, 3f), true);
        assertSingle(c -> c.setDifficulty(1f), f -> f.setRangeFromValues(Arrays.asList(2f, 3f), 2f, 3f), true);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void specialNumber() {
        assertSingle(c -> c.setDifficulty(5f), f -> {
        }, true);
        assertSingle(c -> c.setDifficulty(5f), f -> f.setSpecialNumber(5f, true), true);
        assertSingle(c -> c.setDifficulty(5f), f -> f.setSpecialNumber(5f, false), false);
        assertSingle(c -> c.setDifficulty(3f), f -> f.setSpecialNumber(5f, true), true);

        //combine with min/max
        assertSingle(c -> c.setDifficulty(5f), f -> {
            f.setSpecialNumber(5f, true);
            f.setMinMaxRange(1f, 4f);
        }, true);
        assertSingle(c -> c.setDifficulty(5f), f -> {
            f.setSpecialNumber(5f, false);
            f.setMinMaxRange(1f, 6f);
        }, false);

    }

    private void assertSingle(final Action1<Geocache> cacheSetter, final Action1<DifficultyGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.DIFFICULTY, cacheSetter, filterSetter, expectedResult);
    }
}
