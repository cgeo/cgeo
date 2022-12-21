package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class EventTimeParserTest extends TestCase {

    public static void testGuessEventTimeStandard() {
        assertTime("text 14:20 text", 14, 20);
    }

    public static void testIllegalHoursNoTime() {
        assertNoTime("text 30:40 text");
    }

    public static void testIllegalMinutesNoTime() {
        assertNoTime("text 14:90 text");
    }

    public static void testFullHourOnly() {
        assertTime("text 16 " + getHoursKeyword(), 16, 0);
    }

    public static void testFullHourLowercase() {
        assertTime("text 16 " + StringUtils.lowerCase(getHoursKeyword()), 16, 0);
    }

    public static void testHoursMinutesStandardTimeSeparator() {
        assertTime("text 16:00 " + getHoursKeyword(), 16, 0);
    }

    public static void testHoursMinutesWrongTimeSeparator() {
        assertTime("text 16.00 " + getHoursKeyword(), 16, 0);
    }

    public static void testEndOfSentence() {
        assertTime("text 14:20.", 14, 20);
    }

    public static void testWithHTMLFormatting() {
        assertTime("<b>14:20</b>", 14, 20);
    }

    public static void testTimeRanges() {
        assertTime("<u><em>Uhrzeit:</em></u> 17-20 " + getHoursKeyword() + "</span></strong>", 17, 0);
        assertTime("von 11 bis 13 " + getHoursKeyword(), 11, 0);
        assertTime("from 11 to 13 " + getHoursKeyword(), 11, 0);
        assertTime("von 19.15 " + getHoursKeyword() + " bis ca.20.30 " + getHoursKeyword() + " statt", 19, 15);
    }

    public static void testTimeRangesWithoutBlank() {
        assertTime("text 16" + getHoursKeyword(), 16, 0);
        assertTime("text 16" + StringUtils.lowerCase(getHoursKeyword()), 16, 0);
        assertTime("text 16:00" + getHoursKeyword(), 16, 0);
        assertTime("text 16.00" + getHoursKeyword(), 16, 0);
        assertTime("<u><em>Uhrzeit:</em></u> 17-20" + getHoursKeyword() + "</span></strong>", 17, 0);
        assertTime("von 11 bis 13" + getHoursKeyword(), 11, 0);
        assertTime("from 11 to 13" + getHoursKeyword(), 11, 0);
        assertTime("von 19.15" + getHoursKeyword() + " bis ca.20.30 " + getHoursKeyword() + " statt", 19, 15);
    }

    /**
     * issue #6285
     */
    public static void testMissingSpaceBeforeHoursKeyword() {
        assertTime("Dienstag den 31. Januar ab 18:00" + getHoursKeyword() + " (das Logbuch liegt bis mind. 20:30 " + getHoursKeyword() + " aus)", 18, 0);
    }

    /**
     * see https://www.geocaching.com/geocache/GC7MZG3_ludwigsburger-stammtisch-50
     */
    public static void testEventTimeStandardFormatCSS() {
        assertTime("<div class=\"dtstart_2018-04-27T12:00:00\" style=\"text-indent:15px;\">Start: 27.04.2018", 12, 00);
    }

    public static void testGuessEventTimeShortDescriptionOnly() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.EVENT);
        cache.setDescription(StringUtils.EMPTY);
        cache.setShortDescription("text 14:20 text");
        assertThat(cache.getEventStartTimeInMinutes()).isEqualTo(14 * 60 + 20);
    }

    private static void assertTime(final String description, final int hours, final int minutes) {
        final int minutesAfterMidnight = hours * 60 + minutes;
        assertThat(EventTimeParser.guessEventTimeMinutes(description)).isEqualTo(minutesAfterMidnight);
    }

    private static void assertNoTime(final String description) {
        assertThat(EventTimeParser.guessEventTimeMinutes(description)).isEqualTo(-1);
    }

    private static String getHoursKeyword() {
        return CgeoApplication.getInstance().getString(R.string.cache_time_full_hours);
    }

}
