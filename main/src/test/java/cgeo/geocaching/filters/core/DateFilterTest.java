package cgeo.geocaching.filters.core;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DateFilter class, particularly testing the behavior
 * with relative dates and events at midnight.
 */
public class DateFilterTest {

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    /**
     * Test that events happening at midnight today are included when filtering from "today" onwards.
     * This tests the fix for issue #17633.
     *
     * The issue was that when using relative dates (e.g., "today"), the filter would use the current
     * time (e.g., 13:58:23) rather than midnight (00:00:00). This caused events at midnight to be
     * excluded because the integer division by MILLIS_PER_DAY would put them in different day buckets.
     */
    @Test
    public void testTodayFilterIncludesEventAtMidnight() throws Exception {
        // Create a date representing an event at midnight today
        final Date today = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
        final Date eventAtMidnight = today;

        // Create a DateFilter with relative offset 0 (today) to future
        final DateFilter filter = new DateFilter();
        filter.setRelativeDays(0, Integer.MAX_VALUE);

        // The event at midnight today should match
        final Boolean result = filter.matches(eventAtMidnight);

        assertThat(result).as("Event at midnight today should be included").isTrue();
    }

    /**
     * Test that events in the past are excluded when filtering from "today" onwards.
     */
    @Test
    public void testTodayFilterExcludesPastEvents() throws Exception {
        // Create a date representing yesterday
        final Date today = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
        final Date eventYesterday = DateUtils.addDays(today, -1);

        // Create a DateFilter with relative offset 0 (today) to future
        final DateFilter filter = new DateFilter();
        filter.setRelativeDays(0, Integer.MAX_VALUE);

        // The event yesterday should not match
        final Boolean result = filter.matches(eventYesterday);

        assertThat(result).as("Event yesterday should be excluded").isFalse();
    }

    /**
     * Test that events in the future are included when filtering from "today" onwards.
     */
    @Test
    public void testTodayFilterIncludesFutureEvents() throws Exception {
        // Create a date representing tomorrow
        final Date today = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
        final Date eventTomorrow = DateUtils.addDays(today, 1);

        // Create a DateFilter with relative offset 0 (today) to future
        final DateFilter filter = new DateFilter();
        filter.setRelativeDays(0, Integer.MAX_VALUE);

        // The event tomorrow should match
        final Boolean result = filter.matches(eventTomorrow);

        assertThat(result).as("Event tomorrow should be included").isTrue();
    }

    /**
     * Test that the relative date filter properly normalizes to start of day.
     */
    @Test
    public void testRelativeDateNormalizesToStartOfDay() throws Exception {
        final DateFilter filter = new DateFilter();
        filter.setRelativeDays(0, 0); // Just today

        final Date minDate = filter.getMinDate();
        final Date maxDate = filter.getMaxDate();

        // Min date should be at start of today (00:00:00.000)
        assertThat(minDate).isNotNull();
        final Calendar minCal = Calendar.getInstance();
        minCal.setTime(minDate);
        assertThat(minCal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0);
        assertThat(minCal.get(Calendar.MINUTE)).isEqualTo(0);
        assertThat(minCal.get(Calendar.SECOND)).isEqualTo(0);
        assertThat(minCal.get(Calendar.MILLISECOND)).isEqualTo(0);

        // Max date should be at end of today (23:59:59.999)
        assertThat(maxDate).isNotNull();
        final Calendar maxCal = Calendar.getInstance();
        maxCal.setTime(maxDate);
        assertThat(maxCal.get(Calendar.HOUR_OF_DAY)).isEqualTo(23);
        assertThat(maxCal.get(Calendar.MINUTE)).isEqualTo(59);
        assertThat(maxCal.get(Calendar.SECOND)).isEqualTo(59);
        assertThat(maxCal.get(Calendar.MILLISECOND)).isEqualTo(999);
    }

    /**
     * Test filtering with a specific date range (not relative).
     */
    @Test
    public void testAbsoluteDateFilterIncludesSameDay() throws Exception {
        final Date date = FORMATTER.parse("2025-12-27 00:00:00");
        final Date eventAtNoon = FORMATTER.parse("2025-12-27 12:00:00");

        final DateFilter filter = new DateFilter();
        filter.setMinMaxDate(date, date);

        // Event on the same day should match
        final Boolean result = filter.matches(eventAtNoon);

        assertThat(result).as("Event on the same day should be included").isTrue();
    }
}
