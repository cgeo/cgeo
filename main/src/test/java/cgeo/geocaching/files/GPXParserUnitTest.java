package cgeo.geocaching.files;

import java.text.ParseException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GPXParserUnitTest {

    @Test
    public void testParseDateWithFractionalSeconds() throws ParseException {
        // was experienced in GSAK file
        final String dateString = "2011-08-13T02:52:18.103Z";
        final Date parsedDate = GPXParser.parseDate(dateString);
        assertThat(parsedDate).isNotNull();
    }

    @Test
    public void testParseDateWithHugeFraction() throws ParseException {
        // see issue 821
        final String dateString = "2011-11-07T00:00:00.0000000-07:00";
        final Date parsedDate = GPXParser.parseDate(dateString);
        assertThat(parsedDate).isNotNull();
    }

    @Test
    public void testParseDateWithoutTime() throws ParseException {
        // was experienced in GSAK file for DNF
        final GregorianCalendar expectedDate = new GregorianCalendar(2011, 8 - 1, 13);
        final String dateString = "2011-08-13";
        final Date parsedDate = GPXParser.parseDate(dateString);
        assertThat(parsedDate).isEqualTo(expectedDate.getTime());
    }
}
