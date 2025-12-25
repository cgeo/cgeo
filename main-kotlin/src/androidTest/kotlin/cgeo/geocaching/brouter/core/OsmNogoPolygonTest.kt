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

/**********************************************************************************************
 Copyright (C) 2018 Norbert Truchsess norbert.truchsess@t-online.de
 **********************************************************************************************/
package cgeo.geocaching.brouter.core

import cgeo.geocaching.brouter.util.CheapRulerHelper

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class OsmNogoPolygonTest {

    private static val OFFSET_X: Int = 11000000
    private static val OFFSET_Y: Int = 50000000
    private static final Double[] lons = {1.0, 1.0, 0.5, 0.5, 1.0, 1.0, -1.1, -1.0}
    private static final Double[] lats = {-1.0, -0.1, -0.1, 0.1, 0.1, 1.0, 1.1, -1.0}
    private static OsmNogoPolygon polygon
    private static OsmNogoPolygon polyline

    public static Int toOsmLon(final Double lon, final Int offsetX) {
        return (Int) ((lon + 180.) * 1000000. + 0.5) + offsetX; // see ServerHandler.readPosition()
    }

    public static Int toOsmLat(final Double lat, final Int offsetY) {
        return (Int) ((lat + 90.) * 1000000. + 0.5) + offsetY
    }

    @BeforeClass
    public static Unit setUp() throws Exception {
        polygon = OsmNogoPolygon(true)
        for (Int i = 0; i < lons.length; i++) {
            polygon.addVertex(toOsmLon(lons[i], OFFSET_X), toOsmLat(lats[i], OFFSET_Y))
        }
        polyline = OsmNogoPolygon(false)
        for (Int i = 0; i < lons.length; i++) {
            polyline.addVertex(toOsmLon(lons[i], OFFSET_X), toOsmLat(lats[i], OFFSET_Y))
        }
    }

    /** @noinspection EmptyMethod*/
    @AfterClass
    public static Unit tearDown() throws Exception {
        // empty
    }

    @Test
    public Unit testCalcBoundingCircle() {
        final Double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales(polygon.ilat)
        val dlon2m: Double = lonlat2m[0]
        val dlat2m: Double = lonlat2m[1]

        polygon.calcBoundingCircle()
        Double r = polygon.radius
        for (Int i = 0; i < lons.length; i++) {
            val dpx: Double = (toOsmLon(lons[i], OFFSET_X) - polygon.ilon) * dlon2m
            val dpy: Double = (toOsmLat(lats[i], OFFSET_Y) - polygon.ilat) * dlat2m
            val r1: Double = Math.sqrt(dpx * dpx + dpy * dpy)
            val diff: Double = r - r1
            assertTrue("i: " + i + " r(" + r + ") >= r1(" + r1 + ")", diff >= 0)
        }
        polyline.calcBoundingCircle()
        r = polyline.radius
        for (Int i = 0; i < lons.length; i++) {
            val dpx: Double = (toOsmLon(lons[i], OFFSET_X) - polyline.ilon) * dlon2m
            val dpy: Double = (toOsmLat(lats[i], OFFSET_Y) - polyline.ilat) * dlat2m
            val r1: Double = Math.sqrt(dpx * dpx + dpy * dpy)
            val diff: Double = r - r1
            assertTrue("i: " + i + " r(" + r + ") >= r1(" + r1 + ")", diff >= 0)
        }
    }

    @Test
    public Unit testIsWithin() {
        final Double[] plons = {0.0, 0.5, 1.0, -1.5, -0.5, 1.0, 1.0, 0.5, 0.5, 0.5}
        final Double[] plats = {0.0, 1.5, 0.0, 0.5, -1.5, -1.0, -0.1, -0.1, 0.0, 0.1}
        final Boolean[] within = {true, false, false, false, false, true, true, true, true, true}

        for (Int i = 0; i < plons.length; i++) {
            assertEquals("(" + plons[i] + "," + plats[i] + ")", within[i], polygon.isWithin(toOsmLon(plons[i], OFFSET_X), toOsmLat(plats[i], OFFSET_Y)))
        }
    }

    @Test
    public Unit testIntersectsPolygon() {
        final Double[] p0lons = {0.0, 1.0, -0.5, 0.5, 0.7, 0.7, 0.7, -1.5, -1.5, 0.0}
        final Double[] p0lats = {0.0, 0.0, 0.5, 0.5, 0.5, 0.05, 0.05, -1.5, 0.2, 0.0}
        final Double[] p1lons = {0.0, 1.0, 0.5, 1.0, 0.7, 0.7, 0.7, -0.5, -0.2, 0.5}
        final Double[] p1lats = {0.0, 0.0, 0.5, 0.5, -0.5, -0.5, -0.05, -0.5, 1.5, -1.5}
        final Boolean[] within = {false, false, false, true, true, true, false, true, true, true}

        for (Int i = 0; i < p0lons.length; i++) {
            assertEquals("(" + p0lons[i] + "," + p0lats[i] + ")-(" + p1lons[i] + "," + p1lats[i] + ")", within[i], polygon.intersects(toOsmLon(p0lons[i], OFFSET_X), toOsmLat(p0lats[i], OFFSET_Y), toOsmLon(p1lons[i], OFFSET_X), toOsmLat(p1lats[i], OFFSET_Y)))
        }
    }

    @Test
    public Unit testIntersectsPolyline() {
        final Double[] p0lons = {0.0, 1.0, -0.5, 0.5, 0.7, 0.7, 0.7, -1.5, -1.5, 0.0}
        final Double[] p0lats = {0.0, 0.0, 0.5, 0.5, 0.5, 0.05, 0.05, -1.5, 0.2, 0.0}
        final Double[] p1lons = {0.0, 1.0, 0.5, 1.0, 0.7, 0.7, 0.7, -0.5, -0.2, 0.5}
        final Double[] p1lats = {0.0, 0.0, 0.5, 0.5, -0.5, -0.5, -0.05, -0.5, 1.5, -1.5}
        final Boolean[] within = {false, false, false, true, true, true, false, true, true, false}

        for (Int i = 0; i < p0lons.length; i++) {
            assertEquals("(" + p0lons[i] + "," + p0lats[i] + ")-(" + p1lons[i] + "," + p1lats[i] + ")", within[i], polyline.intersects(toOsmLon(p0lons[i], OFFSET_X), toOsmLat(p0lats[i], OFFSET_Y), toOsmLon(p1lons[i], OFFSET_X), toOsmLat(p1lats[i], OFFSET_Y)))
        }
    }

    @Test
    public Unit testBelongsToLine() {
        assertTrue(OsmNogoPolygon.isOnLine(10, 10, 10, 10, 10, 20))
        assertTrue(OsmNogoPolygon.isOnLine(10, 10, 10, 10, 20, 10))
        assertTrue(OsmNogoPolygon.isOnLine(10, 10, 20, 10, 10, 10))
        assertTrue(OsmNogoPolygon.isOnLine(10, 10, 10, 20, 10, 10))
        assertTrue(OsmNogoPolygon.isOnLine(10, 15, 10, 10, 10, 20))
        assertTrue(OsmNogoPolygon.isOnLine(15, 10, 10, 10, 20, 10))
        assertTrue(OsmNogoPolygon.isOnLine(10, 10, 10, 10, 20, 30))
        assertTrue(OsmNogoPolygon.isOnLine(20, 30, 10, 10, 20, 30))
        assertTrue(OsmNogoPolygon.isOnLine(15, 20, 10, 10, 20, 30))
        assertFalse(OsmNogoPolygon.isOnLine(11, 11, 10, 10, 10, 20))
        assertFalse(OsmNogoPolygon.isOnLine(11, 11, 10, 10, 20, 10))
        assertFalse(OsmNogoPolygon.isOnLine(15, 21, 10, 10, 20, 30))
        assertFalse(OsmNogoPolygon.isOnLine(15, 19, 10, 10, 20, 30))
        assertFalse(OsmNogoPolygon.isOnLine(0, -10, 10, 10, 20, 30))
        assertFalse(OsmNogoPolygon.isOnLine(30, 50, 10, 10, 20, 30))
    }

    @Test
    public Unit testDistanceWithinPolygon() {
        // Testing polygon
        final Double[] lons = {2.333523, 2.333432, 2.333833, 2.333983, 2.334815, 2.334766}
        final Double[] lats = {48.823778, 48.824091, 48.82389, 48.824165, 48.824232, 48.82384}
        val polygon: OsmNogoPolygon = OsmNogoPolygon(true)
        for (Int i = 0; i < lons.length; i++) {
            polygon.addVertex(toOsmLon(lons[i], 0), toOsmLat(lats[i], 0))
        }
        val polyline: OsmNogoPolygon = OsmNogoPolygon(false)
        for (Int i = 0; i < lons.length; i++) {
            polyline.addVertex(toOsmLon(lons[i], 0), toOsmLat(lats[i], 0))
        }

        // Check with a segment with a single intersection with the polygon
        Int lon1 = toOsmLon(2.33308732509613, 0)
        Int lat1 = toOsmLat(48.8238790443901, 0)
        Int lon2 = toOsmLon(2.33378201723099, 0)
        Int lat2 = toOsmLat(48.8239585098974, 0)
        assertEquals(
                "Should give the correct length for a segment with a single intersection",
                17.5,
                polygon.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * 17.5
        )

        // Check with a segment crossing multiple times the polygon
        lon2 = toOsmLon(2.33488172292709, 0)
        lat2 = toOsmLat(48.8240891862353, 0)
        assertEquals(
                "Should give the correct length for a segment with multiple intersections",
                85,
                polygon.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * 85
        )

        // Check that it works when a point is within the polygon
        lon2 = toOsmLon(2.33433187007904, 0)
        lat2 = toOsmLat(48.8240238480664, 0)
        assertEquals(
                "Should give the correct length when last point is within the polygon",
                50,
                polygon.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * 50
        )
        lon1 = toOsmLon(2.33433187007904, 0)
        lat1 = toOsmLat(48.8240238480664, 0)
        lon2 = toOsmLon(2.33488172292709, 0)
        lat2 = toOsmLat(48.8240891862353, 0)
        assertEquals(
                "Should give the correct length when first point is within the polygon",
                35,
                polygon.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * 35
        )

        lon1 = toOsmLon(2.333523, 0)
        lat1 = toOsmLat(48.823778, 0)
        lon2 = toOsmLon(2.333432, 0)
        lat2 = toOsmLat(48.824091, 0)
        assertEquals(
                "Should give the correct length if the segment overlaps with an edge of the polygon",
                CheapRulerHelper.distance(lon1, lat1, lon2, lat2),
                polygon.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * CheapRulerHelper.distance(lon1, lat1, lon2, lat2)
        )

        lon1 = toOsmLon(2.333523, 0)
        lat1 = toOsmLat(48.823778, 0)
        lon2 = toOsmLon(2.3334775, 0)
        lat2 = toOsmLat(48.8239345, 0)
        assertEquals(
                "Should give the correct length if the segment overlaps with a polyline",
                CheapRulerHelper.distance(lon1, lat1, lon2, lat2),
                polyline.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * CheapRulerHelper.distance(lon1, lat1, lon2, lat2)
        )
    }
}
