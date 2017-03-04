package cgeo.geocaching.location;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import junit.framework.TestCase;

public class GeoPointParserTest extends TestCase {

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
        assertGeopointEquals(goal, GeopointParser.parse("N 49° 56.031 | E 8° 38.564"), 1e-6f);
    }

    private static void assertGeopointEquals(final Geopoint expected, final Geopoint actual, final float tolerance) {
        assertThat(expected).isNotNull();
        assertThat(actual).isNotNull();
        assertThat(expected.distanceTo(actual)).isLessThanOrEqualTo(tolerance);
    }

    private assertParsingFails(final String geopointToParse) {
        try {
            GeopointParser.parse(geopointToParse);
            fail();
        } catch (final Geopoint.ParseException e) {
            // expected
        }
    }

    public static void testCoordinateMissingPart() {
        // we are trying to parse a _point_, but have only a latitude. Don't accept the numerical part as longitude!
        assertParsingFails("N 49° 56.031");
    }

    public static void testCoordinateMissingDegree() {
        // Some home coordinates on geocaching.com lack the degree part.
        final Geopoint point = GeopointParser.parse("N 51° 23.123' W ° 17.123");
        assertThat(point).isEqualTo(new Geopoint("N", "51", "23", "123", "W", "0", "17", "123"));
    }

    public static void testSouth() {
        assertThat(GeopointParser.parseLatitude("S 49° 56.031")).isEqualTo(-refLatitude, offset(1e-8));
    }

    public static void testWest() {
        assertThat(GeopointParser.parseLongitude("W 8° 38.564")).isEqualTo(-refLongitude, offset(1e-8));
    }

    public static void testLowerCase() {
        assertThat(GeopointParser.parseLongitude("e 8° 38.564")).isEqualTo(refLongitude, offset(1e-8));
    }

    public static void testVariousFormats() {
        final Geopoint goal1 = GeopointParser.parse("N 49° 43' 57\" | E 2 12' 35");
        final Geopoint goal2 = GeopointParser.parse("N 49 43.95 E2°12.5833333333");
        assertGeopointEquals(goal1, goal2, 1e-6f);
    }

    public static void testParseOurOwnSeparator() {
        final Geopoint separator = GeopointParser.parse("N 49° 43' 57\" · E 2 12' 35");
        final Geopoint noSeparator = GeopointParser.parse("N 49 43.95 E2°12.5833333333");
        assertGeopointEquals(separator, noSeparator, 1e-6f);
    }

    public static void testInSentence() {
        final Geopoint p1 = GeopointParser.parse("Station3: N51 21.523 / E07 02.680");
        final Geopoint p2 = GeopointParser.parse("N51 21.523 E07 02.680");
        assertThat(p1).isNotNull();
        assertThat(p2).isNotNull();
        assertThat(p2).isEqualTo(p1);
    }

    public static void testUnrelatedParts() {
        assertParsingFails("N51 21.523 and some words in between, so there is no relation E07 02.680");
    }

    public static void testComma() {
        final Geopoint pointComma = GeopointParser.parse("N 46° 27' 55,65''\n" +
                "E 15° 53' 41,68''");
        final Geopoint pointDot = GeopointParser.parse("N 46° 27' 55.65''\n" +
                "E 15° 53' 41.68''");
        assertThat(pointComma).isNotNull();
        assertThat(pointDot).isNotNull();
        assertThat(pointDot).isEqualTo(pointComma);
    }

    public static void testBlankAddedByAutocorrectionDot() {
        assertThat(GeopointParser.parseLatitude("N 49° 56. 031")).isEqualTo(refLatitude, offset(1e-8));
    }

    public static void testBlankAddedByAutocorrectionComma() {
        assertThat(GeopointParser.parseLatitude("N 49° 56, 031")).isEqualTo(refLatitude, offset(1e-8));
    }

    public static void testNonTrimmed() {
        assertThat(GeopointParser.parseLatitude("    N 49° 56, 031   ")).isEqualTo(refLatitude, offset(1e-8));
    }

    public static void testEquatorGC53() {
        assertThat(GeopointParser.parse("00° 00.000 E 036° 00.000")).isEqualTo(new Geopoint(0, 36));
    }

    public static void testMeridian() {
        assertThat(GeopointParser.parse("N 23° 00.000 00° 00.000")).isEqualTo(new Geopoint(23, 0));
    }

    public static void testEquatorMeridian() {
        assertThat(GeopointParser.parse("00° 00.000 00° 00.000")).isEqualTo(Geopoint.ZERO);
    }

    public static void testFloatingPointLatitude() {
        assertThat(GeopointParser.parseLatitude("47.648883")).isEqualTo(GeopointParser.parseLatitude("N 47° 38.933"), offset(1e-6));
    }

    public static void testFloatingPointNegativeLatitudeMeansSouth() {
        assertThat(GeopointParser.parseLatitude("-47.648883")).isEqualTo(GeopointParser.parseLatitude("S 47° 38.933"), offset(1e-6));
    }

    public static void testFloatingPointBoth() {
        assertGeopointEquals(GeopointParser.parse("47.648883  122.348067"), GeopointParser.parse("N 47° 38.933 E 122° 20.884"), 1e-4f);
        assertGeopointEquals(GeopointParser.parse("47.648883  -122.348067"), GeopointParser.parse("N 47° 38.933 W 122° 20.884"), 1e-4f);
    }

    public static void testFloatingPointNbsp() {
        assertGeopointEquals(GeopointParser.parse("47.648883  122.348067\u00a0"), GeopointParser.parse("N 47° 38.933 E 122° 20.884"), 1e-4f);
    }

    public static void testDoubleComma() {
        assertThat(GeopointParser.parse("47.648883,122.348067").equals(GeopointParser.parse("47.648883 122.348067")));
    }

    public static void testGerman() {
        assertThat(GeopointParser.parse("N 47° 38.933 O 122° 20.884")).isEqualTo(GeopointParser.parse("N 47° 38.933 E 122° 20.884"));
    }

    public static void testNoSpace() {
        assertThat(GeopointParser.parse("N47°38.933E122°20.884")).isEqualTo(GeopointParser.parse("N 47° 38.933 E 122° 20.884"));
    }

    public static void testUTM() {
        GeopointParser.parse("54S E 293848 N 3915114");
    }

    public static void testZero() {
        GeopointParser.parse("00° 00.000′ 000° 00.00′");
        GeopointParser.parse("00° 00′ 00.00″ 000° 00′ 00.00″");
        GeopointParser.parse("00° 00′ 000° 00′");
        GeopointParser.parse("00° E 000°");
        assertParsingFails("00° 00.001′ 000° 00.01′");
        assertParsingFails("00° 00′ 00.01″ 000° 00′ 00.01″");
        assertParsingFails("00° 01′ 000° 01′");
        assertParsingFails("01° E 000°");
    }

    public static void testFormula() {
        assertParsingFails("N 12° 23.345′ E 123° 34.5AB′");
        assertParsingFails("N 12° 23′ E 123° 3A′");
        assertParsingFails("N 12° E 12A°");
        assertParsingFails("N 12° 23′ 34″ E 123° 34′ 5A″");
        assertParsingFails("N 12° 23′ 34.56″ E 123° 34′ 56.7A″");
        assertParsingFails("-12.345678° 23.4ABCDE°");
    }

    public static void testInvalidCombinations() {
        assertParsingFails("N 07° 59.999′ W 059° 42′ 17.12″");
        assertParsingFails("S 59° 42′ 17.12″ -0.497234");
        assertParsingFails("0.497234 E 007° 59.999′");
    }

    public static void testDMSBounds() {
        GeopointParser.parse("S 90° 00′ 00.00″ W 180° 00′ 00.00″");
        GeopointParser.parse("S 89° 59′ 59.99″ W 179° 59′ 59.99″");
        assertParsingFails("S 90° 00′ 00.01″ W 180° 00′ 00.00″");
        assertParsingFails("S 90° 00′ 00.00″ W 180° 00′ 00.01″");
        assertParsingFails("S 89° 59′ 60.00″ W 179° 59′ 59.99″");
        assertParsingFails("S 89° 59′ 59.99″ W 179° 59′ 60.00″");
        assertParsingFails("S 89° 60′ 00.00″ W 179° 59′ 59.99″");
        assertParsingFails("S 89° 59′ 59.99″ W 179° 60′ 00.00″");
    }

    public static void testMinBounds() {
        GeopointParser.parse("S 90° 00′ W 180° 00′");
        GeopointParser.parse("S 89° 59′ W 179° 59′");
        assertParsingFails("S 90° 01′ W 180° 00′");
        assertParsingFails("S 90° 00′ W 180° 01′");
        assertParsingFails("S 89° 60′ W 180° 00′");
        assertParsingFails("S 90° 00′ W 179° 60′");
    }

    public static void testMinDecBounds() {
        GeopointParser.parse("S 90° 00.000′ W 180° 00.000′");
        GeopointParser.parse("S 89° 59.999′ W 179° 59.999′");
        assertParsingFails("S 90° 00.001′ W 180° 00.000′");
        assertParsingFails("S 90° 00.000′ W 180° 00.001′");
        assertParsingFails("S 89° 60.000′ W 180° 00.000′");
        assertParsingFails("S 90° 00.000′ W 179° 60.000′");
    }

    public static void testNull() {
        try {
            GeopointParser.parseLatitude(null);
            fail();
        } catch (final Geopoint.ParseException e) {
            // expected
        }
        try {
            GeopointParser.parseLongitude(null);
            fail();
        } catch (final Geopoint.ParseException e) {
            // expected
        }
    }

    public static void test922() {
        assertParsingFails("L 12\n M 13\n N 14\n O 15");
    }

    public static void test5538() {
        assertParsingFails("A=6 B=0 C=5 D=4 E=13\n" +
                           "N 48° 53.(A*C*E)+194   E 009° 11.((D*71)-0)-1\n" +
                           "N 48° 53.(6*5*13)+194  E 009° 11.((4*71)-0)-1\n" +
                           "N 48° 53.(390)+194     E 009° 11.((284)-0)-1");
        assertParsingFails("S1: N 49 27.253");
    }

    public static void test5790() {
        GeopointParser.parse("N 52° 36.123 E 010° 06.456'");
        GeopointParser.parse("N52° 36.123 E010°06.456");
        GeopointParser.parse("N52 36.123 E010 06.456");
        GeopointParser.parse("52° 10°");
        GeopointParser.parse("52° -10°");
        GeopointParser.parse("52.55123 10,56789");
        GeopointParser.parse("52.55123° 10.56789°");
    }

    public static void test6090() {
        // Issue #6090
        final Geopoint ref = new Geopoint(12.576117, -1.390933);

        final Geopoint gp1 = GeopointParser.parse("N12 34. 567\nW001 23.456");
        assertGeopointEquals(gp1, ref, 1e-4f);

        final Geopoint gp2 = GeopointParser.parse("N12 34.567\nW001 23. 456");
        assertGeopointEquals(gp2, ref, 1e-4f);

        final Geopoint gp3 = GeopointParser.parse("N12 34. 567\nW001 23. 456");
        assertGeopointEquals(gp3, ref, 1e-4f);
    }

}
