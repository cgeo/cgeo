package cgeo.geocaching.geopoint;

import android.test.AndroidTestCase;

import junit.framework.Assert;

public class GeopointTest extends AndroidTestCase {

    public static void testCreation() {
        final Geopoint gp = new Geopoint(48.2, 3.5);
        Assert.assertEquals(48.2, gp.getLatitude(), 1e-8);
        Assert.assertEquals(3.5, gp.getLongitude(), 1e-8);
    }

    public static void testCreationWithParsing() {
        final Geopoint gp = new Geopoint("N 52° 25,111 E 009° 39,111");
        Assert.assertEquals(52.41852, gp.getLatitude(), 1e-4);
        Assert.assertEquals(9.65185, gp.getLongitude(), 1e-4);
    }

    public static void testCreationAtLimit() {
        // No exception should be raised.
        final Geopoint gp1 = new Geopoint(90.0, 10.0);
        Assert.assertEquals(90, gp1.getLatitude(), 1e-8);

        final Geopoint gp2 = new Geopoint(-90.0, 10.0);
        Assert.assertEquals(-90, gp2.getLatitude(), 1e-8);

        final Geopoint gp3 = new Geopoint(10.0, 180.0);
        Assert.assertEquals(180, gp3.getLongitude(), 1e-8);
    }

    public static void testEqual() {
        final Geopoint gp1 = new Geopoint(48.2, 2.31);
        Assert.assertTrue(gp1.equals(gp1));
        final Geopoint gp2 = new Geopoint(48.3, 2.31);
        Assert.assertFalse(gp1.equals(gp2));
    }

    public static void testCreateE6() {
        final Geopoint gp1 = new Geopoint(48.2, 2.34);
        final Geopoint gp2 = new Geopoint(48200000, 2340000);
        Assert.assertTrue(gp1.isEqualTo(gp2, 1e-6));
    }

    public static void testGetE6() {
        final Geopoint gp = new Geopoint(41.2, -3.4);
        Assert.assertEquals(41200000.0, gp.getLatitudeE6(), 1e-6);
        Assert.assertEquals(-3400000.0, gp.getLongitudeE6(), 1e-6);
    }

    public static void testBearingDistance() {
        final Geopoint gp1 = new Geopoint(-30.4, -1.2);
        final Geopoint gp2 = new Geopoint(-30.1, -2.3);

        final float d12 = gp1.distanceTo(gp2);
        Assert.assertEquals(110.967995, d12, 1e-6);
        Assert.assertEquals(d12, gp2.distanceTo(gp1), 1e-6);

        // Bearing in both directions cannot be added, as this is
        // the initial bearing of the path in both cases.
        Assert.assertEquals(287.162, gp1.bearingTo(gp2), 1e-3);
        Assert.assertEquals(107.715, gp2.bearingTo(gp1), 1e-3);
    }

}
