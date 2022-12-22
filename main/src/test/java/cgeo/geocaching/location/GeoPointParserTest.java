package cgeo.geocaching.location;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.within;

public class GeoPointParserTest {

    private static final double REF_LONGITUDE = 8.0 + 38.564 / 60.0;
    private static final double REF_LATITUDE = 49.0 + 56.031 / 60.0;

    @Test
    public void testParseLatitude() {
        assertThat(GeopointParser.parseLatitude("N 49° 56.031")).isCloseTo(REF_LATITUDE, within(1e-8));
    }

    @Test
    public void testParseLongitude() {
        assertThat(GeopointParser.parseLongitude("E 8° 38.564")).isCloseTo(REF_LONGITUDE, within(1e-8));
    }

    @Test
    public void testFullCoordinates() {
        final Geopoint goal = new Geopoint(REF_LATITUDE, REF_LONGITUDE);
        assertGeopointEquals(goal, GeopointParser.parse("N 49° 56.031 | E 8° 38.564"), 1e-6f);
    }

    private static void assertGeopointEquals(final Geopoint expected, final Geopoint actual, final float tolerance) {
        assertThat(expected).isNotNull();
        assertThat(actual).isNotNull();
        assertThat(expected.distanceTo(actual)).isLessThanOrEqualTo(tolerance);
    }

    private static void assertParsingFails(final String geopointToParse) {
        try {
            GeopointParser.parse(geopointToParse);
            failBecauseExceptionWasNotThrown(Geopoint.ParseException.class);
        } catch (final Geopoint.ParseException e) {
            // expected
        }
    }

    @Test
    public void testCoordinateMissingPart() {
        // we are trying to parse a _point_, but have only a latitude. Don't accept the numerical part as longitude!
        assertParsingFails("N 49° 56.031");
    }

    @Test
    public void testCoordinateMissingDegree() {
        // Some home coordinates on geocaching.com lack the degree part.
        final Geopoint point = GeopointParser.parse("N 51° 23.123' W ° 17.123");
        assertThat(point).isEqualTo(new Geopoint("N", "51", "23", "123", "W", "0", "17", "123"));
    }

    @Test
    public void testSouth() {
        assertThat(GeopointParser.parseLatitude("S 49° 56.031")).isEqualTo(-REF_LATITUDE, offset(1e-8));
    }

    @Test
    public void testWest() {
        assertThat(GeopointParser.parseLongitude("W 8° 38.564")).isEqualTo(-REF_LONGITUDE, offset(1e-8));
    }

    @Test
    public void testLowerCase() {
        assertThat(GeopointParser.parseLongitude("e 8° 38.564")).isEqualTo(REF_LONGITUDE, offset(1e-8));
    }

    @Test
    public void testVariousFormats() {
        final Geopoint goal1 = GeopointParser.parse("N 49° 43' 57\" | E 2 12' 35");
        final Geopoint goal2 = GeopointParser.parse("N 49 43.95 E2°12.5833333333");
        assertGeopointEquals(goal1, goal2, 1e-6f);
    }

    @Test
    public void testParseOurOwnSeparator() {
        final Geopoint separator = GeopointParser.parse("N 49° 43' 57\" · E 2 12' 35");
        final Geopoint noSeparator = GeopointParser.parse("N 49 43.95 E2°12.5833333333");
        assertGeopointEquals(separator, noSeparator, 1e-6f);
    }

    @Test
    public void testInSentence() {
        final Geopoint p1 = GeopointParser.parse("Station3: N51 21.523 / E07 02.680");
        final Geopoint p2 = GeopointParser.parse("N51 21.523 E07 02.680");
        assertThat(p1).isNotNull();
        assertThat(p2).isNotNull();
        assertThat(p2).isEqualTo(p1);
    }

    @Test
    public void testUnrelatedParts() {
        assertParsingFails("N51 21.523 and some words in between, so there is no relation E07 02.680");
    }

    @Test
    public void testComma() {
        final Geopoint pointComma = GeopointParser.parse("N 46° 27' 55,65''\n" +
                "E 15° 53' 41,68''");
        final Geopoint pointDot = GeopointParser.parse("N 46° 27' 55.65''\n" +
                "E 15° 53' 41.68''");
        assertThat(pointComma).isNotNull();
        assertThat(pointDot).isNotNull();
        assertThat(pointDot).isEqualTo(pointComma);
    }

    @Test
    public void testBlankAddedByAutocorrectionDot() {
        assertThat(GeopointParser.parseLatitude("N 49° 56. 031")).isEqualTo(REF_LATITUDE, offset(1e-8));
    }

