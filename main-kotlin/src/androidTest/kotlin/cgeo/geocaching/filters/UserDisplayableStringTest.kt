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
import cgeo.geocaching.filters.core.AndGeocacheFilter
import cgeo.geocaching.filters.core.DateFilter
import cgeo.geocaching.filters.core.DifficultyGeocacheFilter
import cgeo.geocaching.filters.core.FavoritesGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.filters.core.LastFoundGeocacheFilter
import cgeo.geocaching.filters.core.NotGeocacheFilter
import cgeo.geocaching.filters.core.OrGeocacheFilter
import cgeo.geocaching.filters.core.StatusGeocacheFilter
import cgeo.geocaching.filters.core.TypeGeocacheFilter
import cgeo.geocaching.filters.core.UserDisplayableStringUtils
import cgeo.geocaching.utils.functions.Action2

import java.text.ParseException
import java.util.Date
import java.util.Locale
import java.util.Stack

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Assert.fail

class UserDisplayableStringTest {

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit testDifficulty() {
        val f: DifficultyGeocacheFilter = GeocacheFilterType.DIFFICULTY.create()

        val minValue: Float = 2f
        val maxValue: Float = 4f
        val minValueOutput: String = "2.0"
        val maxValueOutput: String = "4.0"

        testUserDisplayStringsForRange(f, f::setMinMaxRange, minValue, maxValue, minValueOutput, maxValueOutput)
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit testFavoritesPercentage() {
        val f: FavoritesGeocacheFilter = GeocacheFilterType.FAVORITES.create()
        f.setPercentage(true)

        val minValue: Float = 2f
        val maxValue: Float = 4f
        val minValueOutput: String = "200%"
        val maxValueOutput: String = "400%"

        testUserDisplayStringsForRange(f, f::setMinMaxRange, minValue, maxValue, minValueOutput, maxValueOutput)
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public Unit testLastFound() {
        val f: LastFoundGeocacheFilter = GeocacheFilterType.LAST_FOUND.create()

        Locale.setDefault(Locale.US)

        final Date minValue
        final Date maxValue
        try {
            minValue = DateFilter.DAY_DATE_FORMAT_USER_DISPLAY.parse("2022-04-06")
            maxValue = DateFilter.DAY_DATE_FORMAT_USER_DISPLAY.parse("2022-06-06")
        } catch (ParseException pe) {
            fail("Problem parsing the test dates")
            return
        }

        val minValueOutput: String = DateFilter.DAY_DATE_FORMAT_USER_DISPLAY.format(minValue)
        val maxValueOutput: String = DateFilter.DAY_DATE_FORMAT_USER_DISPLAY.format(maxValue)

        testUserDisplayStringsForRange(f, f::setMinMaxDate, minValue, maxValue, minValueOutput, maxValueOutput)
    }

    @Test
    public Unit testAndFilter() {
        val typeFilter: TypeGeocacheFilter = GeocacheFilterType.TYPE.create()

        val testTypes: Stack<CacheType> = Stack<>()
        testTypes.add(CacheType.TRADITIONAL)
        typeFilter.setValues(testTypes)

        val filterConfig: AndGeocacheFilter = AndGeocacheFilter()
        filterConfig.addChild(typeFilter)
        val statusFilter: StatusGeocacheFilter = GeocacheFilterType.STATUS.create()
        statusFilter.setStatusFound(true)
        statusFilter.setStatusOwned(true)
        filterConfig.addChild(statusFilter)

        val gcFilter: GeocacheFilter = GeocacheFilter.create("", false, false, filterConfig)

        val valueOutput: String = "Cache Type: Trad, Status: Found=Yes, Owned=Yes"

        assertThat(gcFilter.toUserDisplayableString()).as("display for filter").isEqualTo(valueOutput)
    }

    @Test
    public Unit testOrFilter() {
        val typeFilter: TypeGeocacheFilter = GeocacheFilterType.TYPE.create()

        val testTypes: Stack<CacheType> = Stack<>()
        testTypes.add(CacheType.TRADITIONAL)
        typeFilter.setValues(testTypes)

        val filterConfig: OrGeocacheFilter = OrGeocacheFilter()
        filterConfig.addChild(typeFilter)
        val statusFilter: StatusGeocacheFilter = GeocacheFilterType.STATUS.create()
        statusFilter.setStatusFound(true)
        statusFilter.setStatusOwned(true)
        filterConfig.addChild(statusFilter)

        val gcFilter: GeocacheFilter = GeocacheFilter.create("", false, false, filterConfig)

        val valueOutput: String = "Cache Type: Trad OR Status: Found=Yes, Owned=Yes"

        assertThat(gcFilter.toUserDisplayableString()).as("display for filter").isEqualTo(valueOutput)
    }

    @Test
    public Unit testNotFilter() {
        val typeFilter: TypeGeocacheFilter = GeocacheFilterType.TYPE.create()

        val testTypes: Stack<CacheType> = Stack<>()
        testTypes.add(CacheType.TRADITIONAL)
        typeFilter.setValues(testTypes)

        val filterConfig: NotGeocacheFilter = NotGeocacheFilter()
        filterConfig.addChild(typeFilter)
        val statusFilter: StatusGeocacheFilter = GeocacheFilterType.STATUS.create()
        statusFilter.setStatusFound(true)
        statusFilter.setStatusOwned(true)
        filterConfig.addChild(statusFilter)

        val gcFilter: GeocacheFilter = GeocacheFilter.create("", false, false, filterConfig)

        val valueOutput: String = "NOT[Cache Type: Trad, Status: Found=Yes, Owned=Yes]"

        assertThat(gcFilter.toUserDisplayableString()).as("display for filter").isEqualTo(valueOutput)
    }

    private <T> Unit testSingleUserDisplayStringForRange(final IGeocacheFilter filter, final Action2<T, T> filterSetter,
                                                         final T minValue, final T maxValue,
                                                         final String valueOutput) {
        val displayLevel: Int = 1
        if (filterSetter != null) {
            filterSetter.call(minValue, maxValue)
        }

        val filterName: String = filter.getType().getUserDisplayableName() + ": "
        assertThat(filter.toUserDisplayableString(displayLevel)).as("display for filter").isEqualTo(valueOutput != null ? filterName + valueOutput : null)
    }

    private <T> Unit testUserDisplayStringsForRange(final IGeocacheFilter filter, final Action2<T, T> filterSetter,
                                                    final T minValue, final T maxValue,
                                                    final String minValueOutput, final String maxValueOutput) {
        testSingleUserDisplayStringForRange(filter, filterSetter, null, null, null)
        testSingleUserDisplayStringForRange(filter, filterSetter, minValue, null, String(UserDisplayableStringUtils.GREATER_THAN_OR_EQUAL_TO) + minValueOutput)
        testSingleUserDisplayStringForRange(filter, filterSetter, minValue, maxValue, minValueOutput + "-" + maxValueOutput)
        testSingleUserDisplayStringForRange(filter, filterSetter, null, maxValue, String(UserDisplayableStringUtils.LESS_THAN_OR_EQUAL_TO) + maxValueOutput)
        testSingleUserDisplayStringForRange(filter, filterSetter, maxValue, maxValue, maxValueOutput)
    }
}
