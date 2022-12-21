package cgeo.geocaching.brouter.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CheapAngleMeterTest {
    public static int toOsmLon(final double lon) {
        return (int) ((lon + 180.) / CheapRulerHelper.ILATLNG_TO_LATLNG + 0.5);
    }

    public static int toOsmLat(final double lat) {
        return (int) ((lat + 90.) / CheapRulerHelper.ILATLNG_TO_LATLNG + 0.5);
    }

    @Test
    public void testCalcAngle() {
        final CheapAngleMeter am = new CheapAngleMeter();
        // Segment ends
        int lon0;
        int lat0;
        int lon1;
        int lat1;
        int lon2;
        int lat2;

        lon0 = toOsmLon(2.317126);
        lat0 = toOsmLat(48.817927);
        lon1 = toOsmLon(2.317316);
        lat1 = toOsmLat(48.817978);
        lon2 = toOsmLon(2.317471);
        lat2 = toOsmLat(48.818043);
        assertEquals(
                "Works for an angle between -pi/4 and pi/4",
                -10.,
                am.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2),
                0.05 * 10.
        );

        lon0 = toOsmLon(2.317020662874013);
        lat0 = toOsmLat(48.81799440182911);
        lon1 = toOsmLon(2.3169460585876327);
        lat1 = toOsmLat(48.817812421536644);
        lon2 = lon0;
        lat2 = lat0;
        assertEquals(
                "Works for an angle between 3*pi/4 and 5*pi/4",
                180.,
                am.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2),
                0.05 * 180.
        );

        lon0 = toOsmLon(2.317112);
        lat0 = toOsmLat(48.817802);
        lon1 = toOsmLon(2.317632);
        lat1 = toOsmLat(48.817944);
        lon2 = toOsmLon(2.317673);
        lat2 = toOsmLat(48.817799);
        assertEquals(
                "Works for an angle between -3*pi/4 and -pi/4",
                100.,
                am.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2),
                0.1 * 100.
        );

        lon0 = toOsmLon(2.317128);
        lat0 = toOsmLat(48.818072);
        lon1 = toOsmLon(2.317532);
        lat1 = toOsmLat(48.818108);
        lon2 = toOsmLon(2.317497);
        lat2 = toOsmLat(48.818264);
        assertEquals(
                "Works for an angle between pi/4 and 3*pi/4",
                -100.,
                am.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2),
                0.1 * 100.
        );
    }

    @Test
    public void testCalcAngle2() {
        final CheapAngleMeter am = new CheapAngleMeter();
        final int lon1 = 8500000;
        final int lat1 = 49500000;

        final double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales(lat1);
        final double lon2m = lonlat2m[0];
        final double lat2m = lonlat2m[1];

        for (double afrom = -175.; afrom < 180.; afrom += 10.) {
            final double sf = Math.sin(afrom * Math.PI / 180.);
            final double cf = Math.cos(afrom * Math.PI / 180.);

            final int lon0 = (int) (0.5 + lon1 - cf * 150. / lon2m);
            final int lat0 = (int) (0.5 + lat1 - sf * 150. / lat2m);

            for (double ato = -177.; ato < 180.; ato += 10.) {
                final double st = Math.sin(ato * Math.PI / 180.);
                final double ct = Math.cos(ato * Math.PI / 180.);

                final int lon2 = (int) (0.5 + lon1 + ct * 250. / lon2m);
                final int lat2 = (int) (0.5 + lat1 + st * 250. / lat2m);

                double a1 = afrom - ato;
                if (a1 > 180.) {
                    a1 -= 360.;
                }
                if (a1 < -180.) {
                    a1 += 360.;
                }
                final double a2 = am.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2);
                final double c1 = Math.cos(a1 * Math.PI / 180.);
                final double c2 = am.getCosAngle();

                assertEquals("angle mismatch for afrom=" + afrom + " ato=" + ato, a1, a2, 0.2);
                assertEquals("cosinus mismatch for afrom=" + afrom + " ato=" + ato, c1, c2, 0.001);
            }
        }
    }

}
