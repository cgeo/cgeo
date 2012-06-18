package cgeo.geocaching.utils;

import android.test.AndroidTestCase;

public class AngleUtilsTest extends AndroidTestCase {

    public static void testNormalize() {
        assertEquals(0.0f, AngleUtils.normalize(0));
        assertEquals(0.0f, AngleUtils.normalize(360));
        assertEquals(0.0f, AngleUtils.normalize(720));
        assertEquals(0.0f, AngleUtils.normalize(-360));
        assertEquals(0.0f, AngleUtils.normalize(-720));
        assertEquals(1.0f, AngleUtils.normalize(721));
        assertEquals(359.0f, AngleUtils.normalize(-721));
    }

    public static void testDifference() {
        assertEquals(0.0f, AngleUtils.difference(12, 12));
        assertEquals(0.0f, AngleUtils.difference(372, 12));
        assertEquals(0.0f, AngleUtils.difference(12, 372));
        assertEquals(10.0f, AngleUtils.difference(10, 20));
        assertEquals(10.0f, AngleUtils.difference(355, 5));
        assertEquals(10.0f, AngleUtils.difference(715, -715));
        assertEquals(-10.0f, AngleUtils.difference(20, 10));
        assertEquals(-10.0f, AngleUtils.difference(5, 355));
        assertEquals(-10.0f, AngleUtils.difference(-715, 715));
        assertEquals(-180.0f, AngleUtils.difference(-90, 90));
        assertEquals(-180.0f, AngleUtils.difference(90, -90));
    }
}
