package cgeo.geocaching.test;

import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointParser;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class GeoPointParserTest extends AndroidTestCase {

    private static final double refLongitude = 8.0 + 38.564 / 60.0;
    private static final double refLatitude = 49.0 + 56.031 / 60.0;

    public void testParseLatitude() {

        Assert.assertEquals(refLatitude, GeopointParser.parseLatitude("N 49° 56.031"), 1e-8);
    }

    public void testParseLongitude() {

        Assert.assertEquals(refLongitude, GeopointParser.parseLongitude("E 8° 38.564"), 1e-8);
    }

    public void testFullCoordinates() {
        final Geopoint goal = new Geopoint(refLatitude, refLongitude);
        Assert.assertTrue(goal.isEqualTo(GeopointParser.parse("N 49° 56.031 | E 8° 38.564"), 1e-6));
    }

    public void testSouth() {
        Assert.assertEquals(-refLatitude, GeopointParser.parseLatitude("S 49° 56.031"), 1e-8);
    }

    public void testWest() {
        Assert.assertEquals(-refLongitude, GeopointParser.parseLongitude("W 8° 38.564"), 1e-8);
    }

    public void testLowerCase() {
        Assert.assertEquals(refLongitude, GeopointParser.parseLongitude("e 8° 38.564"), 1e-8);
    }

    public void testVariousFormats() {
        final Geopoint goal1 = GeopointParser.parse("N 49° 43' 57\" | E 2 12' 35");
        final Geopoint goal2 = GeopointParser.parse("N 49 43.95 E2°12.5833333333");
        Assert.assertTrue(goal1.isEqualTo(goal2, 1e-6));
    }
}
