package cgeo.geocaching.filters;

import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.ConstantGeocacheFilter;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.LogicalGeocacheFilter;
import cgeo.geocaching.filters.core.NotGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LogicalGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void andFilter() {
        assertLogicFilter(AndGeocacheFilter.create(), true, getConstantFilter(true), getConstantFilter(true));
        assertLogicFilter(AndGeocacheFilter.create(), false, getConstantFilter(false), getConstantFilter(true));
        assertLogicFilter(AndGeocacheFilter.create(), null, getConstantFilter(null), getConstantFilter(true));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void orFilter() {
        assertLogicFilter(OrGeocacheFilter.create(), true, getConstantFilter(true), getConstantFilter(true));
        assertLogicFilter(OrGeocacheFilter.create(), true, getConstantFilter(false), getConstantFilter(true));
        assertLogicFilter(OrGeocacheFilter.create(), false, getConstantFilter(false), getConstantFilter(false));
        assertLogicFilter(OrGeocacheFilter.create(), true, getConstantFilter(null), getConstantFilter(true));
        assertLogicFilter(OrGeocacheFilter.create(), null, getConstantFilter(null), getConstantFilter(false));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void notFilter() {
        assertLogicFilter(NotGeocacheFilter.create(), false, getConstantFilter(true));
        assertLogicFilter(NotGeocacheFilter.create(), true, getConstantFilter(false));
        assertLogicFilter(NotGeocacheFilter.create(), null, getConstantFilter(null));
    }

    private static void assertLogicFilter(final LogicalGeocacheFilter filter, final Boolean expectedResult, final IGeocacheFilter... children) {
        for (IGeocacheFilter child : children) {
            filter.addChild(child);
        }
        assertThat(filter.filter(null)).isEqualTo(expectedResult);
    }


    private static IGeocacheFilter getConstantFilter(final Boolean returnValue) {
        return returnValue == null ? ConstantGeocacheFilter.ALWAYS_NULL :
            (returnValue ? ConstantGeocacheFilter.ALWAYS_TRUE : ConstantGeocacheFilter.ALWAYS_FALSE);
    }

}
