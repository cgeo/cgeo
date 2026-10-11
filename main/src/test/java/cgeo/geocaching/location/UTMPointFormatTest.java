package cgeo.geocaching.location;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

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

    @Test
    public void testParseSingaporeUTMStringRoundTrip() {
        assertEquatorialRoundTrip("48N E 366494 N 143721", 1.3d, 103.8d);
    }

    @Test
    public void testParseKualaLumpurUTMStringRoundTrip() {
        assertEquatorialRoundTrip("47N E 801396 N 349434", 3.15785d, 101.71165d);
    }

    @Test(expected = UTMPoint.ParseException.class)
    public void testParseUTMStringRejectsSmallNorthingOutsideEquatorialBand() {
        new UTMPoint("48P E 366494 N 143721");
    }

    private static void assertEquatorialRoundTrip(final String utmString, final double latitude, final double longitude) {
        final UTMPoint parsed = new UTMPoint(utmString);
        final Geopoint geopoint = parsed.toLatLong();

        assertThat(geopoint.getLatitude()).isEqualTo(latitude, offset(1e-5d));
        assertThat(geopoint.getLongitude()).isEqualTo(longitude, offset(1e-5d));
        assertThat(UTMPoint.latLong2UTM(geopoint).toString()).isEqualTo(utmString);
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
