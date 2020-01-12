package cgeo.geocaching.location;

import android.os.Build;

import org.junit.Test;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeopointTest {

    @Test
    public void testCreation() {
        final Geopoint gp = new Geopoint(48.2, 3.5);
        assertThat(gp.getLatitude()).isEqualTo(48.2, offset(1e-8));
        assertThat(gp.getLongitude()).isEqualTo(3.5, offset(1e-8));
    }

    @Test
    public void testCreationWithParsing() {
        final Geopoint gp = new Geopoint("N 52° 25,111 E 009° 39,111");
        assertThat(gp.getLatitude()).isEqualTo(52.41852, offset(1e-4));
        assertThat(gp.getLongitude()).isEqualTo(9.65185, offset(1e-4));
    }

    @Test
    public void testCreationAtLimit() {
        // No exception should be raised.
        final Geopoint gp1 = new Geopoint(90.0, 10.0);
        assertThat(gp1.getLatitude()).isEqualTo(90, offset(1e-8));

        final Geopoint gp2 = new Geopoint(-90.0, 10.0);
        assertThat(gp2.getLatitude()).isEqualTo(-90, offset(1e-8));

        final Geopoint gp3 = new Geopoint(10.0, 180.0);
        assertThat(gp3.getLongitude()).isEqualTo(180, offset(1e-8));
    }

    @Test
    public void testEqual() {
        final Geopoint gp1 = new Geopoint(48.2, 2.31);
        assertThat(gp1).isEqualTo(gp1);
        final Geopoint gp2 = new Geopoint(48.3, 2.31);
        assertThat(gp1).isNotEqualTo(gp2);
    }

    @Test
    public void testEqualExternal() {
        final Geopoint gp1 = new Geopoint(48.2, 2.31);
        assertThat(Geopoint.equals(gp1, gp1)).isTrue();
        final Geopoint gp2 = new Geopoint(48.3, 2.31);
        assertThat(Geopoint.equals(gp1, gp2)).isFalse();
        assertThat(Geopoint.equals(null, null)).isTrue();
        assertThat(Geopoint.equals(null, gp1)).isFalse();
        assertThat(Geopoint.equals(gp1, null)).isFalse();
    }

    @Test
    public void testGetE6() {
        final Geopoint gp = new Geopoint(41.2, -3.4);
        assertThat((double) gp.getLatitudeE6()).isEqualTo(41200000.0, offset(1e-6));
        assertThat((double) gp.getLongitudeE6()).isEqualTo(-3400000.0, offset(1e-6));
    }

    @Test
    public void testEqualsFormatted() {
        final Geopoint gp1 = new Geopoint(48.559984, 2.713871);
        final Geopoint gp2 = new Geopoint(48.559981, 2.713873);
        assertThat(Geopoint.equals(gp1, gp2)).isFalse();
        assertThat(Geopoint.equalsFormatted(gp1, gp2, GeopointFormatter.Format.LAT_LON_DECMINUTE)).isTrue();
    }

    @Test
    public void testBearingDistance() {
        final Geopoint gp1 = new Geopoint(-30.4, -1.2);
        final Geopoint gp2 = new Geopoint(-30.1, -2.3);

        final float d12 = gp1.distanceTo(gp2);

        // broken distance calculation in 4.2.1
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
            assertThat((double) d12).isEqualTo(110.83107, offset(1e-6));
        } else {
            assertThat((double) d12).isEqualTo(110.967995, offset(1e-6));
        }

        assertThat((double) gp2.distanceTo(gp1)).isEqualTo(d12, offset(1e-6));

        // Bearing in both directions cannot be added, as this is
        // the initial bearing of the path in both cases.
        assertThat((double) gp1.bearingTo(gp2)).isEqualTo(287.162, offset(1e-3));
        assertThat((double) gp2.bearingTo(gp1)).isEqualTo(107.715, offset(1e-3));
    }

    @Test
    public void testDDD() {
        // Maximum acceptable deviation for degrees is 1e-5 (fractional part is scaled up by 1e5)

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

        checkTolerance(gp3, gp3a, 1e-5);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);

        checkDDD(gp4, 'N', 51, 12, 'E', 13, 89);

        final Geopoint gp4a = new Geopoint(String.valueOf(gp4.getLatDir()), String.valueOf(gp4.getLatDeg()), String.valueOf(gp4.getLatDegFrac()),
                String.valueOf(gp4.getLonDir()), String.valueOf(gp4.getLonDeg()), String.valueOf(gp4.getLonDegFrac()));

        checkTolerance(gp4, gp4a, 1e-5);

        // case 5
        final Geopoint gp5 = new Geopoint(51.00012d, 13.9999999d);

        checkDDD(gp5, 'N', 51, 12, 'E', 14, 0);

        final Geopoint gp5a = new Geopoint(String.valueOf(gp5.getLatDir()), String.valueOf(gp5.getLatDeg()), String.valueOf(gp5.getLatDegFrac()),
                String.valueOf(gp5.getLonDir()), String.valueOf(gp5.getLonDeg()), String.valueOf(gp5.getLonDegFrac()));

        checkTolerance(gp5, gp5a, 1e-5);
    }

    private static void checkDDD(final Geopoint gp, final char latDir, final int latDeg, final int latDegFrac, final char lonDir, final int lonDeg, final int lonDegFrac) {
        assertThat(gp.getLatDir()).isEqualTo(latDir);
        assertThat(gp.getLatDeg()).isEqualTo(latDeg);
        assertThat(gp.getLatDegFrac()).isEqualTo(latDegFrac);
        assertThat(gp.getLonDir()).isEqualTo(lonDir);
        assertThat(gp.getLonDeg()).isEqualTo(lonDeg);
        assertThat(gp.getLonDegFrac()).isEqualTo(lonDegFrac);
    }

    private static void checkTolerance(final Geopoint gp1, final Geopoint gp2, final double tolerance) {
        assertThat(gp1.getLatitude()).isEqualTo(gp2.getLatitude(), offset(tolerance));
        assertThat(gp1.getLongitude()).isEqualTo(gp2.getLongitude(), offset(tolerance));
    }

    @Test
    public void testRaw() {
        final Geopoint gp1 = new Geopoint(51.3d, 13.8d);
        assertThat(gp1.getLatMinRaw()).isEqualTo(18);
        assertThat(gp1.getLonMinRaw()).isEqualTo(48);

        final Geopoint gp2 = new Geopoint(51.345678d, 13.876543d);
        assertThat(gp2.getLatMinRaw()).isEqualTo(20.74068, offset(1e-5));
        assertThat(gp2.getLonMinRaw()).isEqualTo(52.59258, offset(1e-5));
        assertThat(gp2.getLatSecRaw()).isEqualTo(44.4408, offset(1e-5));
        assertThat(gp2.getLonSecRaw()).isEqualTo(35.5548, offset(1e-5));

        final Geopoint gp3 = new Geopoint(-51.345678d, -13.876543d);
        assertThat(gp3.getLatMinRaw()).isEqualTo(20.74068, offset(1e-5));
        assertThat(gp3.getLonMinRaw()).isEqualTo(52.59258, offset(1e-5));
        assertThat(gp3.getLatSecRaw()).isEqualTo(44.4408, offset(1e-5));
        assertThat(gp3.getLonSecRaw()).isEqualTo(35.5548, offset(1e-5));

        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);
        assertThat(gp4.getLatMinRaw()).isEqualTo(0.0072, offset(1e-4));
        assertThat(gp4.getLonMinRaw()).isEqualTo(0.0534, offset(1e-4));
    }

    @Test
    public void testDMM() {
        // Maximum acceptable deviation for degrees+minutes is 2e-5 (fractional part is scaled up by 1e3)

        // case 1
        final Geopoint gp1 = new Geopoint(51.3d, 13.8d);

        checkDMM(gp1, 'N', 51, 18, 0, 'E', 13, 48, 0);

        final Geopoint gp1a = new Geopoint(String.valueOf(gp1.getLatDir()), String.valueOf(gp1.getLatDeg()), String.valueOf(gp1.getLatMin()), String.valueOf(gp1.getLatMinFrac()),
                String.valueOf(gp1.getLonDir()), String.valueOf(gp1.getLonDeg()), String.valueOf(gp1.getLonMin()), String.valueOf(gp1.getLonMinFrac()));

        assertThat(gp1a).isEqualTo(gp1);

        // case 1n
        final Geopoint gp1n = new Geopoint(-51.3d, -13.8d);

        checkDMM(gp1n, 'S', 51, 18, 0, 'W', 13, 48, 0);

        final Geopoint gp1na = new Geopoint(String.valueOf(gp1n.getLatDir()), String.valueOf(gp1n.getLatDeg()), String.valueOf(gp1n.getLatMin()), String.valueOf(gp1n.getLatMinFrac()),
                String.valueOf(gp1n.getLonDir()), String.valueOf(gp1n.getLonDeg()), String.valueOf(gp1n.getLonMin()), String.valueOf(gp1n.getLonMinFrac()));

        assertThat(gp1na).isEqualTo(gp1n);

        // case 2
        final Geopoint gp2 = new Geopoint(51.34567d, 13.87654d);

        checkDMM(gp2, 'N', 51, 20, 740, 'E', 13, 52, 592);

        final Geopoint gp2a = new Geopoint(String.valueOf(gp2.getLatDir()), String.valueOf(gp2.getLatDeg()), String.valueOf(gp2.getLatMin()), String.valueOf(gp2.getLatMinFrac()),
                String.valueOf(gp2.getLonDir()), String.valueOf(gp2.getLonDeg()), String.valueOf(gp2.getLonMin()), String.valueOf(gp2.getLonMinFrac()));

        checkTolerance(gp2, gp2a, 2e-5);

        // case 3
        final Geopoint gp3 = new Geopoint(51.3d, 13.8d);

        checkDMM(gp3, 'N', 51, 18, 0, 'E', 13, 48, 0);

        final Geopoint gp3a = new Geopoint(String.valueOf(gp3.getLatDir()), String.valueOf(gp3.getLatDeg()), String.valueOf(gp3.getLatMin()), String.valueOf(gp3.getLatMinFrac()),
                String.valueOf(gp3.getLonDir()), String.valueOf(gp3.getLonDeg()), String.valueOf(gp3.getLonMin()), String.valueOf(gp3.getLonMinFrac()));

        checkTolerance(gp3, gp3a, 2e-5);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);

        checkDMM(gp4, 'N', 51, 0, 7, 'E', 13, 0, 53);

        final Geopoint gp4a = new Geopoint(String.valueOf(gp4.getLatDir()), String.valueOf(gp4.getLatDeg()), String.valueOf(gp4.getLatMin()), String.valueOf(gp4.getLatMinFrac()),
                String.valueOf(gp4.getLonDir()), String.valueOf(gp4.getLonDeg()), String.valueOf(gp4.getLonMin()), String.valueOf(gp4.getLonMinFrac()));

        checkTolerance(gp4, gp4a, 2e-5);

        // case 5
        final Geopoint gp5 = new Geopoint(51.00012d, 13.999999d);

        checkDMM(gp5, 'N', 51, 0, 7, 'E', 13, 59, 999);

        final Geopoint gp5a = new Geopoint(String.valueOf(gp5.getLatDir()), String.valueOf(gp5.getLatDeg()), String.valueOf(gp5.getLatMin()), String.valueOf(gp5.getLatMinFrac()),
                String.valueOf(gp5.getLonDir()), String.valueOf(gp5.getLonDeg()), String.valueOf(gp5.getLonMin()), String.valueOf(gp5.getLonMinFrac()));

        checkTolerance(gp5, gp5a, 2e-5);
    }

    private static void checkDMM(final Geopoint gp, final char latDir, final int latDeg, final int latMin, final int latMinFrac, final char lonDir, final int lonDeg, final int lonMin, final int lonMinFrac) {
        assertThat(gp.getLatDir()).isEqualTo(latDir);
        assertThat(gp.getLatDeg()).isEqualTo(latDeg);
        assertThat(gp.getLatMin()).isEqualTo(latMin);
        assertThat(gp.getLatMinFrac()).isEqualTo(latMinFrac);
        assertThat(gp.getLonDir()).isEqualTo(lonDir);
        assertThat(gp.getLonDeg()).isEqualTo(lonDeg);
        assertThat(gp.getLonMin()).isEqualTo(lonMin);
        assertThat(gp.getLonMinFrac()).isEqualTo(lonMinFrac);
    }

    @Test
    public void testDMS() {
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

        checkTolerance(gp2, gp2a, 1e-6);

        // case 3
        final Geopoint gp3 = new Geopoint(51.29999833333333d, 13.8d);

        checkDMS(gp3, 'N', 51, 17, 59, 992, 'E', 13, 48, 0, 0);

        final Geopoint gp3a = new Geopoint(String.valueOf(gp3.getLatDir()), String.valueOf(gp3.getLatDeg()), String.valueOf(gp3.getLatMin()), String.valueOf(gp3.getLatSec()), String.valueOf(gp3.getLatSecFrac()),
                String.valueOf(gp3.getLonDir()), String.valueOf(gp3.getLonDeg()), String.valueOf(gp3.getLonMin()), String.valueOf(gp3.getLonSec()), String.valueOf(gp3.getLonSecFrac()));

        checkTolerance(gp3, gp3a, 1e-6);

        // case 4
        final Geopoint gp4 = new Geopoint(51.00012d, 13.00089d);

        checkDMS(gp4, 'N', 51, 0, 0, 432, 'E', 13, 0, 3, 204);

        final Geopoint gp4a = new Geopoint(String.valueOf(gp4.getLatDir()), String.valueOf(gp4.getLatDeg()), String.valueOf(gp4.getLatMin()), String.valueOf(gp4.getLatSec()), String.valueOf(gp4.getLatSecFrac()),
                String.valueOf(gp4.getLonDir()), String.valueOf(gp4.getLonDeg()), String.valueOf(gp4.getLonMin()), String.valueOf(gp4.getLonSec()), String.valueOf(gp4.getLonSecFrac()));

        checkTolerance(gp4, gp4a, 1e-6);
    }

    private static void checkDMS(final Geopoint gp, final char latDir, final int latDeg, final int latMin, final int latSec, final int latSecFrac, final char lonDir, final int lonDeg, final int lonMin, final int lonSec, final int lonSecFrac) {
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

    @SuppressWarnings("unused")
    @Test(expected = Geopoint.ParseException.class)
    public void testParseParam1() {
        new Geopoint("some nonsense text");
    }

    @SuppressWarnings("unused")
    @Test(expected = Geopoint.ParseException.class)
    public void testParseParam2() {
        new Geopoint("latitude", "longitude");
    }

    @SuppressWarnings("unused")
    @Test(expected = Geopoint.ParseException.class)
    public void testParseParam6() {
        new Geopoint("latDir", "latDeg", "latDegFrac", "lonDir", "lonDeg", "lonDegFrac");
    }

    @SuppressWarnings("unused")
    @Test(expected = Geopoint.ParseException.class)
    public void testParseParam8() {
        new Geopoint("latDir", "latDeg", "latMin", "latMinFrac", "lonDir", "lonDeg", "lonMin", "lonMinFrac");
    }

    @SuppressWarnings("unused")
    @Test(expected = Geopoint.ParseException.class)
    public void testParseParam10() {
        new Geopoint("latDir", "latDeg", "latMin", "latSec", "latSecFrac", "lonDir", "lonDeg", "lonMin", "lonSec", "lonSecFrac");
    }

    @Test
    public void testValid() {
        assertThat(new Geopoint(0, 0).isValid()).isTrue();
        assertThat(new Geopoint(90, 0).isValid()).isTrue();
        assertThat(new Geopoint(91, 0).isValid()).isFalse();
        assertThat(new Geopoint(-90, 0).isValid()).isTrue();
        assertThat(new Geopoint(-91, 0).isValid()).isFalse();
        assertThat(new Geopoint(0, 180).isValid()).isTrue();
        assertThat(new Geopoint(0, 181).isValid()).isFalse();
        assertThat(new Geopoint(0, -180).isValid()).isTrue();
        assertThat(new Geopoint(0, -181).isValid()).isFalse();
    }

    @Test
    public void testRounded() {
        final Geopoint gp = new Geopoint(1.234567, 0.123456);
        assertThat(gp.roundedAt(1)).isEqualTo(new Geopoint(1, 0));
        assertThat(gp.roundedAt(10)).isEqualTo(new Geopoint(1.2, 0.1));
        assertThat(gp.roundedAt(100)).isEqualTo(new Geopoint(1.23, 0.12));
        assertThat(gp.roundedAt(1000)).isEqualTo(new Geopoint(1.235, 0.123));
        assertThat(gp.roundedAt(10000)).isEqualTo(new Geopoint(1.2346, 0.1235));
        assertThat(gp.roundedAt(100000)).isEqualTo(new Geopoint(1.23457, 0.12346));
        assertThat(gp.roundedAt(1000000)).isEqualTo(new Geopoint(1.234567, 0.123456));
    }
}
