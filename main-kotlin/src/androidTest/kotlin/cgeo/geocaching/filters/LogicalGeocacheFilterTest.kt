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

import cgeo.geocaching.filters.core.AndGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.filters.core.LogicalGeocacheFilter
import cgeo.geocaching.filters.core.NotGeocacheFilter
import cgeo.geocaching.filters.core.OrGeocacheFilter
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.config.LegacyFilterConfig

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class LogicalGeocacheFilterTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit andFilter() {
        assertLogicFilter(AndGeocacheFilter(), true, getConstantFilter(true), getConstantFilter(true))
        assertLogicFilter(AndGeocacheFilter(), false, getConstantFilter(false), getConstantFilter(true))
        assertLogicFilter(AndGeocacheFilter(), null, getConstantFilter(null), getConstantFilter(true))
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit orFilter() {
        assertLogicFilter(OrGeocacheFilter(), true, getConstantFilter(true), getConstantFilter(true))
        assertLogicFilter(OrGeocacheFilter(), true, getConstantFilter(false), getConstantFilter(true))
        assertLogicFilter(OrGeocacheFilter(), false, getConstantFilter(false), getConstantFilter(false))
        assertLogicFilter(OrGeocacheFilter(), true, getConstantFilter(null), getConstantFilter(true))
        assertLogicFilter(OrGeocacheFilter(), null, getConstantFilter(null), getConstantFilter(false))
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit notFilter() {
        assertLogicFilter(NotGeocacheFilter(), false, getConstantFilter(true))
        assertLogicFilter(NotGeocacheFilter(), true, getConstantFilter(false))
        assertLogicFilter(NotGeocacheFilter(), null, getConstantFilter(null))
    }

    private static Unit assertLogicFilter(final LogicalGeocacheFilter filter, final Boolean expectedResult, final IGeocacheFilter... children) {
        for (IGeocacheFilter child : children) {
            filter.addChild(child)
        }
        assertThat(filter.filter(null)).isEqualTo(expectedResult)
    }


    private static IGeocacheFilter getConstantFilter(final Boolean returnValue) {
        return IGeocacheFilter() {
            override             public Boolean filter(final Geocache cache) {
                return returnValue
            }

            override             public GeocacheFilterType getType() {
                return null
            }

            override             public Boolean isFiltering() {
                return false
            }

            override             public String getId() {
                return "constant"
            }

            override             public Unit setConfig(final LegacyFilterConfig config) {
                //no implementation needed
            }

            override             public LegacyFilterConfig getConfig() {
                return null
            }

            override             public ObjectNode getJsonConfig() {
                return null
            }

            override             public Unit setJsonConfig(final ObjectNode node) {
                //no implementation needed
            }
        }
    }

}
