package cgeo.geocaching.utils;

import junit.framework.TestCase;

public class AngleUtilsTest extends TestCase {

    public static void testNormalize() {
        assertEquals(AngleUtils.normalize(0), 0.0f, 0);
        assertEquals(AngleUtils.normalize(-0.0f), 0.0f, 0);
        assertEquals(AngleUtils.normalize(360), 0.0f, 0);
        assertEquals(AngleUtils.normalize(720), 0.0f, 0);
        assertEquals(AngleUtils.normalize(-360), 0.0f, 0);
        assertEquals(AngleUtils.normalize(-720), 0.0f, 0);
        assertEquals(AngleUtils.normalize(721), 1.0f, 0);
        assertEquals(AngleUtils.normalize(-721), 359.0f, 0);
        assertEquals(AngleUtils.normalize(-Float.MIN_VALUE), 0.0f, 0);
    }

    public static void testDifference() {
        assertEquals(AngleUtils.difference(12, 12), 0.0f, 0);
        assertEquals(AngleUtils.difference(372, 12), 0.0f, 0);
        assertEquals(AngleUtils.difference(12, 372), 0.0f, 0);
        assertEquals(AngleUtils.difference(10, 20), 10.0f, 0);
        assertEquals(AngleUtils.difference(355, 5), 10.0f, 0);
        assertEquals(AngleUtils.difference(715, -715), 10.0f, 0);
        assertEquals(AngleUtils.difference(20, 10), -10.0f, 0);
        assertEquals(AngleUtils.difference(5, 355), -10.0f, 0);
        assertEquals(AngleUtils.difference(-715, 715), -10.0f, 0);
        assertEquals(AngleUtils.difference(-90, 90), -180.0f, 0);
        assertEquals(AngleUtils.difference(90, -90), -180.0f, 0);
    }
}
