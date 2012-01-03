package cgeo.geocaching.geopoint;

import cgeo.geocaching.geopoint.GeopointParser.ParseException;

import android.test.AndroidTestCase;

public class GeoPointParserTest extends AndroidTestCase {

    private static final double refLongitude = 8.0 + 38.564 / 60.0;
    private static final double refLatitude = 49.0 + 56.031 / 60.0;

    public static void testParseLatitude() {
        assertEquals(refLatitude, GeopointParser.parseLatitude("N 49° 56.031"), 1e-8);
    }

    public static void testParseLongitude() {
        assertEquals(refLongitude, GeopointParser.parseLongitude("E 8° 38.564"), 1e-8);
    }

    public static void testFullCoordinates() {
        final Geopoint goal = new Geopoint(refLatitude, refLongitude);
        assertTrue(goal.isEqualTo(GeopointParser.parse("N 49° 56.031 | E 8° 38.564"), 1e-6));
    }

    public static void testCoordinateMissingPart() {
        // we are trying to parse a _point_, but have only one a latitude. Don't accept the numerical part as longitude!
        Geopoint point = null;
        try {
            point = GeopointParser.parse("N 49° 56.031");
        } catch (ParseException e) {
            // expected
        }
        assertEquals(null, point);
    }


    public static void testSouth() {
        assertEquals(-refLatitude, GeopointParser.parseLatitude("S 49° 56.031"), 1e-8);
    }

    public static void testWest() {
        assertEquals(-refLongitude, GeopointParser.parseLongitude("W 8° 38.564"), 1e-8);
    }

    public static void testLowerCase() {
        assertEquals(refLongitude, GeopointParser.parseLongitude("e 8° 38.564"), 1e-8);
    }

    public static void testVariousFormats() {
        final Geopoint goal1 = GeopointParser.parse("N 49° 43' 57\" | E 2 12' 35");
        final Geopoint goal2 = GeopointParser.parse("N 49 43.95 E2°12.5833333333");
        assertTrue(goal1.isEqualTo(goal2, 1e-6));
    }

    public static void testInSentence() {
        final Geopoint p1 = GeopointParser.parse("Station3: N51 21.523 / E07 02.680");
        final Geopoint p2 = GeopointParser.parse("N51 21.523", "E07 02.680");
        assertNotNull(p1);
        assertNotNull(p2);
        assertTrue(p1.isEqualTo(p2));
    }

    public static void testUnrelatedParts() {
        Geopoint point = null;
        try {
            point = GeopointParser.parse("N51 21.523 and some words in between, so there is no relation E07 02.680");
        } catch (ParseException e) {
            // expected
        }
        assertEquals(null, point);
    }
}
