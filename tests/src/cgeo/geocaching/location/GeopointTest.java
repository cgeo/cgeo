package cgeo.geocaching.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import cgeo.geocaching.location.Geopoint;

import android.os.Build;
import android.os.Bundle;
import android.test.AndroidTestCase;

public class GeopointTest extends AndroidTestCase {

    public static void testCreation() {
        final Geopoint gp = new Geopoint(48.2, 3.5);
        assertThat(gp.getLatitude()).isEqualTo(48.2, offset(1e-8));
        assertThat(gp.getLongitude()).isEqualTo(3.5, offset(1e-8));
    }

    public static void testCreationWithParsing() {
        final Geopoint gp = new Geopoint("N 52° 25,111 E 009° 39,111");
        assertThat(gp.getLatitude()).isEqualTo(52.41852, offset(1e-4));
        assertThat(gp.getLongitude()).isEqualTo(9.65185, offset(1e-4));
    }

    public static void testCreationAtLimit() {
        // No exception should be raised.
        final Geopoint gp1 = new Geopoint(90.0, 10.0);
        assertThat(gp1.getLatitude()).isEqualTo(90, offset(1e-8));

        final Geopoint gp2 = new Geopoint(-90.0, 10.0);
        assertThat(gp2.getLatitude()).isEqualTo(-90, offset(1e-8));

        final Geopoint gp3 = new Geopoint(10.0, 180.0);
        assertThat(gp3.getLongitude()).isEqualTo(180, offset(1e-8));
    }

    public static void testEqual() {
        final Geopoint gp1 = new Geopoint(48.2, 2.31);
        assertThat(gp1.equals(gp1)).isTrue();
        final Geopoint gp2 = new Geopoint(48.3, 2.31);
        assertThat(gp1.equals(gp2)).isFalse();
    }

    public static void testGetE6() {
        final Geopoint gp = new Geopoint(41.2, -3.4);
        assertThat((double) gp.getLatitudeE6()).isEqualTo(41200000.0, offset(1e-6));
        assertThat((double) gp.getLongitudeE6()).isEqualTo(-3400000.0, offset(1e-6));
    }

    public static void testBearingDistance() {
        final Geopoint gp1 = new Geopoint(-30.4, -1.2);
        final Geopoint gp2 = new Geopoint(-30.1, -2.3);

        final float d12 = gp1.distanceTo(gp2);

        // broken distance calculation in 4.2.1
        if (Build.VERSION.SDK_INT == 17) {
            assertThat((double) d12).isEqualTo(110.83107, offset(1e-6));
        }
        else {
            assertThat((double) d12).isEqualTo(110.967995, offset(1e-6));
        }

        assertThat((double) gp2.distanceTo(gp1)).isEqualTo(d12, offset(1e-6));

        // Bearing in both directions cannot be added, as this is
        // the initial bearing of the path in both cases.
        assertThat((double) gp1.bearingTo(gp2)).isEqualTo(287.162, offset(1e-3));
        assertThat((double) gp2.bearingTo(gp1)).isEqualTo(107.715, offset(1e-3));
    }

    public static void testParcelable() {
        final Geopoint gp = new Geopoint(1.2, 3.4);
        final String KEY = "geopoint";
        final Bundle bundle = new Bundle();
        bundle.putParcelable(KEY, gp);
        assertThat(bundle.getParcelable(KEY)).isEqualTo(gp);
    }

