/**********************************************************************************************
 Copyright (C) 2018 Norbert Truchsess norbert.truchsess@t-online.de
 **********************************************************************************************/
package cgeo.geocaching.brouter.core;

import cgeo.geocaching.brouter.util.CheapRulerHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OsmNogoPolygonTest {

    private static final int OFFSET_X = 11000000;
    private static final int OFFSET_Y = 50000000;
    private static final double[] lons = {1.0, 1.0, 0.5, 0.5, 1.0, 1.0, -1.1, -1.0};
    private static final double[] lats = {-1.0, -0.1, -0.1, 0.1, 0.1, 1.0, 1.1, -1.0};
    private static OsmNogoPolygon polygon;
    private static OsmNogoPolygon polyline;

    public static int toOsmLon(final double lon, final int offsetX) {
        return (int) ((lon + 180.) * 1000000. + 0.5) + offsetX; // see ServerHandler.readPosition()
    }

    public static int toOsmLat(final double lat, final int offsetY) {
        return (int) ((lat + 90.) * 1000000. + 0.5) + offsetY;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        polygon = new OsmNogoPolygon(true);
        for (int i = 0; i < lons.length; i++) {
            polygon.addVertex(toOsmLon(lons[i], OFFSET_X), toOsmLat(lats[i], OFFSET_Y));
        }
        polyline = new OsmNogoPolygon(false);
        for (int i = 0; i < lons.length; i++) {
            polyline.addVertex(toOsmLon(lons[i], OFFSET_X), toOsmLat(lats[i], OFFSET_Y));
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // empty
    }

    @Test
    public void testCalcBoundingCircle() {
        final double[] lonlat2m = CheapRulerHelper.getLonLatToMeterScales(polygon.ilat);
        final double dlon2m = lonlat2m[0];
        final double dlat2m = lonlat2m[1];

        polygon.calcBoundingCircle();
        double r = polygon.radius;
        for (int i = 0; i < lons.length; i++) {
            final double dpx = (toOsmLon(lons[i], OFFSET_X) - polygon.ilon) * dlon2m;
            final double dpy = (toOsmLat(lats[i], OFFSET_Y) - polygon.ilat) * dlat2m;
            final double r1 = Math.sqrt(dpx * dpx + dpy * dpy);
            final double diff = r - r1;
            assertTrue("i: " + i + " r(" + r + ") >= r1(" + r1 + ")", diff >= 0);
        }
        polyline.calcBoundingCircle();
        r = polyline.radius;
        for (int i = 0; i < lons.length; i++) {
            final double dpx = (toOsmLon(lons[i], OFFSET_X) - polyline.ilon) * dlon2m;
            final double dpy = (toOsmLat(lats[i], OFFSET_Y) - polyline.ilat) * dlat2m;
            final double r1 = Math.sqrt(dpx * dpx + dpy * dpy);
            final double diff = r - r1;
            assertTrue("i: " + i + " r(" + r + ") >= r1(" + r1 + ")", diff >= 0);
        }
    }

    @Test
    public void testIsWithin() {
        final double[] plons = {0.0, 0.5, 1.0, -1.5, -0.5, 1.0, 1.0, 0.5, 0.5, 0.5};
        final double[] plats = {0.0, 1.5, 0.0, 0.5, -1.5, -1.0, -0.1, -0.1, 0.0, 0.1};
        final boolean[] within = {true, false, false, false, false, true, true, true, true, true};

        for (int i = 0; i < plons.length; i++) {
            assertEquals("(" + plons[i] + "," + plats[i] + ")", within[i], polygon.isWithin(toOsmLon(plons[i], OFFSET_X), toOsmLat(plats[i], OFFSET_Y)));
        }
    }

    @Test
    public void testIntersectsPolygon() {
        final double[] p0lons = {0.0, 1.0, -0.5, 0.5, 0.7, 0.7, 0.7, -1.5, -1.5, 0.0};
        final double[] p0lats = {0.0, 0.0, 0.5, 0.5, 0.5, 0.05, 0.05, -1.5, 0.2, 0.0};
        final double[] p1lons = {0.0, 1.0, 0.5, 1.0, 0.7, 0.7, 0.7, -0.5, -0.2, 0.5};
        final double[] p1lats = {0.0, 0.0, 0.5, 0.5, -0.5, -0.5, -0.05, -0.5, 1.5, -1.5};
        final boolean[] within = {false, false, false, true, true, true, false, true, true, true};

        for (int i = 0; i < p0lons.length; i++) {
            assertEquals("(" + p0lons[i] + "," + p0lats[i] + ")-(" + p1lons[i] + "," + p1lats[i] + ")", within[i], polygon.intersects(toOsmLon(p0lons[i], OFFSET_X), toOsmLat(p0lats[i], OFFSET_Y), toOsmLon(p1lons[i], OFFSET_X), toOsmLat(p1lats[i], OFFSET_Y)));
        }
    }

    @Test
    public void testIntersectsPolyline() {
        final double[] p0lons = {0.0, 1.0, -0.5, 0.5, 0.7, 0.7, 0.7, -1.5, -1.5, 0.0};
        final double[] p0lats = {0.0, 0.0, 0.5, 0.5, 0.5, 0.05, 0.05, -1.5, 0.2, 0.0};
        final double[] p1lons = {0.0, 1.0, 0.5, 1.0, 0.7, 0.7, 0.7, -0.5, -0.2, 0.5};
        final double[] p1lats = {0.0, 0.0, 0.5, 0.5, -0.5, -0.5, -0.05, -0.5, 1.5, -1.5};
        final boolean[] within = {false, false, false, true, true, true, false, true, true, false};

        for (int i = 0; i < p0lons.length; i++) {
            assertEquals("(" + p0lons[i] + "," + p0lats[i] + ")-(" + p1lons[i] + "," + p1lats[i] + ")", within[i], polyline.intersects(toOsmLon(p0lons[i], OFFSET_X), toOsmLat(p0lats[i], OFFSET_Y), toOsmLon(p1lons[i], OFFSET_X), toOsmLat(p1lats[i], OFFSET_Y)));
        }
    }

    @Test
    public void testBelongsToLine() {
        assertTrue(OsmNogoPolygon.isOnLine(10, 10, 10, 10, 10, 20));
        assertTrue(OsmNogoPolygon.isOnLine(10, 10, 10, 10, 20, 10));
        assertTrue(OsmNogoPolygon.isOnLine(10, 10, 20, 10, 10, 10));
        assertTrue(OsmNogoPolygon.isOnLine(10, 10, 10, 20, 10, 10));
        assertTrue(OsmNogoPolygon.isOnLine(10, 15, 10, 10, 10, 20));
        assertTrue(OsmNogoPolygon.isOnLine(15, 10, 10, 10, 20, 10));
        assertTrue(OsmNogoPolygon.isOnLine(10, 10, 10, 10, 20, 30));
        assertTrue(OsmNogoPolygon.isOnLine(20, 30, 10, 10, 20, 30));
        assertTrue(OsmNogoPolygon.isOnLine(15, 20, 10, 10, 20, 30));
        assertFalse(OsmNogoPolygon.isOnLine(11, 11, 10, 10, 10, 20));
        assertFalse(OsmNogoPolygon.isOnLine(11, 11, 10, 10, 20, 10));
        assertFalse(OsmNogoPolygon.isOnLine(15, 21, 10, 10, 20, 30));
        assertFalse(OsmNogoPolygon.isOnLine(15, 19, 10, 10, 20, 30));
        assertFalse(OsmNogoPolygon.isOnLine(0, -10, 10, 10, 20, 30));
        assertFalse(OsmNogoPolygon.isOnLine(30, 50, 10, 10, 20, 30));
    }

    @Test
    public void testDistanceWithinPolygon() {
        // Testing polygon
        final double[] lons = {2.333523, 2.333432, 2.333833, 2.333983, 2.334815, 2.334766};
        final double[] lats = {48.823778, 48.824091, 48.82389, 48.824165, 48.824232, 48.82384};
        final OsmNogoPolygon polygon = new OsmNogoPolygon(true);
        for (int i = 0; i < lons.length; i++) {
            polygon.addVertex(toOsmLon(lons[i], 0), toOsmLat(lats[i], 0));
        }
        final OsmNogoPolygon polyline = new OsmNogoPolygon(false);
        for (int i = 0; i < lons.length; i++) {
            polyline.addVertex(toOsmLon(lons[i], 0), toOsmLat(lats[i], 0));
        }

        // Check with a segment with a single intersection with the polygon
        int lon1 = toOsmLon(2.33308732509613, 0);
        int lat1 = toOsmLat(48.8238790443901, 0);
        int lon2 = toOsmLon(2.33378201723099, 0);
        int lat2 = toOsmLat(48.8239585098974, 0);
        assertEquals(
                "Should give the correct length for a segment with a single intersection",
                17.5,
                polygon.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * 17.5
        );

        // Check with a segment crossing multiple times the polygon
        lon2 = toOsmLon(2.33488172292709, 0);
        lat2 = toOsmLat(48.8240891862353, 0);
        assertEquals(
                "Should give the correct length for a segment with multiple intersections",
                85,
                polygon.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * 85
        );

        // Check that it works when a point is within the polygon
        lon2 = toOsmLon(2.33433187007904, 0);
        lat2 = toOsmLat(48.8240238480664, 0);
        assertEquals(
                "Should give the correct length when last point is within the polygon",
                50,
                polygon.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * 50
        );
        lon1 = toOsmLon(2.33433187007904, 0);
        lat1 = toOsmLat(48.8240238480664, 0);
        lon2 = toOsmLon(2.33488172292709, 0);
        lat2 = toOsmLat(48.8240891862353, 0);
        assertEquals(
                "Should give the correct length when first point is within the polygon",
                35,
                polygon.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * 35
        );

        lon1 = toOsmLon(2.333523, 0);
        lat1 = toOsmLat(48.823778, 0);
        lon2 = toOsmLon(2.333432, 0);
        lat2 = toOsmLat(48.824091, 0);
        assertEquals(
                "Should give the correct length if the segment overlaps with an edge of the polygon",
                CheapRulerHelper.distance(lon1, lat1, lon2, lat2),
                polygon.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * CheapRulerHelper.distance(lon1, lat1, lon2, lat2)
        );

        lon1 = toOsmLon(2.333523, 0);
        lat1 = toOsmLat(48.823778, 0);
        lon2 = toOsmLon(2.3334775, 0);
        lat2 = toOsmLat(48.8239345, 0);
        assertEquals(
                "Should give the correct length if the segment overlaps with a polyline",
                CheapRulerHelper.distance(lon1, lat1, lon2, lat2),
                polyline.distanceWithinPolygon(lon1, lat1, lon2, lat2),
                0.05 * CheapRulerHelper.distance(lon1, lat1, lon2, lat2)
        );
    }
}
