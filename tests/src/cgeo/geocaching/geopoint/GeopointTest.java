package cgeo.geocaching.geopoint;

import cgeo.geocaching.geopoint.direction.DDD;
import cgeo.geocaching.geopoint.direction.DMM;
import cgeo.geocaching.geopoint.direction.DMS;

import android.os.Bundle;
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
        DDD ddd1 = gp1.asDDD();

        checkDDD(ddd1, 'N', 51, 30000, 'E', 13, 80000);

        Geopoint gp1a = DDD.createGeopoint(String.valueOf(ddd1.latDir), String.valueOf(ddd1.latDeg), String.valueOf(ddd1.latDegFrac),
                String.valueOf(ddd1.lonDir), String.valueOf(ddd1.lonDeg), String.valueOf(ddd1.lonDegFrac));

        Assert.assertTrue(gp1a.equals(gp1));

        // case 2
        final Geopoint gp2 = new Geopoint(51.34567d, 13.87654d);
        DDD ddd2 = gp2.asDDD();

        checkDDD(ddd2, 'N', 51, 34567, 'E', 13, 87654);

        Geopoint gp2a = DDD.createGeopoint(String.valueOf(ddd2.latDir), String.valueOf(ddd2.latDeg), String.valueOf(ddd2.latDegFrac),
                String.valueOf(ddd2.lonDir), String.valueOf(ddd2.lonDeg), String.valueOf(ddd2.lonDegFrac));

        Assert.assertTrue(gp2a.equals(gp2));

        // case 3
        final Geopoint gp3 = new Geopoint(51.29999833333333d, 13.8d);
        DDD ddd3 = gp3.asDDD();

        checkDDD(ddd3, 'N', 51, 30000, 'E', 13, 80000);

        Geopoint gp3a = DDD.createGeopoint(String.valueOf(ddd3.latDir), String.valueOf(ddd3.latDeg), String.valueOf(ddd3.latDegFrac),
                String.valueOf(ddd3.lonDir), String.valueOf(ddd3.lonDeg), String.valueOf(ddd3.lonDegFrac));

        checkTolerance(gp3, gp3a, 5e-5);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);
        DDD ddd4 = gp4.asDDD();

        checkDDD(ddd4, 'N', 51, 12, 'E', 13, 89);

        Geopoint gp4a = DDD.createGeopoint(String.valueOf(ddd4.latDir), String.valueOf(ddd4.latDeg), String.valueOf(ddd4.latDegFrac),
                String.valueOf(ddd4.lonDir), String.valueOf(ddd4.lonDeg), String.valueOf(ddd4.lonDegFrac));

        checkTolerance(gp4, gp4a, 5e-5);
    }

    private static void checkDDD(DDD ddd, char latDir, int latDeg, int latDegFrac, char lonDir, int lonDeg, int lonDegFrac) {
        Assert.assertEquals(latDir, ddd.latDir);
        Assert.assertEquals(latDeg, ddd.latDeg);
        Assert.assertEquals(latDegFrac, ddd.latDegFrac);
        Assert.assertEquals(lonDir, ddd.lonDir);
        Assert.assertEquals(lonDeg, ddd.lonDeg);
        Assert.assertEquals(lonDegFrac, ddd.lonDegFrac);
    }

    private static void checkTolerance(Geopoint gp1, Geopoint gp2, double tolerance) {
        Assert.assertTrue(Math.abs(gp1.getLatitude() - gp2.getLatitude()) <= tolerance);
        Assert.assertTrue(Math.abs(gp1.getLongitude() - gp2.getLongitude()) <= tolerance);
    }

    public static void testDMM() {
        // case 1
        final Geopoint gp1 = new Geopoint(51.3d, 13.8d);
        DMM dmm1 = gp1.asDMM();

        checkDMM(dmm1, 'N', 51, 18, 0, 'E', 13, 48, 0);

        Geopoint gp1a = DMM.createGeopoint(String.valueOf(dmm1.latDir), String.valueOf(dmm1.latDeg), String.valueOf(dmm1.latMin), String.valueOf(dmm1.latMinFrac),
                String.valueOf(dmm1.lonDir), String.valueOf(dmm1.lonDeg), String.valueOf(dmm1.lonMin), String.valueOf(dmm1.lonMinFrac));

        Assert.assertTrue(gp1a.equals(gp1));

        // case 2
        final Geopoint gp2 = new Geopoint(51.34567d, 13.87654d);
        DMM dmm2 = gp2.asDMM();

        checkDMM(dmm2, 'N', 51, 20, 740, 'E', 13, 52, 592);

        Geopoint gp2a = DMM.createGeopoint(String.valueOf(dmm2.latDir), String.valueOf(dmm2.latDeg), String.valueOf(dmm2.latMin), String.valueOf(dmm2.latMinFrac),
                String.valueOf(dmm2.lonDir), String.valueOf(dmm2.lonDeg), String.valueOf(dmm2.lonMin), String.valueOf(dmm2.lonMinFrac));

        checkTolerance(gp2, gp2a, 5e-5);

        // case 3
        final Geopoint gp3 = new Geopoint(51.29999833333333d, 13.8d);
        DMM dmm3 = gp3.asDMM();

        checkDMM(dmm3, 'N', 51, 18, 0, 'E', 13, 48, 0);

        Geopoint gp3a = DMM.createGeopoint(String.valueOf(dmm3.latDir), String.valueOf(dmm3.latDeg), String.valueOf(dmm3.latMin), String.valueOf(dmm3.latMinFrac),
                String.valueOf(dmm3.lonDir), String.valueOf(dmm3.lonDeg), String.valueOf(dmm3.lonMin), String.valueOf(dmm3.lonMinFrac));

        checkTolerance(gp3, gp3a, 5e-5);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);
        DMM dmm4 = gp4.asDMM();

        checkDMM(dmm4, 'N', 51, 0, 7, 'E', 13, 0, 53);

        Geopoint gp4a = DMM.createGeopoint(String.valueOf(dmm4.latDir), String.valueOf(dmm4.latDeg), String.valueOf(dmm4.latMin), String.valueOf(dmm4.latMinFrac),
                String.valueOf(dmm4.lonDir), String.valueOf(dmm4.lonDeg), String.valueOf(dmm4.lonMin), String.valueOf(dmm4.lonMinFrac));

        checkTolerance(gp4, gp4a, 5e-5);
    }

    private static void checkDMM(DMM dmm, char latDir, int latDeg, int latMin, int latMinFrac, char lonDir, int lonDeg, int lonMin, int lonMinFrac) {
        Assert.assertEquals(latDir, dmm.latDir);
        Assert.assertEquals(latDeg, dmm.latDeg);
        Assert.assertEquals(latMin, dmm.latMin);
        Assert.assertEquals(latMinFrac, dmm.latMinFrac);
        Assert.assertEquals(lonDir, dmm.lonDir);
        Assert.assertEquals(lonDeg, dmm.lonDeg);
        Assert.assertEquals(lonMin, dmm.lonMin);
        Assert.assertEquals(lonMinFrac, dmm.lonMinFrac);
    }

    public static void testDMS() {
        // case 1
        final Geopoint gp1 = new Geopoint(51.3d, 13.8d);
        DMS dms1 = gp1.asDMS();

        checkDMS(dms1, 'N', 51, 18, 0, 0, 'E', 13, 48, 0, 0);

        Geopoint gp1a = DMS.createGeopoint(String.valueOf(dms1.latDir), String.valueOf(dms1.latDeg), String.valueOf(dms1.latMin), String.valueOf(dms1.latSec), String.valueOf(dms1.latSecFrac),
                String.valueOf(dms1.lonDir), String.valueOf(dms1.lonDeg), String.valueOf(dms1.lonMin), String.valueOf(dms1.lonSec), String.valueOf(dms1.lonSecFrac));

        Assert.assertTrue(gp1a.equals(gp1));

        // case 2
        final Geopoint gp2 = new Geopoint(51.34567d, 13.87654d);
        DMS dms2 = gp2.asDMS();

        checkDMS(dms2, 'N', 51, 20, 44, 412, 'E', 13, 52, 35, 544);

        Geopoint gp2a = DMS.createGeopoint(String.valueOf(dms2.latDir), String.valueOf(dms2.latDeg), String.valueOf(dms2.latMin), String.valueOf(dms2.latSec), String.valueOf(dms2.latSecFrac),
                String.valueOf(dms2.lonDir), String.valueOf(dms2.lonDeg), String.valueOf(dms2.lonMin), String.valueOf(dms2.lonSec), String.valueOf(dms2.lonSecFrac));

        checkTolerance(gp2, gp2a, 5e-6);

        // case 3
        final Geopoint gp3 = new Geopoint(51.29999833333333d, 13.8d);
        DMS dms3 = gp3.asDMS();

        checkDMS(dms3, 'N', 51, 17, 59, 994, 'E', 13, 48, 0, 0);

        Geopoint gp3a = DMS.createGeopoint(String.valueOf(dms3.latDir), String.valueOf(dms3.latDeg), String.valueOf(dms3.latMin), String.valueOf(dms3.latSec), String.valueOf(dms3.latSecFrac),
                String.valueOf(dms3.lonDir), String.valueOf(dms3.lonDeg), String.valueOf(dms3.lonMin), String.valueOf(dms3.lonSec), String.valueOf(dms3.lonSecFrac));

        checkTolerance(gp3, gp3a, 5e-6);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);
        DMS dms4 = gp4.asDMS();

        checkDMS(dms4, 'N', 51, 0, 0, 432, 'E', 13, 0, 3, 204);

        Geopoint gp4a = DMS.createGeopoint(String.valueOf(dms4.latDir), String.valueOf(dms4.latDeg), String.valueOf(dms4.latMin), String.valueOf(dms4.latSec), String.valueOf(dms4.latSecFrac),
                String.valueOf(dms4.lonDir), String.valueOf(dms4.lonDeg), String.valueOf(dms4.lonMin), String.valueOf(dms4.lonSec), String.valueOf(dms4.lonSecFrac));

        checkTolerance(gp4, gp4a, 5e-6);
    }

    private static void checkDMS(DMS dms, char latDir, int latDeg, int latMin, int latSec, int latSecFrac, char lonDir, int lonDeg, int lonMin, int lonSec, int lonSecFrac) {
        Assert.assertEquals(latDir, dms.latDir);
        Assert.assertEquals(latDeg, dms.latDeg);
        Assert.assertEquals(latMin, dms.latMin);
        Assert.assertEquals(latSec, dms.latSec);
        Assert.assertEquals(latSecFrac, dms.latSecFrac);
        Assert.assertEquals(lonDir, dms.lonDir);
        Assert.assertEquals(lonDeg, dms.lonDeg);
        Assert.assertEquals(lonMin, dms.lonMin);
        Assert.assertEquals(lonSec, dms.lonSec);
        Assert.assertEquals(lonSecFrac, dms.lonSecFrac);
    }

    public static void testElevation() {
        assertEquals(125.663703918457, (new Geopoint(48.0, 2.0)).getElevation(), 0.1);
    }

}
