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

    private void assertSingle(final Action1<Geocache> cacheSetter, final Action1<DateRangeGeocacheFilter> filterSetter, final Boolean expectedResult) {
        GeocacheFilterTestUtils.testSingle(GeocacheFilterType.HIDDEN, cacheSetter, filterSetter, expectedResult);
    }
}
