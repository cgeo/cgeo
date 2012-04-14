package cgeo.geocaching.geopoint;

import android.test.AndroidTestCase;

public class ViewportTest extends AndroidTestCase {

    final private static Viewport vpRef = new Viewport(-1.0, 3.0, -2.0, 4.0);

    public static void assertBounds(final Viewport vp) {
        assertEquals(new Geopoint(1.0, 1.0), vp.center);
        assertEquals(new Geopoint(3.0, 4.0), vp.topRight);
        assertEquals(new Geopoint(-1.0, -2.0), vp.bottomLeft);
    }

    public static void testCreationBounds() {
        assertBounds(new Viewport(new Geopoint(-1.0, -2.0), new Geopoint(3.0, 4.0)));
        assertBounds(new Viewport(new Geopoint(3.0, 4.0), new Geopoint(-1.0, -2.0)));
        assertBounds(new Viewport(new Geopoint(-1.0, 4.0), new Geopoint(3.0, -2.0)));
        assertBounds(new Viewport(new Geopoint(3.0, -2.0), new Geopoint(-1.0, 4.0)));
    }

    public static void testCreationCenter() {
        assertBounds(new Viewport(new Geopoint(1.0, 1.0), 4.0, 6.0));
    }

    public static void testCreationSeparate() {
        assertBounds(vpRef);
    }

    public static void testMinMax() {
        assertEquals(-1.0, vpRef.getLatitudeMin());
        assertEquals(3.0, vpRef.getLatitudeMax());
        assertEquals(-2.0, vpRef.getLongitudeMin());
        assertEquals(4.0, vpRef.getLongitudeMax());
    }

    public static void testSpans() {
        assertEquals(4.0, vpRef.getLatitudeSpan());
        assertEquals(6.0, vpRef.getLongitudeSpan());
    }

    public static void testInViewport() {
        assertFalse(vpRef.contains(new Geopoint(-2.0, -2.0)));
        assertFalse(vpRef.contains(new Geopoint(4.0, 4.0)));
        assertTrue(vpRef.contains(new Geopoint(0.0, 0.0)));
        assertTrue(vpRef.contains(new Geopoint(-1.0, -2.0)));
        assertTrue(vpRef.contains(new Geopoint(3.0, 4.0)));
    }

    public static void testSqlWhere() {
        assertEquals("latitude >= -1.0 and latitude <= 3.0 and longitude >= -2.0 and longitude <= 4.0", vpRef.sqlWhere());
    }

    public static void testEquals() {
        assertEquals(vpRef, vpRef);
        assertEquals(vpRef, new Viewport(vpRef.bottomLeft, vpRef.topRight));
        assertFalse(vpRef.equals(new Viewport(new Geopoint(0.0, 0.0), 1.0, 1.0)));
    }

    public static void testResize() {
        assertEquals(vpRef, vpRef.resize(1.0));
        assertEquals(new Viewport(new Geopoint(-3.0, -5.0), new Geopoint(5.0, 7.0)), vpRef.resize(2.0));
        assertEquals(new Viewport(new Geopoint(0.0, -0.5), new Geopoint(2.0, 2.5)), vpRef.resize(0.5));
    }

    public static void testIncludes() {
        assertTrue(vpRef.includes(vpRef));
        assertTrue(vpRef.includes(vpRef.resize(0.5)));
        assertFalse(vpRef.includes(vpRef.resize(2.0)));
    }

}
