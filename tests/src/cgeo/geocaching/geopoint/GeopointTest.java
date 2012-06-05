package cgeo.geocaching.geopoint;

import junit.framework.Assert;

import android.os.Bundle;
import android.test.AndroidTestCase;

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

    public static void testParcelable() {
        final Geopoint gp = new Geopoint(1.2, 3.4);
        final String KEY = "geopoint";
        final Bundle bundle = new Bundle();
        bundle.putParcelable(KEY, gp);
        assertEquals(gp, bundle.getParcelable(KEY));
    }

    public static void testDDD() {
        // case 1
        final Geopoint gp1 = new Geopoint(51.3d, 13.8d);

        checkDDD(gp1, 'N', 51, 30000, 'E', 13, 80000);

        Geopoint gp1a = new Geopoint(String.valueOf(gp1.getLatDir()), String.valueOf(gp1.getLatDeg()), String.valueOf(gp1.getLatDegFrac()),
                String.valueOf(gp1.getLonDir()), String.valueOf(gp1.getLonDeg()), String.valueOf(gp1.getLonDegFrac()));

        Assert.assertTrue(gp1a.equals(gp1));

        // case 2
        final Geopoint gp2 = new Geopoint(51.34567d, 13.87654d);

        checkDDD(gp2, 'N', 51, 34567, 'E', 13, 87654);

        Geopoint gp2a = new Geopoint(String.valueOf(gp2.getLatDir()), String.valueOf(gp2.getLatDeg()), String.valueOf(gp2.getLatDegFrac()),
                String.valueOf(gp2.getLonDir()), String.valueOf(gp2.getLonDeg()), String.valueOf(gp2.getLonDegFrac()));

        Assert.assertTrue(gp2a.equals(gp2));

        // case 3
        final Geopoint gp3 = new Geopoint(51.29999833333333d, 13.8d);

        checkDDD(gp3, 'N', 51, 30000, 'E', 13, 80000);

        Geopoint gp3a = new Geopoint(String.valueOf(gp3.getLatDir()), String.valueOf(gp3.getLatDeg()), String.valueOf(gp3.getLatDegFrac()),
                String.valueOf(gp3.getLonDir()), String.valueOf(gp3.getLonDeg()), String.valueOf(gp3.getLonDegFrac()));

        checkTolerance(gp3, gp3a, 5e-5);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);

        checkDDD(gp4, 'N', 51, 12, 'E', 13, 89);

        Geopoint gp4a = new Geopoint(String.valueOf(gp4.getLatDir()), String.valueOf(gp4.getLatDeg()), String.valueOf(gp4.getLatDegFrac()),
                String.valueOf(gp4.getLonDir()), String.valueOf(gp4.getLonDeg()), String.valueOf(gp4.getLonDegFrac()));

        checkTolerance(gp4, gp4a, 5e-5);
    }

    private static void checkDDD(Geopoint gp, char latDir, int latDeg, int latDegFrac, char lonDir, int lonDeg, int lonDegFrac) {
        Assert.assertEquals(latDir, gp.getLatDir());
        Assert.assertEquals(latDeg, gp.getLatDeg());
        Assert.assertEquals(latDegFrac, gp.getLatDegFrac());
        Assert.assertEquals(lonDir, gp.getLonDir());
        Assert.assertEquals(lonDeg, gp.getLonDeg());
        Assert.assertEquals(lonDegFrac, gp.getLonDegFrac());
    }

    private static void checkTolerance(Geopoint gp1, Geopoint gp2, double tolerance) {
        Assert.assertTrue(Math.abs(gp1.getLatitude() - gp2.getLatitude()) <= tolerance);
        Assert.assertTrue(Math.abs(gp1.getLongitude() - gp2.getLongitude()) <= tolerance);
    }

    public static void testDMM() {
        // case 1
        final Geopoint gp1 = new Geopoint(51.3d, 13.8d);

        checkDMM(gp1, 'N', 51, 18, 0, 'E', 13, 48, 0);

        Geopoint gp1a = new Geopoint(String.valueOf(gp1.getLatDir()), String.valueOf(gp1.getLatDeg()), String.valueOf(gp1.getLatMin()), String.valueOf(gp1.getLatMinFrac()),
                String.valueOf(gp1.getLonDir()), String.valueOf(gp1.getLonDeg()), String.valueOf(gp1.getLonMin()), String.valueOf(gp1.getLonMinFrac()));

        Assert.assertTrue(gp1a.equals(gp1));

        // case 2
        final Geopoint gp2 = new Geopoint(51.34567d, 13.87654d);

        checkDMM(gp2, 'N', 51, 20, 740, 'E', 13, 52, 592);

        Geopoint gp2a = new Geopoint(String.valueOf(gp2.getLatDir()), String.valueOf(gp2.getLatDeg()), String.valueOf(gp2.getLatMin()), String.valueOf(gp2.getLatMinFrac()),
                String.valueOf(gp2.getLonDir()), String.valueOf(gp2.getLonDeg()), String.valueOf(gp2.getLonMin()), String.valueOf(gp2.getLonMinFrac()));

        checkTolerance(gp2, gp2a, 5e-5);

        // case 3
        final Geopoint gp3 = new Geopoint(51.3d, 13.8d);

        checkDMM(gp3, 'N', 51, 18, 0, 'E', 13, 48, 0);

        Geopoint gp3a = new Geopoint(String.valueOf(gp3.getLatDir()), String.valueOf(gp3.getLatDeg()), String.valueOf(gp3.getLatMin()), String.valueOf(gp3.getLatMinFrac()),
                String.valueOf(gp3.getLonDir()), String.valueOf(gp3.getLonDeg()), String.valueOf(gp3.getLonMin()), String.valueOf(gp3.getLonMinFrac()));

        checkTolerance(gp3, gp3a, 5e-5);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);

        checkDMM(gp4, 'N', 51, 0, 7, 'E', 13, 0, 53);

        Geopoint gp4a = new Geopoint(String.valueOf(gp4.getLatDir()), String.valueOf(gp4.getLatDeg()), String.valueOf(gp4.getLatMin()), String.valueOf(gp4.getLatMinFrac()),
                String.valueOf(gp4.getLonDir()), String.valueOf(gp4.getLonDeg()), String.valueOf(gp4.getLonMin()), String.valueOf(gp4.getLonMinFrac()));

        checkTolerance(gp4, gp4a, 5e-5);
    }

    private static void checkDMM(Geopoint gp, char latDir, int latDeg, int latMin, int latMinFrac, char lonDir, int lonDeg, int lonMin, int lonMinFrac) {
        Assert.assertEquals(latDir, gp.getLatDir());
        Assert.assertEquals(latDeg, gp.getLatDeg());
        Assert.assertEquals(latMin, gp.getLatMin());
        Assert.assertEquals(latMinFrac, gp.getLatMinFrac());
        Assert.assertEquals(lonDir, gp.getLonDir());
        Assert.assertEquals(lonDeg, gp.getLonDeg());
        Assert.assertEquals(lonMin, gp.getLonMin());
        Assert.assertEquals(lonMinFrac, gp.getLonMinFrac());
    }

    public static void testDMS() {
        // case 1
        final Geopoint gp1 = new Geopoint(51.3d, 13.8d);

        checkDMS(gp1, 'N', 51, 18, 0, 0, 'E', 13, 48, 0, 0);

        Geopoint gp1a = new Geopoint(String.valueOf(gp1.getLatDir()), String.valueOf(gp1.getLatDeg()), String.valueOf(gp1.getLatMin()), String.valueOf(gp1.getLatSec()), String.valueOf(gp1.getLatSecFrac()),
                String.valueOf(gp1.getLonDir()), String.valueOf(gp1.getLonDeg()), String.valueOf(gp1.getLonMin()), String.valueOf(gp1.getLonSec()), String.valueOf(gp1.getLonSecFrac()));

        Assert.assertTrue(gp1a.equals(gp1));

        // case 2
        final Geopoint gp2 = new Geopoint(51.34567d, 13.87654d);

        checkDMS(gp2, 'N', 51, 20, 44, 412, 'E', 13, 52, 35, 544);

        Geopoint gp2a = new Geopoint(String.valueOf(gp2.getLatDir()), String.valueOf(gp2.getLatDeg()), String.valueOf(gp2.getLatMin()), String.valueOf(gp2.getLatSec()), String.valueOf(gp2.getLatSecFrac()),
                String.valueOf(gp2.getLonDir()), String.valueOf(gp2.getLonDeg()), String.valueOf(gp2.getLonMin()), String.valueOf(gp2.getLonSec()), String.valueOf(gp2.getLonSecFrac()));

        checkTolerance(gp2, gp2a, 5e-6);

        // case 3
        final Geopoint gp3 = new Geopoint(51.29999833333333d, 13.8d);

        checkDMS(gp3, 'N', 51, 17, 59, 994, 'E', 13, 48, 0, 0);

        Geopoint gp3a = new Geopoint(String.valueOf(gp3.getLatDir()), String.valueOf(gp3.getLatDeg()), String.valueOf(gp3.getLatMin()), String.valueOf(gp3.getLatSec()), String.valueOf(gp3.getLatSecFrac()),
                String.valueOf(gp3.getLonDir()), String.valueOf(gp3.getLonDeg()), String.valueOf(gp3.getLonMin()), String.valueOf(gp3.getLonSec()), String.valueOf(gp3.getLonSecFrac()));

        checkTolerance(gp3, gp3a, 5e-6);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);

        checkDMS(gp4, 'N', 51, 0, 0, 432, 'E', 13, 0, 3, 204);

        Geopoint gp4a = new Geopoint(String.valueOf(gp4.getLatDir()), String.valueOf(gp4.getLatDeg()), String.valueOf(gp4.getLatMin()), String.valueOf(gp4.getLatSec()), String.valueOf(gp4.getLatSecFrac()),
                String.valueOf(gp4.getLonDir()), String.valueOf(gp4.getLonDeg()), String.valueOf(gp4.getLonMin()), String.valueOf(gp4.getLonSec()), String.valueOf(gp4.getLonSecFrac()));

        checkTolerance(gp4, gp4a, 5e-6);
    }

    private static void checkDMS(Geopoint gp, char latDir, int latDeg, int latMin, int latSec, int latSecFrac, char lonDir, int lonDeg, int lonMin, int lonSec, int lonSecFrac) {
        Assert.assertEquals(latDir, gp.getLatDir());
        Assert.assertEquals(latDeg, gp.getLatDeg());
        Assert.assertEquals(latMin, gp.getLatMin());
        Assert.assertEquals(latSec, gp.getLatSec());
        Assert.assertEquals(latSecFrac, gp.getLatSecFrac());
        Assert.assertEquals(lonDir, gp.getLonDir());
        Assert.assertEquals(lonDeg, gp.getLonDeg());
        Assert.assertEquals(lonMin, gp.getLonMin());
        Assert.assertEquals(lonSec, gp.getLonSec());
        Assert.assertEquals(lonSecFrac, gp.getLonSecFrac());
    }

    public static void testElevation() {
        assertEquals(125.663703918457, (new Geopoint(48.0, 2.0)).getElevation(), 0.1);
    }

}
