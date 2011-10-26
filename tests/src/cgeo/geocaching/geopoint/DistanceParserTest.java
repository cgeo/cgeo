package cgeo.geocaching.geopoint;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class DistanceParserTest extends AndroidTestCase {

	static private final double MM = 1e-6;  // 1mm, in kilometers

    public static void testFormats() {
        Assert.assertEquals(1.2, DistanceParser.parseDistance("1200 m", true), MM);
        Assert.assertEquals(1.2, DistanceParser.parseDistance("1.2 km", true), MM);
        Assert.assertEquals(0.36576, DistanceParser.parseDistance("1200 ft", true), MM);
        Assert.assertEquals(1.09728, DistanceParser.parseDistance("1200 yd", true), MM);
        Assert.assertEquals(1.9312128, DistanceParser.parseDistance("1.2 mi", true), MM);
	}

    public static void testImplicit() {
        Assert.assertEquals(1.2, DistanceParser.parseDistance("1200", true), MM);
        Assert.assertEquals(0.36576, DistanceParser.parseDistance("1200", false), MM);
	}

    public static void testComma() {
        Assert.assertEquals(1.2, DistanceParser.parseDistance("1,2km", true), MM);
	}

    public static void testCase() {
        Assert.assertEquals(0.36576, DistanceParser.parseDistance("1200 FT", true), MM);
	}

}