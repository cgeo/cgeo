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

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.models.Geocache

import org.apache.commons.lang3.StringUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class EventTimeParserTest {

    @Test
    public Unit testGuessEventTimeStandard() {
        assertTime("text 14:20 text", 14, 20)
    }

    @Test
    public Unit testIllegalHoursNoTime() {
        assertNoTime("text 30:40 text")
    }

    @Test
    public Unit testIllegalMinutesNoTime() {
        assertNoTime("text 14:90 text")
    }

    @Test
    public Unit testFullHourOnly() {
        assertTime("text 16 " + getHoursKeyword(), 16, 0)
    }

    @Test
    public Unit testFullHourLowercase() {
        assertTime("text 16 " + StringUtils.lowerCase(getHoursKeyword()), 16, 0)
    }

    @Test
    public Unit testHoursMinutesStandardTimeSeparator() {
        assertTime("text 16:00 " + getHoursKeyword(), 16, 0)
    }

    @Test
    public Unit testHoursMinutesWrongTimeSeparator() {
        assertTime("text 16.00 " + getHoursKeyword(), 16, 0)
    }

    @Test
    public Unit testEndOfSentence() {
        assertTime("text 14:20.", 14, 20)
    }

    @Test
    public Unit testWithHTMLFormatting() {
        assertTime("<b>14:20</b>", 14, 20)
    }

    @Test
    public Unit testTimeRanges() {
        assertTime("<u><em>Uhrzeit:</em></u> 17-20 " + getHoursKeyword() + "</span></strong>", 17, 0)
        assertTime("von 11 bis 13 " + getHoursKeyword(), 11, 0)
        assertTime("from 11 to 13 " + getHoursKeyword(), 11, 0)
        assertTime("von 19.15 " + getHoursKeyword() + " bis ca.20.30 " + getHoursKeyword() + " statt", 19, 15)
    }

    @Test
    public Unit testTimeRangesWithoutBlank() {
        assertTime("text 16" + getHoursKeyword(), 16, 0)
        assertTime("text 16" + StringUtils.lowerCase(getHoursKeyword()), 16, 0)
        assertTime("text 16:00" + getHoursKeyword(), 16, 0)
        assertTime("text 16.00" + getHoursKeyword(), 16, 0)
        assertTime("<u><em>Uhrzeit:</em></u> 17-20" + getHoursKeyword() + "</span></strong>", 17, 0)
        assertTime("von 11 bis 13" + getHoursKeyword(), 11, 0)
        assertTime("from 11 to 13" + getHoursKeyword(), 11, 0)
        assertTime("von 19.15" + getHoursKeyword() + " bis ca.20.30 " + getHoursKeyword() + " statt", 19, 15)
    }

    /**
     * issue #6285
     */
    @Test
    public Unit testMissingSpaceBeforeHoursKeyword() {
        assertTime("Dienstag den 31. Januar ab 18:00" + getHoursKeyword() + " (das Logbuch liegt bis mind. 20:30 " + getHoursKeyword() + " aus)", 18, 0)
    }

    /**
     * see <a href="https://www.geocaching.com/geocache/GC7MZG3_ludwigsburger-stammtisch-50">event Ludwigsburger Stammtisch used as example</a>
     */
    @Test
    public Unit testEventTimeStandardFormatCSS() {
        assertTime("<div class=\"dtstart_2018-04-27T12:00:00\" style=\"text-indent:15px;\">Start: 27.04.2018", 12, 00)
    }

    @Test
    public Unit testGuessEventTimeShortDescriptionOnly() {
        val cache: Geocache = Geocache()
        cache.setType(CacheType.EVENT)
        cache.setDescription(StringUtils.EMPTY)
        cache.setShortDescription("text 14:20 text")
        assertThat(cache.getEventStartTimeInMinutes()).isEqualTo(14 * 60 + 20)
    }

    private static Unit assertTime(final String description, final Int hours, final Int minutes) {
        val minutesAfterMidnight: Int = hours * 60 + minutes
        assertThat(EventTimeParser.guessEventTimeMinutes(description)).isEqualTo(minutesAfterMidnight)
    }

    private static Unit assertNoTime(final String description) {
        assertThat(EventTimeParser.guessEventTimeMinutes(description)).isEqualTo(-1)
    }

    private static String getHoursKeyword() {
        return CgeoApplication.getInstance().getString(R.string.cache_time_full_hours)
    }

}
