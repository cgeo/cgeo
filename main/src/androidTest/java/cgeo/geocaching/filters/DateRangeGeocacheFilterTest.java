package cgeo.geocaching.filters;

import cgeo.geocaching.filters.core.DateRangeGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.functions.Action1;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * This class tests the Date filters, using HIDDEN filter as an example
 */
public class DateRangeGeocacheFilterTest {

    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
    private static final DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd", Locale.US);


    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void simple() throws Exception {
        final Date d = FORMATTER.parse("2022-04-06");
        assertSingle(c -> c.setHidden(d), dd -> dd.setMinMaxDate(d, d), true);
    }

    @Test
    public void oneMillisecondDifference() throws Exception {
        final Date d = new Date((FORMATTER.parse("2022-04-06").getTime() / MILLIS_PER_DAY) * MILLIS_PER_DAY);
        assertThat(FORMATTER.format(d)).isEqualTo("2022-04-06");
        final Date dDaySameLater = new Date(d.getTime() + 1);
        assertThat(FORMATTER.format(dDaySameLater)).isEqualTo("2022-04-06");
        final Date dDayBefore = new Date(d.getTime() - 1);
        assertThat(FORMATTER.format(dDayBefore)).isEqualTo("2022-04-05");

        assertSingle(c -> c.setHidden(dDayBefore), dd -> dd.setMinMaxDate(d, null), false);
        assertSingle(c -> c.setHidden(d), dd -> dd.setMinMaxDate(null, dDayBefore), false);

        assertSingle(c -> c.setHidden(dDaySameLater), dd -> dd.setMinMaxDate(null, d), true);
        assertSingle(c -> c.setHidden(d), dd -> dd.setMinMaxDate(dDaySameLater, null), true);
    }

    /**
     * Test for issue #17633: Events at midnight today should be included when filtering
     * from "today" onwards using relative dates.
     */
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void relativeDateTodayIncludesEventAtMidnight() {
        // Create event date at midnight today
        final Date todayMidnight = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

        // Filter from today (offset 0) to far future
        assertSingle(c -> c.setHidden(todayMidnight), dd -> dd.setRelativeMinMaxDays(0, Integer.MAX_VALUE), true);
    }

    /**
     * Test that events yesterday are excluded when filtering from today onwards.
     */
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void relativeDateTodayExcludesYesterday() {
        // Create event date yesterday
        final Date todayMidnight = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
        final Date yesterday = DateUtils.addDays(todayMidnight, -1);

        // Filter from today (offset 0) to far future
        assertSingle(c -> c.setHidden(yesterday), dd -> dd.setRelativeMinMaxDays(0, Integer.MAX_VALUE), false);
    }

    /**
     * Test that events tomorrow are included when filtering from today onwards.
     */
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") // is done in called test method
    public void relativeDateTodayIncludesTomorrow() {
        // Create event date tomorrow
        final Date todayMidnight = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
        final Date tomorrow = DateUtils.addDays(todayMidnight, 1);

        // Filter from today (offset 0) to far future
        assertSingle(c -> c.setHidden(tomorrow), dd -> dd.setRelativeMinMaxDays(0, Integer.MAX_VALUE), true);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void ignoreYearSameDayDifferentYears() throws Exception {
        // Test that same day in different years matches when ignoreYear is true
        final Date date2020 = FORMATTER.parse("2020-04-06");
        final Date date2021 = FORMATTER.parse("2021-04-06");
        final Date date2022 = FORMATTER.parse("2022-04-06");

        assertSingle(c -> c.setHidden(date2020), dd -> {
            dd.setMinMaxDate(date2021, date2021);
            dd.getDateFilter().setIgnoreYear(true);
        }, true);
        assertSingle(c -> c.setHidden(date2022), dd -> {
            dd.setMinMaxDate(date2021, date2021);
            dd.getDateFilter().setIgnoreYear(true);
        }, true);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void ignoreYearDifferentDays() throws Exception {
        // Test that different days don't match even with ignoreYear
        final Date april6 = FORMATTER.parse("2022-04-06");
        final Date april7 = FORMATTER.parse("2022-04-07");

        assertSingle(c -> c.setHidden(april6), dd -> {
            dd.setMinMaxDate(april7, april7);
            dd.getDateFilter().setIgnoreYear(true);
        }, false);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void ignoreYearRangeWithinYear() throws Exception {
        // Test a range that doesn't wrap around year boundary
        final Date march1 = FORMATTER.parse("2022-03-01");
        final Date june30 = FORMATTER.parse("2022-06-30");
        final Date april15 = FORMATTER.parse("2021-04-15");

        // april15 should match because it's between march1 and june30 (ignoring year)
        assertSingle(c -> c.setHidden(april15), dd -> {
            dd.setMinMaxDate(march1, june30);
            dd.getDateFilter().setIgnoreYear(true);
        }, true);

        final Date february15 = FORMATTER.parse("2022-02-15");
        // february15 should NOT match
        assertSingle(c -> c.setHidden(february15), dd -> {
            dd.setMinMaxDate(march1, june30);
            dd.getDateFilter().setIgnoreYear(true);
        }, false);
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    public void ignoreYearRangeWrappingYearBoundary() throws Exception {
        // Test a range that wraps around year boundary (e.g., Nov to Feb)
        final Date november1 = FORMATTER.parse("2022-11-01");
        final Date february28 = FORMATTER.parse("2022-02-28");

        // December should match
        final Date december15 = FORMATTER.parse("2021-12-15");
        assertSingle(c -> c.setHidden(december15), dd -> {
            dd.setMinMaxDate(november1, february28);
            dd.getDateFilter().setIgnoreYear(true);
        }, true);

        // January should match
        final Date january15 = FORMATTER.parse("2022-01-15");
        assertSingle(c -> c.setHidden(january15), dd -> {
            dd.setMinMaxDate(november1, february28);
            dd.getDateFilter().setIgnoreYear(true);
        }, true);

        // March should NOT match
        final Date march15 = FORMATTER.parse("2022-03-15");
        assertSingle(c -> c.setHidden(march15), dd -> {
            dd.setMinMaxDate(november1, february28);
            dd.getDateFilter().setIgnoreYear(true);
        }, false);
    }

    private void assertSingle(final Action1<Geocache> cacheSetter, final Action1<DateRangeGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.HIDDEN, cacheSetter, filterSetter, expectedResult);
    }
}