    public static void testDDD() {
        // case 1
        final Geopoint gp1 = new Geopoint(51.3d, 13.8d);

        checkDDD(gp1, 'N', 51, 30000, 'E', 13, 80000);

        final Geopoint gp1a = new Geopoint(String.valueOf(gp1.getLatDir()), String.valueOf(gp1.getLatDeg()), String.valueOf(gp1.getLatDegFrac()),
                String.valueOf(gp1.getLonDir()), String.valueOf(gp1.getLonDeg()), String.valueOf(gp1.getLonDegFrac()));

        assertThat(gp1a).isEqualTo(gp1);

        // case 2
        final Geopoint gp2 = new Geopoint(51.34567d, 13.87654d);

        checkDDD(gp2, 'N', 51, 34567, 'E', 13, 87654);

        final Geopoint gp2a = new Geopoint(String.valueOf(gp2.getLatDir()), String.valueOf(gp2.getLatDeg()), String.valueOf(gp2.getLatDegFrac()),
                String.valueOf(gp2.getLonDir()), String.valueOf(gp2.getLonDeg()), String.valueOf(gp2.getLonDegFrac()));

        assertThat(gp2a).isEqualTo(gp2);

        // case 3
        final Geopoint gp3 = new Geopoint(51.29999833333333d, 13.8d);

        checkDDD(gp3, 'N', 51, 30000, 'E', 13, 80000);

        final Geopoint gp3a = new Geopoint(String.valueOf(gp3.getLatDir()), String.valueOf(gp3.getLatDeg()), String.valueOf(gp3.getLatDegFrac()),
                String.valueOf(gp3.getLonDir()), String.valueOf(gp3.getLonDeg()), String.valueOf(gp3.getLonDegFrac()));

        checkTolerance(gp3, gp3a, 5e-5);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);

        checkDDD(gp4, 'N', 51, 12, 'E', 13, 89);

        final Geopoint gp4a = new Geopoint(String.valueOf(gp4.getLatDir()), String.valueOf(gp4.getLatDeg()), String.valueOf(gp4.getLatDegFrac()),
                String.valueOf(gp4.getLonDir()), String.valueOf(gp4.getLonDeg()), String.valueOf(gp4.getLonDegFrac()));

