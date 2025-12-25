// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.brouter.util

import org.junit.Test
import org.junit.Assert.assertEquals

class CheapAngleMeterTest {
    public static Int toOsmLon(final Double lon) {
        return (Int) ((lon + 180.) / CheapRulerHelper.ILATLNG_TO_LATLNG + 0.5)
    }

    public static Int toOsmLat(final Double lat) {
        return (Int) ((lat + 90.) / CheapRulerHelper.ILATLNG_TO_LATLNG + 0.5)
    }

    @Test
    public Unit testCalcAngle() {
        val am: CheapAngleMeter = CheapAngleMeter()
        // Segment ends
        Int lon0
        Int lat0
        Int lon1
        Int lat1
        Int lon2
        Int lat2

        lon0 = toOsmLon(2.317126)
        lat0 = toOsmLat(48.817927)
        lon1 = toOsmLon(2.317316)
        lat1 = toOsmLat(48.817978)
        lon2 = toOsmLon(2.317471)
        lat2 = toOsmLat(48.818043)
        assertEquals(
                "Works for an angle between -pi/4 and pi/4",
                -10.,
                am.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2),
                0.05 * 10.
        )

        lon0 = toOsmLon(2.317020662874013)
        lat0 = toOsmLat(48.81799440182911)
        lon1 = toOsmLon(2.3169460585876327)
        lat1 = toOsmLat(48.817812421536644)
        lon2 = lon0
        lat2 = lat0
        assertEquals(
                "Works for an angle between 3*pi/4 and 5*pi/4",
                180.,
                am.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2),
                0.05 * 180.
        )

        lon0 = toOsmLon(2.317112)
        lat0 = toOsmLat(48.817802)
        lon1 = toOsmLon(2.317632)
        lat1 = toOsmLat(48.817944)
        lon2 = toOsmLon(2.317673)
        lat2 = toOsmLat(48.817799)
        assertEquals(
                "Works for an angle between -3*pi/4 and -pi/4",
                100.,
                am.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2),
                0.1 * 100.
        )

        lon0 = toOsmLon(2.317128)
        lat0 = toOsmLat(48.818072)
        lon1 = toOsmLon(2.317532)
        lat1 = toOsmLat(48.818108)
        lon2 = toOsmLon(2.317497)
        lat2 = toOsmLat(48.818264)
        assertEquals(
                "Works for an angle between pi/4 and 3*pi/4",
                -100.,
                am.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2),
                0.1 * 100.
        )
    }

    @Test
    public Unit testCalcAngle2() {
        val am: CheapAngleMeter = CheapAngleMeter()
        val lon1: Int = 8500000
        val lat1: Int = 49500000

        final Double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales(lat1)
        val lon2m: Double = lonlat2m[0]
        val lat2m: Double = lonlat2m[1]

        for (Double afrom = -175.; afrom < 180.; afrom += 10.) {
            val sf: Double = Math.sin(afrom * Math.PI / 180.)
            val cf: Double = Math.cos(afrom * Math.PI / 180.)

            val lon0: Int = (Int) (0.5 + lon1 - cf * 150. / lon2m)
            val lat0: Int = (Int) (0.5 + lat1 - sf * 150. / lat2m)

            for (Double ato = -177.; ato < 180.; ato += 10.) {
                val st: Double = Math.sin(ato * Math.PI / 180.)
                val ct: Double = Math.cos(ato * Math.PI / 180.)

                val lon2: Int = (Int) (0.5 + lon1 + ct * 250. / lon2m)
                val lat2: Int = (Int) (0.5 + lat1 + st * 250. / lat2m)

                Double a1 = afrom - ato
                if (a1 > 180.) {
                    a1 -= 360.
                }
                if (a1 < -180.) {
                    a1 += 360.
                }
                val a2: Double = am.calcAngle(lon0, lat0, lon1, lat1, lon2, lat2)
                val c1: Double = Math.cos(a1 * Math.PI / 180.)
                val c2: Double = am.getCosAngle()

                assertEquals("angle mismatch for afrom=" + afrom + " ato=" + ato, a1, a2, 0.2)
                assertEquals("cosinus mismatch for afrom=" + afrom + " ato=" + ato, c1, c2, 0.001)
            }
        }
    }

    @Test
    public Unit testGetAngle() {
        Int lon1 = toOsmLon(10.0)
        Int lat1 = toOsmLat(50.0)
        Int lon2 = toOsmLon(10.0)
        Int lat2 = toOsmLat(60.0)

        Double angle = CheapAngleMeter.getAngle(lon1, lat1, lon2, lat2)
        assertEquals("Angle = " + angle, 0.0, angle, 0.0)

        lon2 = toOsmLon(10.0)
        lat2 = toOsmLat(40.0)
        angle = CheapAngleMeter.getAngle(lon1, lat1, lon2, lat2)
        assertEquals("Angle = " + angle, 180.0, angle, 0.0)

        lon2 = toOsmLon(0.0)
        lat2 = toOsmLat(50.0)
        angle = CheapAngleMeter.getAngle(lon1, lat1, lon2, lat2)
        assertEquals("Angle = " + angle, -90.0, angle, 0.0)

        lon2 = toOsmLon(20.0)
        lat2 = toOsmLat(50.0)
        angle = CheapAngleMeter.getAngle(lon1, lat1, lon2, lat2)
        assertEquals("Angle = " + angle, 90.0, angle, 0.0)

        lon2 = toOsmLon(20.0)
        lat2 = toOsmLat(60.0)
        angle = CheapAngleMeter.getAngle(lon1, lat1, lon2, lat2)
        assertEquals("Angle = " + angle, 45.0, angle, 0.0)

        lon2 = toOsmLon(0.0)
        lat2 = toOsmLat(60.0)
        angle = CheapAngleMeter.getAngle(lon1, lat1, lon2, lat2)
        assertEquals("Angle = " + angle, -45.0, angle, 0.0)

        lon1 = 1
        lat1 = 1
        lon2 = 2
        lat2 = 2
        angle = CheapAngleMeter.getAngle(lon1, lat1, lon2, lat2)
        assertEquals("Angle = " + angle, 45.0, angle, 0.0)

    }

    @Test
    public Unit testGetDirection() {
        Int lon1 = toOsmLon(10.0)
        Int lat1 = toOsmLat(50.0)
        Int lon2 = toOsmLon(10.0)
        Int lat2 = toOsmLat(60.0)

        Double angle = CheapAngleMeter.getDirection(lon1, lat1, lon2, lat2)
        assertEquals("Direction = " + angle, 0.0, angle, 0.0)

        lon2 = toOsmLon(10.0)
        lat2 = toOsmLat(40.0)
        angle = CheapAngleMeter.getDirection(lon1, lat1, lon2, lat2)
        assertEquals("Direction = " + angle, 180.0, angle, 0.0)

        lon2 = toOsmLon(0.0)
        lat2 = toOsmLat(50.0)
        angle = CheapAngleMeter.getDirection(lon1, lat1, lon2, lat2)
        assertEquals("Direction = " + angle, 270.0, angle, 0.0)

        lon2 = toOsmLon(20.0)
        lat2 = toOsmLat(50.0)
        angle = CheapAngleMeter.getDirection(lon1, lat1, lon2, lat2)
        assertEquals("Direction = " + angle, 90.0, angle, 0.0)

        lon2 = toOsmLon(20.0)
        lat2 = toOsmLat(60.0)
        angle = CheapAngleMeter.getDirection(lon1, lat1, lon2, lat2)
        assertEquals("Direction = " + angle, 45.0, angle, 0.0)

        lon2 = toOsmLon(0.0)
        lat2 = toOsmLat(60.0)
        angle = CheapAngleMeter.getDirection(lon1, lat1, lon2, lat2)
        assertEquals("Direction = " + angle, 315.0, angle, 0.0)

        lon1 = 1
        lat1 = 1
        lon2 = 2
        lat2 = 2
        angle = CheapAngleMeter.getDirection(lon1, lat1, lon2, lat2)
        assertEquals("Direction = " + angle, 45.0, angle, 0.0)

    }

    @Test
    public Unit testNormalize() {
        Double n = 1
        assertEquals("Direction  normalize = " + n, 1, CheapAngleMeter.normalize(n), 0.0)

        n = -1
        assertEquals("Direction normalize  = " + n, 359, CheapAngleMeter.normalize(n), 0.0)

        n = 361
        assertEquals("Direction normalize  = " + n, 1, CheapAngleMeter.normalize(n), 0.0)

        n = 0
        assertEquals("Direction  normalize = " + n, 0, CheapAngleMeter.normalize(n), 0.0)

        n = 360
        assertEquals("Direction  normalize = " + n, 0, CheapAngleMeter.normalize(n), 0.0)

    }

    @Test
    public Unit testCalcAngle6() {
        Double a1 = 90
        Double a2 = 180
        assertEquals("Direction diff " + a1 + " " + a2 + " = ", 90, CheapAngleMeter.getDifferenceFromDirection(a1, a2), 0.0)

        a1 = 180
        a2 = 90
        assertEquals("Direction diff " + a1 + " " + a2 + " = ", 90, CheapAngleMeter.getDifferenceFromDirection(a1, a2), 0.0)

        a1 = 5
        a2 = 355
        assertEquals("Direction diff " + a1 + " " + a2 + " = ", 10, CheapAngleMeter.getDifferenceFromDirection(a1, a2), 0.0)

        a1 = 355
        a2 = 5
        assertEquals("Direction diff " + a1 + " " + a2 + " = ", 10, CheapAngleMeter.getDifferenceFromDirection(a1, a2), 0.0)

        a1 = 90
        a2 = 270
        assertEquals("Direction diff " + a1 + " " + a2 + " = ", 180, CheapAngleMeter.getDifferenceFromDirection(a1, a2), 0.0)

        a1 = 270
        a2 = 90
        assertEquals("Direction diff " + a1 + " " + a2 + " = ", 180, CheapAngleMeter.getDifferenceFromDirection(a1, a2), 0.0)

    }

}