    @Test
    public void testBlankAddedByAutocorrectionComma() {
        assertThat(GeopointParser.parseLatitude("N 49° 56, 031")).isEqualTo(REF_LATITUDE, offset(1e-8));
    }

    @Test
    public void testNonTrimmed() {
        assertThat(GeopointParser.parseLatitude("    N 49° 56, 031   ")).isEqualTo(REF_LATITUDE, offset(1e-8));
    }

    @Test
    public void testEquatorGC53() {
        assertThat(GeopointParser.parse("00° 00.000 E 036° 00.000")).isEqualTo(new Geopoint(0, 36));
    }

    @Test
    public void testMeridian() {
        assertThat(GeopointParser.parse("N 23° 00.000 00° 00.000")).isEqualTo(new Geopoint(23, 0));
    }

    @Test
    public void testEquatorMeridian() {
        assertThat(GeopointParser.parse("00° 00.000 00° 00.000")).isEqualTo(Geopoint.ZERO);
    }

    @Test
    public void testFloatingPointLatitude() {
        assertThat(GeopointParser.parseLatitude("47.648883")).isEqualTo(GeopointParser.parseLatitude("N 47° 38.933"), offset(1e-6));
    }

    @Test
    public void testFloatingPointNegativeLatitudeMeansSouth() {
        assertThat(GeopointParser.parseLatitude("-47.648883")).isEqualTo(GeopointParser.parseLatitude("S 47° 38.933"), offset(1e-6));
    }

    @Test
    public void testFloatingPointBoth() {
        assertGeopointEquals(GeopointParser.parse("47.648883  122.348067"), GeopointParser.parse("N 47° 38.933 E 122° 20.884"), 1e-4f);
        assertGeopointEquals(GeopointParser.parse("47.648883  -122.348067"), GeopointParser.parse("N 47° 38.933 W 122° 20.884"), 1e-4f);
    }

    @Test
    public void testFloatingPointNbsp() {
        assertGeopointEquals(GeopointParser.parse("47.648883  122.348067\u00a0"), GeopointParser.parse("N 47° 38.933 E 122° 20.884"), 1e-4f);
    }

    @Test
    public void testGerman() {
        assertThat(GeopointParser.parse("N 47° 38.933 O 122° 20.884")).isEqualTo(GeopointParser.parse("N 47° 38.933 E 122° 20.884"));
    }

    @Test
    public void testNoSpace() {
        assertThat(GeopointParser.parse("N47°38.933E122°20.884")).isEqualTo(GeopointParser.parse("N 47° 38.933 E 122° 20.884"));
    }

    @Test
    public void testSpace() {
        final Geopoint referencePoint = GeopointParser.parse("N 47° 38.933 E 122° 20.884");
        assertGeopointEquals(GeopointParser.parse("47. 648883 122.348067"), referencePoint, 1e-4f);
        assertGeopointEquals(GeopointParser.parse("47.648883   122. 348067"), referencePoint, 1e-4f);
        assertGeopointEquals(GeopointParser.parse("47. 648883   122. 348067"), referencePoint, 1e-4f);
        assertGeopointEquals(GeopointParser.parse("N 47° 38. 933   E 122° 20. 884"), referencePoint, 1e-4f);
        assertGeopointEquals(GeopointParser.parse("N  47 38. 933  E  122 20. 884"), referencePoint, 1e-4f);
        assertParsingFails("N  47 38.   933  E  122 20.   884");
    }

    @Test
    public void testUTM() {
        GeopointParser.parse("54S E 293848 N 3915114");
    }

    @Test
    public void testZero() {
        GeopointParser.parse("00° 00.000′ 000° 00.00′");
        GeopointParser.parse("00° 00′ 00.00″ 000° 00′ 00.00″");
        GeopointParser.parse("00° 00′ 000° 00′");
        GeopointParser.parse("00° E 000°");
        assertParsingFails("00° 00.001′ 000° 00.01′");
        assertParsingFails("00° 00′ 00.01″ 000° 00′ 00.01″");
        assertParsingFails("00° 01′ 000° 01′");
        assertParsingFails("01° E 000°");
    }

    @Test
    public void testFormula() {
        assertParsingFails("N 12° 23.345′ E 123° 34.5AB′");
        assertParsingFails("N 12° 23′ E 123° 3A′");
        assertParsingFails("N 12° E 12A°");
        assertParsingFails("N 12° 23′ 34″ E 123° 34′ 5A″");
        assertParsingFails("N 12° 23′ 34.56″ E 123° 34′ 56.7A″");
        assertParsingFails("-12.345678° 23.4ABCDE°");
    }

