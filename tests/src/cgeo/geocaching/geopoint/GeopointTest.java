package cgeo.geocaching.geopoint;

import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Geopoint.GeopointException;

import android.test.AndroidTestCase;

import junit.framework.Assert;

@SuppressWarnings("static-method")
public class GeopointTest extends AndroidTestCase {

    public void testCreation() {
        final Geopoint gp = new Geopoint(48.2, 3.5);
        Assert.assertEquals(48.2, gp.getLatitude(), 1e-8);
        Assert.assertEquals(3.5, gp.getLongitude(), 1e-8);
    }

    public void testCreationAtLimit() {
        // No exception should be raised at limits.
        final Geopoint gp1 = new Geopoint(90.0, 10.0);
        Assert.assertEquals(90, gp1.getLatitude(), 1e-8);

        final Geopoint gp2 = new Geopoint(-90.0, 10.0);
        Assert.assertEquals(-90, gp2.getLatitude(), 1e-8);

        final Geopoint gp3 = new Geopoint(10.0, 180.0);
        Assert.assertEquals(180, gp3.getLongitude(), 1e-8);

        // 180 should be preferred to -180
        final Geopoint gp4 = new Geopoint(10.0, -180.0);
        Assert.assertEquals(180, gp4.getLongitude(), 1e-8);
    }

    private static void createShouldFail(final double lat, final double lon) {
        try {
            final Geopoint gp = new Geopoint(lat, lon);
            Assert.fail("creation should fail: " + gp);
        } catch (GeopointException e) {
            // Success
        }
    }

    public void testCreationFails() {
        createShouldFail(90.1, 0.0);
        createShouldFail(-90.1, 0.0);
        createShouldFail(0.0, 180.1);
        createShouldFail(0.0, -180.1);
    }

    public void testEqual() {
        final Geopoint gp1 = new Geopoint(48.2, 2.31);
        Assert.assertTrue(gp1.equals(gp1));
        final Geopoint gp2 = new Geopoint(48.3, 2.31);
        Assert.assertFalse(gp1.equals(gp2));
    }

    public void testCreateE6() {
        final Geopoint gp1 = new Geopoint(48.2, 2.34);
        final Geopoint gp2 = new Geopoint(48200000, 2340000);
        Assert.assertTrue(gp1.isEqualTo(gp2, 1e-6));
    }

    public void testGetE6() {
        final Geopoint gp = new Geopoint(41.2, -3.4);
        Assert.assertEquals(41200000.0, gp.getLatitudeE6(), 1e-6);
        Assert.assertEquals(-3400000.0, gp.getLongitudeE6(), 1e-6);
    }

    public void testBearingDistance() {
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
