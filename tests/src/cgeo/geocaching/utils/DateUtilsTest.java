package cgeo.geocaching.utils;

import java.util.Calendar;

import junit.framework.TestCase;

public class DateUtilsTest extends TestCase {

    public static void testDaysSince() {
        Calendar start = Calendar.getInstance();
        for (int hour = 0; hour < 24; hour++) {
            start.set(Calendar.HOUR_OF_DAY, hour);
            assertEquals(0, DateUtils.daysSince(start.getTimeInMillis()));
        }
    }

}
