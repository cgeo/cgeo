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

import cgeo.geocaching.filters.core.DateRangeGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.utils.functions.Action1

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

/**
 * This class tests the Date filters, using HIDDEN filter as an example
 */
class DateRangeGeocacheFilterTest {

    private static val MILLIS_PER_DAY: Long = 24 * 60 * 60 * 1000
    private static val FORMATTER: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)


    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit simple() throws Exception {
        val d: Date = FORMATTER.parse("2022-04-06")
        assertSingle(c -> c.setHidden(d), dd -> dd.setMinMaxDate(d, d), true)
    }

    @Test
    public Unit oneMillisecondDifference() throws Exception {
        val d: Date = Date((FORMATTER.parse("2022-04-06").getTime() / MILLIS_PER_DAY) * MILLIS_PER_DAY)
        assertThat(FORMATTER.format(d)).isEqualTo("2022-04-06")
        val dDaySameLater: Date = Date(d.getTime() + 1)
        assertThat(FORMATTER.format(dDaySameLater)).isEqualTo("2022-04-06")
        val dDayBefore: Date = Date(d.getTime() - 1)
        assertThat(FORMATTER.format(dDayBefore)).isEqualTo("2022-04-05")

        assertSingle(c -> c.setHidden(dDayBefore), dd -> dd.setMinMaxDate(d, null), false)
        assertSingle(c -> c.setHidden(d), dd -> dd.setMinMaxDate(null, dDayBefore), false)

        assertSingle(c -> c.setHidden(dDaySameLater), dd -> dd.setMinMaxDate(null, d), true)
        assertSingle(c -> c.setHidden(d), dd -> dd.setMinMaxDate(dDaySameLater, null), true)
    }

    private Unit assertSingle(final Action1<Geocache> cacheSetter, final Action1<DateRangeGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.HIDDEN, cacheSetter, filterSetter, expectedResult)
    }
}
