package cgeo.geocaching.filters;

import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.LogicalGeocacheFilter;
import cgeo.geocaching.filters.core.NotGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

import androidx.annotation.Nullable;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class LogicalGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void andFilter() {
        testLogicFilter(new AndGeocacheFilter(), true, getConstantFilter(true), getConstantFilter(true));
        testLogicFilter(new AndGeocacheFilter(), false, getConstantFilter(false), getConstantFilter(true));
        testLogicFilter(new AndGeocacheFilter(), null, getConstantFilter(null), getConstantFilter(true));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void orFilter() {
        testLogicFilter(new OrGeocacheFilter(), true, getConstantFilter(true), getConstantFilter(true));
        testLogicFilter(new OrGeocacheFilter(), true, getConstantFilter(false), getConstantFilter(true));
        testLogicFilter(new OrGeocacheFilter(), false, getConstantFilter(false), getConstantFilter(false));
        testLogicFilter(new OrGeocacheFilter(), true, getConstantFilter(null), getConstantFilter(true));
        testLogicFilter(new OrGeocacheFilter(), null, getConstantFilter(null), getConstantFilter(false));
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void notFilter() {
        testLogicFilter(new NotGeocacheFilter(), false, getConstantFilter(true));
        testLogicFilter(new NotGeocacheFilter(), true, getConstantFilter(false));
        testLogicFilter(new NotGeocacheFilter(), null, getConstantFilter(null));
    }

    private static void testLogicFilter(final LogicalGeocacheFilter filter, final Boolean expectedResult, final IGeocacheFilter ... children) {
        for (IGeocacheFilter child : children) {
            filter.addChild(child);
        }
        assertThat(filter.filter(null)).isEqualTo(expectedResult);
    }


    private static IGeocacheFilter getConstantFilter(final Boolean returnValue) {
        return new IGeocacheFilter() {
            @Nullable
            @Override
            public Boolean filter(final Geocache cache) {
                return returnValue;
            }

            @Override
            public GeocacheFilterType getType() {
                return null;
            }

            @Override
            public boolean isFiltering() {
                return false;
            }

            @Override
            public String getId() {
                return "constant";
            }

            @Override
            public void setConfig(final ExpressionConfig config) {
                //no implementation needed
            }

            @Override
            public ExpressionConfig getConfig() {
                return null;
            }
        };
    }

}
