package cgeo.geocaching.location;

import org.junit.Test;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Test the UTMPoint parsing and formatting.
 */
public class UTMPointFormatTest {

    @Test
    public void testParseUTMStringSimple() {
        final UTMPoint utm = new UTMPoint("54S 293848 3915114");

        assertThat(utm.getZoneNumber()).isEqualTo(54);
        assertThat(utm.getZoneLetter()).isEqualTo('S');
        assertThat(utm.getEasting()).isEqualTo(293848, offset(1.1d));
        assertThat(utm.getNorthing()).isEqualTo(3915114, offset(1.1d));
    }

    @Test
    public void testParseUTMStringWithEandN() {
        final UTMPoint utm = new UTMPoint("54S E 293848 N 3915114");

        assertThat(utm.getZoneNumber()).isEqualTo(54);
        assertThat(utm.getZoneLetter()).isEqualTo('S');
        assertThat(utm.getEasting()).isEqualTo(293848, offset(1.1d));
        assertThat(utm.getNorthing()).isEqualTo(3915114, offset(1.1d));
    }

    @Test
    public void testParseUTMStringWithDecimals() {
        final UTMPoint utm = new UTMPoint("54S 293848.4 3915114.5");

        assertThat(utm.getZoneNumber()).isEqualTo(54);
        assertThat(utm.getZoneLetter()).isEqualTo('S');
        assertThat(utm.getEasting()).isEqualTo(293848.4, offset(1.1d));
        assertThat(utm.getNorthing()).isEqualTo(3915114.5, offset(1.1d));
    }

    @Test
    public void testParseUTMStringWithLowerCaseLetters() {
        final UTMPoint utm = new UTMPoint("54s e 293848 n 3915114");

        assertThat(utm.getZoneNumber()).isEqualTo(54);
        assertThat(utm.getZoneLetter()).isEqualTo('S');
        assertThat(utm.getEasting()).isEqualTo(293848, offset(1.1d));
        assertThat(utm.getNorthing()).isEqualTo(3915114, offset(1.1d));
    }

    @Test
    public void testParseUTMStringWithCommaAsDecimalSeparator() {
        final UTMPoint utm = new UTMPoint("54S 293848,4 3915114,5");

        assertThat(utm.getZoneNumber()).isEqualTo(54);
        assertThat(utm.getZoneLetter()).isEqualTo('S');
        assertThat(utm.getEasting()).isEqualTo(293848.4, offset(1.1d));
        assertThat(utm.getNorthing()).isEqualTo(3915114.5, offset(1.1d));
    }

    @Test
    public void testParseUTMStringWithBlankAfterZoneNumber() {
        final UTMPoint utm = new UTMPoint("54 S 293848 3915114");

        assertThat(utm.getZoneNumber()).isEqualTo(54);
        assertThat(utm.getZoneLetter()).isEqualTo('S');
        assertThat(utm.getEasting()).isEqualTo(293848, offset(1.1d));
        assertThat(utm.getNorthing()).isEqualTo(3915114, offset(1.1d));
    }

    @Test
    public void testParseUTMStringWithSingleDigitZoneNumber() {
        final UTMPoint utm = new UTMPoint("5S 293848 3915114");

        assertThat(utm.getZoneNumber()).isEqualTo(5);
        assertThat(utm.getZoneLetter()).isEqualTo('S');
        assertThat(utm.getEasting()).isEqualTo(293848, offset(1.1d));
        assertThat(utm.getNorthing()).isEqualTo(3915114, offset(1.1d));
    }

    @SuppressWarnings({"unused"})
    @Test(expected = UTMPoint.ParseException.class)
    public void testParseUTMStringWithException() {
        new UTMPoint("5S blah blub");
    }

    @Test
    public void testToString() {
        assertThat(new UTMPoint(54, 'S', 293848, 3915114).toString()).isEqualTo("54S E 293848 N 3915114");
    }

    @Test
    public void testToStringWithRoundedDecimals() {
        assertThat(new UTMPoint(54, 'S', 293847.5, 3915114.3).toString()).isEqualTo("54S E 293848 N 3915114");
    }

}