        checkTolerance(gp4, gp4a, 5e-5);
    }

    private static void checkDDD(Geopoint gp, char latDir, int latDeg, int latDegFrac, char lonDir, int lonDeg, int lonDegFrac) {
        assertThat(gp.getLatDir()).isEqualTo(latDir);
        assertThat(gp.getLatDeg()).isEqualTo(latDeg);
        assertThat(gp.getLatDegFrac()).isEqualTo(latDegFrac);
        assertThat(gp.getLonDir()).isEqualTo(lonDir);
        assertThat(gp.getLonDeg()).isEqualTo(lonDeg);
        assertThat(gp.getLonDegFrac()).isEqualTo(lonDegFrac);
    }

    private static void checkTolerance(Geopoint gp1, Geopoint gp2, double tolerance) {
        assertThat(Math.abs(gp1.getLatitude() - gp2.getLatitude()) <= tolerance).isTrue();
        assertThat(Math.abs(gp1.getLongitude() - gp2.getLongitude()) <= tolerance).isTrue();
    }

    public static void testDMM() {
        // case 1
        final Geopoint gp1 = new Geopoint(51.3d, 13.8d);

        checkDMM(gp1, 'N', 51, 18, 0, 'E', 13, 48, 0);

        final Geopoint gp1a = new Geopoint(String.valueOf(gp1.getLatDir()), String.valueOf(gp1.getLatDeg()), String.valueOf(gp1.getLatMin()), String.valueOf(gp1.getLatMinFrac()),
                String.valueOf(gp1.getLonDir()), String.valueOf(gp1.getLonDeg()), String.valueOf(gp1.getLonMin()), String.valueOf(gp1.getLonMinFrac()));

        assertThat(gp1a).isEqualTo(gp1);

        // case 2
        final Geopoint gp2 = new Geopoint(51.34567d, 13.87654d);

        checkDMM(gp2, 'N', 51, 20, 740, 'E', 13, 52, 592);

        final Geopoint gp2a = new Geopoint(String.valueOf(gp2.getLatDir()), String.valueOf(gp2.getLatDeg()), String.valueOf(gp2.getLatMin()), String.valueOf(gp2.getLatMinFrac()),
                String.valueOf(gp2.getLonDir()), String.valueOf(gp2.getLonDeg()), String.valueOf(gp2.getLonMin()), String.valueOf(gp2.getLonMinFrac()));

        checkTolerance(gp2, gp2a, 5e-5);

        // case 3
        final Geopoint gp3 = new Geopoint(51.3d, 13.8d);

        checkDMM(gp3, 'N', 51, 18, 0, 'E', 13, 48, 0);

        final Geopoint gp3a = new Geopoint(String.valueOf(gp3.getLatDir()), String.valueOf(gp3.getLatDeg()), String.valueOf(gp3.getLatMin()), String.valueOf(gp3.getLatMinFrac()),
                String.valueOf(gp3.getLonDir()), String.valueOf(gp3.getLonDeg()), String.valueOf(gp3.getLonMin()), String.valueOf(gp3.getLonMinFrac()));

        checkTolerance(gp3, gp3a, 5e-5);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);

        checkDMM(gp4, 'N', 51, 0, 7, 'E', 13, 0, 53);

        final Geopoint gp4a = new Geopoint(String.valueOf(gp4.getLatDir()), String.valueOf(gp4.getLatDeg()), String.valueOf(gp4.getLatMin()), String.valueOf(gp4.getLatMinFrac()),
                String.valueOf(gp4.getLonDir()), String.valueOf(gp4.getLonDeg()), String.valueOf(gp4.getLonMin()), String.valueOf(gp4.getLonMinFrac()));

        checkTolerance(gp4, gp4a, 5e-5);
    }

    private static void checkDMM(Geopoint gp, char latDir, int latDeg, int latMin, int latMinFrac, char lonDir, int lonDeg, int lonMin, int lonMinFrac) {
        assertThat(gp.getLatDir()).isEqualTo(latDir);
        assertThat(gp.getLatDeg()).isEqualTo(latDeg);
        assertThat(gp.getLatMin()).isEqualTo(latMin);
        assertThat(gp.getLatMinFrac()).isEqualTo(latMinFrac);
        assertThat(gp.getLonDir()).isEqualTo(lonDir);
        assertThat(gp.getLonDeg()).isEqualTo(lonDeg);
        assertThat(gp.getLonMin()).isEqualTo(lonMin);
        assertThat(gp.getLonMinFrac()).isEqualTo(lonMinFrac);
    }

    public static void testDMS() {
        // case 1
        final Geopoint gp1 = new Geopoint(51.3d, 13.8d);

        checkDMS(gp1, 'N', 51, 18, 0, 0, 'E', 13, 48, 0, 0);

        final Geopoint gp1a = new Geopoint(String.valueOf(gp1.getLatDir()), String.valueOf(gp1.getLatDeg()), String.valueOf(gp1.getLatMin()), String.valueOf(gp1.getLatSec()), String.valueOf(gp1.getLatSecFrac()),
                String.valueOf(gp1.getLonDir()), String.valueOf(gp1.getLonDeg()), String.valueOf(gp1.getLonMin()), String.valueOf(gp1.getLonSec()), String.valueOf(gp1.getLonSecFrac()));

        assertThat(gp1a).isEqualTo(gp1);

        // case 2
        final Geopoint gp2 = new Geopoint(51.34567d, 13.87654d);

        checkDMS(gp2, 'N', 51, 20, 44, 412, 'E', 13, 52, 35, 544);

        final Geopoint gp2a = new Geopoint(String.valueOf(gp2.getLatDir()), String.valueOf(gp2.getLatDeg()), String.valueOf(gp2.getLatMin()), String.valueOf(gp2.getLatSec()), String.valueOf(gp2.getLatSecFrac()),
                String.valueOf(gp2.getLonDir()), String.valueOf(gp2.getLonDeg()), String.valueOf(gp2.getLonMin()), String.valueOf(gp2.getLonSec()), String.valueOf(gp2.getLonSecFrac()));

        checkTolerance(gp2, gp2a, 5e-6);

        // case 3
        final Geopoint gp3 = new Geopoint(51.29999833333333d, 13.8d);

        checkDMS(gp3, 'N', 51, 17, 59, 994, 'E', 13, 48, 0, 0);

        final Geopoint gp3a = new Geopoint(String.valueOf(gp3.getLatDir()), String.valueOf(gp3.getLatDeg()), String.valueOf(gp3.getLatMin()), String.valueOf(gp3.getLatSec()), String.valueOf(gp3.getLatSecFrac()),
                String.valueOf(gp3.getLonDir()), String.valueOf(gp3.getLonDeg()), String.valueOf(gp3.getLonMin()), String.valueOf(gp3.getLonSec()), String.valueOf(gp3.getLonSecFrac()));

        checkTolerance(gp3, gp3a, 5e-6);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);

        checkDMS(gp4, 'N', 51, 0, 0, 432, 'E', 13, 0, 3, 204);

        final Geopoint gp4a = new Geopoint(String.valueOf(gp4.getLatDir()), String.valueOf(gp4.getLatDeg()), String.valueOf(gp4.getLatMin()), String.valueOf(gp4.getLatSec()), String.valueOf(gp4.getLatSecFrac()),
                String.valueOf(gp4.getLonDir()), String.valueOf(gp4.getLonDeg()), String.valueOf(gp4.getLonMin()), String.valueOf(gp4.getLonSec()), String.valueOf(gp4.getLonSecFrac()));

        checkTolerance(gp4, gp4a, 5e-6);
    }

    private static void checkDMS(Geopoint gp, char latDir, int latDeg, int latMin, int latSec, int latSecFrac, char lonDir, int lonDeg, int lonMin, int lonSec, int lonSecFrac) {
        assertThat(gp.getLatDir()).isEqualTo(latDir);
        assertThat(gp.getLatDeg()).isEqualTo(latDeg);
        assertThat(gp.getLatMin()).isEqualTo(latMin);
        assertThat(gp.getLatSec()).isEqualTo(latSec);
        assertThat(gp.getLatSecFrac()).isEqualTo(latSecFrac);
        assertThat(gp.getLonDir()).isEqualTo(lonDir);
        assertThat(gp.getLonDeg()).isEqualTo(lonDeg);
        assertThat(gp.getLonMin()).isEqualTo(lonMin);
        assertThat(gp.getLonSec()).isEqualTo(lonSec);
        assertThat(gp.getLonSecFrac()).isEqualTo(lonSecFrac);
    }

    private static void assertParseException(Runnable runnable) {
        try {
            runnable.run();
            fail("Should have thrown Geopoint.ParseException");
        } catch (Geopoint.ParseException e) {
            //success
        }
    }

    public static void testParseParam1() {
        assertParseException(new Runnable() {

            @SuppressWarnings("unused")
            @Override
            public void run() {
                new Geopoint("some nonsense text");
            }
        });
    }

    public static void testParseParam2() {
        assertParseException(new Runnable() {

            @SuppressWarnings("unused")
            @Override
            public void run() {
                new Geopoint("latitude", "longitude");
            }
        });
    }

    public static void testParseParam6() {
        assertParseException(new Runnable() {

            @SuppressWarnings("unused")
            @Override
            public void run() {
                new Geopoint("latDir", "latDeg", "latDegFrac", "lonDir", "lonDeg", "lonDegFrac");
            }
        });
    }

    public static void testParseParam8() {
        assertParseException(new Runnable() {

            @SuppressWarnings("unused")
            @Override
            public void run() {
                new Geopoint("latDir", "latDeg", "latMin", "latMinFrac", "lonDir", "lonDeg", "lonMin", "lonMinFrac");
            }
        });
    }

    public static void testParseParam10() {
        assertParseException(new Runnable() {

            @SuppressWarnings("unused")
            @Override
            public void run() {
                new Geopoint("latDir", "latDeg", "latMin", "latSec", "latSecFrac", "lonDir", "lonDeg", "lonMin", "lonSec", "lonSecFrac");
            }
        });
    }
}
