package cgeo.geocaching.location;

import android.test.AndroidTestCase;

public class DistanceParserTest extends AndroidTestCase {

    static private final double MM = 1e-6; // 1mm, in kilometers

    public static void testFormats() {
        assertEquals(1.2, DistanceParser.parseDistance("1200 m", true), MM);
        assertEquals(1.2, DistanceParser.parseDistance("1.2 km", true), MM);
        assertEquals(0.36576, DistanceParser.parseDistance("1200 ft", true), MM);
        assertEquals(1.09728, DistanceParser.parseDistance("1200 yd", true), MM);
        assertEquals(1.9312128, DistanceParser.parseDistance("1.2 mi", true), MM);
    }

    public static void testImplicit() {
        assertEquals(1.2, DistanceParser.parseDistance("1200", true), MM);
        assertEquals(0.36576, DistanceParser.parseDistance("1200", false), MM);
    }

    public static void testComma() {
        assertEquals(1.2, DistanceParser.parseDistance("1,2km", true), MM);
    }

    public static void testFeet() {
        assertEquals(0.36576, DistanceParser.parseDistance("1200 FT", true), MM);
    }

}