    @Test
    public void testInvalidCombinations() {
        assertParsingFails("N 07° 59.999′ W 059° 42′ 17.12″");
        assertParsingFails("S 59° 42′ 17.12″ -0.497234");
        assertParsingFails("0.497234 E 007° 59.999′");
    }

    @Test
    public void testDMSBounds() {
        GeopointParser.parse("S 90° 00′ 00.00″ W 180° 00′ 00.00″");
        GeopointParser.parse("S 89° 59′ 59.99″ W 179° 59′ 59.99″");
        assertParsingFails("S 90° 00′ 00.01″ W 180° 00′ 00.00″");
        assertParsingFails("S 90° 00′ 00.00″ W 180° 00′ 00.01″");
        assertParsingFails("S 89° 59′ 60.00″ W 179° 59′ 59.99″");
        assertParsingFails("S 89° 59′ 59.99″ W 179° 59′ 60.00″");
        assertParsingFails("S 89° 60′ 00.00″ W 179° 59′ 59.99″");
        assertParsingFails("S 89° 59′ 59.99″ W 179° 60′ 00.00″");
    }

    @Test
    public void testMinBounds() {
        GeopointParser.parse("S 90° 00′ W 180° 00′");
        GeopointParser.parse("S 89° 59′ W 179° 59′");
        assertParsingFails("S 90° 01′ W 180° 00′");
        assertParsingFails("S 90° 00′ W 180° 01′");
        assertParsingFails("S 89° 60′ W 180° 00′");
        assertParsingFails("S 90° 00′ W 179° 60′");
    }

    @Test
    public void testMinDecBounds() {
        GeopointParser.parse("S 90° 00.000′ W 180° 00.000′");
        GeopointParser.parse("S 89° 59.999′ W 179° 59.999′");
        assertParsingFails("S 90° 00.001′ W 180° 00.000′");
        assertParsingFails("S 90° 00.000′ W 180° 00.001′");
        assertParsingFails("S 89° 60.000′ W 180° 00.000′");
        assertParsingFails("S 90° 00.000′ W 179° 60.000′");
    }

    @Test
    public void testNull() {
        try {
            GeopointParser.parseLatitude(null);
            failBecauseExceptionWasNotThrown(Geopoint.ParseException.class);
        } catch (final Geopoint.ParseException e) {
            // expected
        }
        try {
            GeopointParser.parseLongitude(null);
            failBecauseExceptionWasNotThrown(Geopoint.ParseException.class);
        } catch (final Geopoint.ParseException e) {
            // expected
        }
    }

    @Test
    public void test922() {
        assertParsingFails("L 12\n M 13\n N 14\n O 15");
    }

    @Test
    public void test5538() {
        assertParsingFails("A=6 B=0 C=5 D=4 E=13\n" +
                "N 48° 53.(A*C*E)+194   E 009° 11.((D*71)-0)-1\n" +
                "N 48° 53.(6*5*13)+194  E 009° 11.((4*71)-0)-1\n" +
                "N 48° 53.(390)+194     E 009° 11.((284)-0)-1");
        assertParsingFails("S1: N 49 27.253");
    }

    @Test
    public void test5790() {
        GeopointParser.parse("N 52° 36.123 E 010° 06.456'");
        GeopointParser.parse("N52° 36.123 E010°06.456");
        GeopointParser.parse("N52 36.123 E010 06.456");
        GeopointParser.parse("52° 10°");
        GeopointParser.parse("52° -10°");
        GeopointParser.parse("52,55123 10,56789");
        GeopointParser.parse("52.55123° 10.56789°");
    }

    @Test
    public void test6090() {
        // Issue #6090
        final Geopoint ref = new Geopoint(12.576117, -1.390933);

        final Geopoint gp1 = GeopointParser.parse("N12 34. 567\nW001 23.456");
        assertGeopointEquals(gp1, ref, 1e-4f);

        final Geopoint gp2 = GeopointParser.parse("N12 34.567\nW001 23. 456");
        assertGeopointEquals(gp2, ref, 1e-4f);

        final Geopoint gp3 = GeopointParser.parse("N12 34. 567\nW001 23. 456");
        assertGeopointEquals(gp3, ref, 1e-4f);
    }

