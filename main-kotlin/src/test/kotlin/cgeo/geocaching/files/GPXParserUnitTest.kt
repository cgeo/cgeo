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

package cgeo.geocaching.files

import java.text.ParseException
import java.util.Date
import java.util.GregorianCalendar

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GPXParserUnitTest {

    @Test
    public Unit testParseDateWithFractionalSeconds() throws ParseException {
        // was experienced in GSAK file
        val dateString: String = "2011-08-13T02:52:18.103Z"
        val parsedDate: Date = GPXParser.parseDate(dateString)
        assertThat(parsedDate).isNotNull()
    }

    @Test
    public Unit testParseDateWithHugeFraction() throws ParseException {
        // see issue 821
        val dateString: String = "2011-11-07T00:00:00.0000000-07:00"
        val parsedDate: Date = GPXParser.parseDate(dateString)
        assertThat(parsedDate).isNotNull()
    }

    @Test
    public Unit testParseDateWithoutTime() throws ParseException {
        // was experienced in GSAK file for DNF
        val expectedDate: GregorianCalendar = GregorianCalendar(2011, 8 - 1, 13)
        val dateString: String = "2011-08-13"
        val parsedDate: Date = GPXParser.parseDate(dateString)
        assertThat(parsedDate).isEqualTo(expectedDate.getTime())
    }
}
