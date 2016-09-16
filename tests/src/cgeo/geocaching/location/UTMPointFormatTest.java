package cgeo.geocaching.location;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test the UTMPoint parsing and formatting.
 */
public class UTMPointFormatTest {

    @Test
    public void testParseUTMStringSimple() {
        final UTMPoint utm = new UTMPoint("54S 293848 3915114");

        Assert.assertEquals(54, utm.getZoneNumber());
        Assert.assertEquals('S', utm.getZoneLetter());
        Assert.assertEquals(293848, utm.getEasting(), 1.1d);
        Assert.assertEquals(3915114, utm.getNorthing(), 1.1d);
    }

    @Test
    public void testParseUTMStringWithEandN() {
        final UTMPoint utm = new UTMPoint("54S E 293848 N 3915114");

        Assert.assertEquals(54, utm.getZoneNumber());
        Assert.assertEquals('S', utm.getZoneLetter());
        Assert.assertEquals(293848, utm.getEasting(), 1.1d);
        Assert.assertEquals(3915114, utm.getNorthing(), 1.1d);
    }

    @Test
    public void testParseUTMStringWithDecimals() {
        final UTMPoint utm = new UTMPoint("54S 293848.4 3915114.5");

        Assert.assertEquals(54, utm.getZoneNumber());
        Assert.assertEquals('S', utm.getZoneLetter());
        Assert.assertEquals(293848.4, utm.getEasting(), 1.1d);
        Assert.assertEquals(3915114.5, utm.getNorthing(), 1.1d);
    }

    @Test
    public void testParseUTMStringWithLowerCaseLetters() {
        final UTMPoint utm = new UTMPoint("54s e 293848 n 3915114");

        Assert.assertEquals(54, utm.getZoneNumber());
        Assert.assertEquals('S', utm.getZoneLetter());
        Assert.assertEquals(293848, utm.getEasting(), 1.1d);
        Assert.assertEquals(3915114, utm.getNorthing(), 1.1d);
    }

    @Test
    public void testParseUTMStringWithCommaAsDecimalSeparator() {
        final UTMPoint utm = new UTMPoint("54S 293848,4 3915114,5");

        Assert.assertEquals(54, utm.getZoneNumber());
        Assert.assertEquals('S', utm.getZoneLetter());
        Assert.assertEquals(293848.4, utm.getEasting(), 1.1d);
        Assert.assertEquals(3915114.5, utm.getNorthing(), 1.1d);
    }

    @Test
    public void testParseUTMStringWithBlankAfterZoneNumber() {
        final UTMPoint utm = new UTMPoint("54 S 293848 3915114");

        Assert.assertEquals(54, utm.getZoneNumber());
        Assert.assertEquals('S', utm.getZoneLetter());
        Assert.assertEquals(293848, utm.getEasting(), 1.1d);
        Assert.assertEquals(3915114, utm.getNorthing(), 1.1d);
    }

    @Test
    public void testParseUTMStringWithSingleDigitZoneNumber() {
        final UTMPoint utm = new UTMPoint("5S 293848 3915114");

        Assert.assertEquals(5, utm.getZoneNumber());
        Assert.assertEquals('S', utm.getZoneLetter());
        Assert.assertEquals(293848, utm.getEasting(), 1.1d);
        Assert.assertEquals(3915114, utm.getNorthing(), 1.1d);
    }

    @Test(expected = UTMPoint.ParseException.class)
    public void testParseUTMStringWithException() {
        new UTMPoint("5S blah blub");
    }

    @Test
    public void testToString() {
        Assert.assertEquals("54S E 293848 N 3915114", new UTMPoint(54, 'S', 293848, 3915114).toString());
    }

    @Test
    public void testToStringWithRoundedDecimals() {
        Assert.assertEquals("54S E 293848 N 3915114", new UTMPoint(54, 'S', 293847.5, 3915114.3).toString());
    }

}