    @Test
    public void test6802() {
        try {
            GeopointParser.parseLatitude("N 1.1.1.1.1.1.1");
            failBecauseExceptionWasNotThrown(Geopoint.ParseException.class);
        } catch (final Geopoint.ParseException e) {
            // expected
        }
        try {
            GeopointParser.parseLongitude("E 99?++?9.93@#$%&-+777");
            failBecauseExceptionWasNotThrown(Geopoint.ParseException.class);
        } catch (final Geopoint.ParseException e) {
            // expected
        }
    }

    @Test
    public void test8078() {
        assertParsingFails("2.2.3.8");
    }

    @Test
    public void test8589() {
        assertParsingFails("6, 12, 16, 29");
    }

    @Test
    public void test8845() {
        final Geopoint ref = new Geopoint("N", "49", "56", "31", "E", "8", "38", "564");
        final Geopoint point = GeopointParser.parse("N49° 56.031', E08° 38.564'");
        assertThat(point).isEqualTo(ref);
    }

    @Test
    public void parseMultipleCoordinatesWithCorrectStartEndPositions() {
        final String initalText = "@n1 (W) N48 01.194 E011 43.814\n" +
                "@n2 (W) N48 01.194 E011 43.814";
        final Collection<GeopointWrapper> parsed = GeopointParser.parseAll(initalText);
        assertThat(parsed.size()).isEqualTo(2);
        final Iterator<GeopointWrapper> it = parsed.iterator();
        assertGeopointWrapper(it.next(), new Geopoint("N 48° 01.194' · E 011° 43.814'"), 8, 30, initalText);
        assertGeopointWrapper(it.next(), new Geopoint("N 48° 01.194' · E 011° 43.814'"), 9, 31, initalText.substring(30));
    }

    @Test
    public void parseMultipleCoordinatesWithDotWithCorrectStartEndPositions() {
        final String initalText = "@n1 (W) N 48° 01.194' · E 011° 43.814'\n" +
                "@n2 (W) N 48° 01.194' · E 011° 43.814'";
        final Collection<GeopointWrapper> parsed = GeopointParser.parseAll(initalText);
        assertThat(parsed.size()).isEqualTo(2);
        final Iterator<GeopointWrapper> it = parsed.iterator();
        assertGeopointWrapper(it.next(), new Geopoint("N 48° 01.194' · E 011° 43.814'"), 8, 38, initalText);
        assertGeopointWrapper(it.next(), new Geopoint("N 48° 01.194' · E 011° 43.814'"), 9, 39, initalText.substring(38));
    }

    @Test
    public void parseMultipleCoordinatesWithoutSpaceWithCorrectStartEndPositions() {
        final String initalText = "N48 01.194 E011 43.814 " +
                "N58 01.194 E011 43.814";
        final Collection<GeopointWrapper> parsed = GeopointParser.parseAll(initalText);
        assertThat(parsed.size()).isEqualTo(2);
        final Iterator<GeopointWrapper> it = parsed.iterator();
        assertGeopointWrapper(it.next(), new Geopoint("N 48° 01.194' · E 011° 43.814'"), 0, 22, initalText);
        assertGeopointWrapper(it.next(), new Geopoint("N 58° 01.194' · E 011° 43.814'"), 1, 23, initalText.substring(22));
    }


    @Test
    public void parseMultipleCoordinatesWithSpaceAfterDotWithCorrectStartEndPositions() {
        final String initalText = "N48 01. 194 E011 43. 814 " +
                "N58 01. 194 E011 43. 814";
        final String parsedText = "N48 01.194 E011 43.814 " +
                "N58 01.194 E011 43.814";
        final Collection<GeopointWrapper> parsed = GeopointParser.parseAll(initalText);
        assertThat(parsed.size()).isEqualTo(2);
        final Iterator<GeopointWrapper> it = parsed.iterator();
        assertGeopointWrapper(it.next(), new Geopoint("N 48° 01.194' · E 011° 43.814'"), 0, 22, parsedText);
        assertGeopointWrapper(it.next(), new Geopoint("N 58° 01.194' · E 011° 43.814'"), 1, 23, parsedText.substring(22));
    }

    private static void assertGeopointWrapper(final GeopointWrapper match, final Geopoint gp, final int start, final int end, final String text) {
        assertThat(match.getGeopoint()).isEqualTo(gp);
        assertThat(match.getStart()).isEqualTo(start);
        assertThat(match.getEnd()).isEqualTo(end);
        if (text != null && !text.isEmpty()) {
            assertThat(match.getText()).isEqualTo(text);
        }
    }
}
