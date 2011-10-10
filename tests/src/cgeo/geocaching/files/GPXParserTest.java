package cgeo.geocaching.files;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgLog;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.R;

import android.content.res.Resources;
import android.os.Handler;
import android.test.InstrumentationTestCase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GPXParserTest extends InstrumentationTestCase {
    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // 2010-04-20T07:00:00Z

    public void testGPXVersion100() throws Exception {
        testGPXVersion(R.raw.gc1bkp3_gpx100);
    }

    private cgCache testGPXVersion(final int resourceId) throws IOException {
        final List<cgCache> caches = readGPX(resourceId);
        assertNotNull(caches);
        assertEquals(1, caches.size());
        final cgCache cache = caches.get(0);
        assertEquals("GC1BKP3", cache.geocode);
        assertEquals("9946f030-a514-46d8-a050-a60e92fd2e1a", cache.guid);
        assertEquals("traditional", cache.type);
        assertEquals(false, cache.archived);
        assertEquals(false, cache.disabled);
        assertEquals("Die Schatzinsel / treasure island", cache.name);
        assertEquals("Die unbesiegbaren Geo - Geparden", cache.owner);
        assertEquals(CacheSize.MICRO, cache.size);
        assertEquals(1.0f, cache.difficulty.floatValue());
        assertEquals(5.0f, cache.terrain.floatValue());
        assertEquals("Baden-Württemberg, Germany", cache.location);
        assertEquals("Ein alter Kindheitstraum, ein Schatz auf einer unbewohnten Insel.\nA old dream of my childhood, a treasure on a lonely island.", cache.shortdesc);
        assertTrue(new Geopoint(48.859683, 9.1874).isEqualTo(cache.coords));
        return cache;
    }

    public void testGPXVersion101() throws IOException {
        final cgCache cache = testGPXVersion(R.raw.gc1bkp3_gpx101);
        assertNotNull(cache.attributes);
        assertEquals(10, cache.attributes.size());
    }

    public void testOC() throws IOException {
        final List<cgCache> caches = readGPX(R.raw.oc5952_gpx);
        final cgCache cache = caches.get(0);
        assertEquals("OC5952", cache.geocode);
        assertEquals("traditional", cache.type);
        assertEquals(false, cache.archived);
        assertEquals(false, cache.disabled);
        assertEquals("Die Schatzinsel / treasure island", cache.name);
        assertEquals("Die unbesiegbaren Geo - Geparden", cache.owner);
        assertEquals(CacheSize.SMALL, cache.size);
        assertEquals(1.0f, cache.difficulty.floatValue());
        assertEquals(4.0f, cache.terrain.floatValue());
        assertEquals("Baden-Württemberg, Germany", cache.location);
        assertEquals("Ein alter Kindheitstraum, ein Schatz auf einer unbewohnten Insel. A old dream of my childhood, a treasure on a lonely is", cache.shortdesc);
        assertTrue(new Geopoint(48.85968, 9.18740).isEqualTo(cache.coords));
    }

    public void testGc31j2h() throws IOException {
        final List<cgCache> caches = readGPX(R.raw.gc31j2h);
        assertEquals(1, caches.size());
        final cgCache cache = caches.get(0);

        assertGc31j2h(cache);
        assertSame(cache, caches.get(0));

        // no waypoints without importing waypoint file
        assertNull(cache.waypoints);
    }

    public void testGc31j2hWpts() throws IOException {
        List<cgCache> caches = readGPX(R.raw.gc31j2h, R.raw.gc31j2h_wpts);
        assertEquals(1, caches.size());
        cgCache cache = caches.get(0);
        assertGc31j2h(cache);
        assertGc31j2hWaypoints(cache);
    }

    public void testGc31j2hWptsWithoutCache() throws IOException {
        final List<cgCache> caches = readGPX(R.raw.gc31j2h_wpts);
        assertEquals(0, caches.size());
    }

    public static void testConvertWaypointSym2Type() {
        assertEquals(WaypointType.WAYPOINT, GPXParser.convertWaypointSym2Type("unknown sym"));

        assertEquals(WaypointType.PKG, GPXParser.convertWaypointSym2Type("Parking area"));
        assertEquals(WaypointType.STAGE, GPXParser.convertWaypointSym2Type("Stages of a multicache"));
        assertEquals(WaypointType.PUZZLE, GPXParser.convertWaypointSym2Type("Question to answer"));
        assertEquals(WaypointType.TRAILHEAD, GPXParser.convertWaypointSym2Type("Trailhead"));
        assertEquals(WaypointType.FLAG, GPXParser.convertWaypointSym2Type("Final location"));
        assertEquals(WaypointType.WAYPOINT, GPXParser.convertWaypointSym2Type("Reference point"));
    }

    private static void assertGc31j2h(final cgCache cache) {
        assertEquals("GC31J2H", cache.getGeocode());
        assertEquals("Hockenheimer City-Brunnen", cache.getName());
        assertTrue("Hockenheimer City-Brunnen by vptsz, Multi-cache (2/1)", cache.getShortDescription().startsWith("Kurzer informativer Multi entlang der Brunnen"));
        assertTrue(cache.getDescription().startsWith("Cachemobile können kostenfrei am Messplatz geparkt werden."));
        assertTrue(cache.hasTrackables());
        assertEquals(2.0f, cache.getDifficulty().floatValue(), 0.01f);
        assertEquals(1.0f, cache.getTerrain().floatValue(), 0.01f);
        final Geopoint refCoordinates = new Geopoint("N 49° 19.122", "E 008° 32.739");
        assertEquals(cgBase.formatLatitude(refCoordinates.getLatitude(), true), cache.getLatitude());
        assertEquals(cgBase.formatLongitude(refCoordinates.getLongitude(), true), cache.getLongitude());
        assertEquals("vptsz", cache.getOwner());
        assertEquals(CacheSize.SMALL, cache.getSize());
        assertEquals("multi", cache.getType());
        assertFalse(cache.isArchived());
        assertFalse(cache.isDisabled());
        assertFalse(cache.isEventCache());
        assertFalse(cache.isMembersOnly());
        assertFalse(cache.isOwn());
        assertTrue(cache.found);
        assertEquals("Station3: Der zerbrochene Stein zählt doppelt.\nFinal: Oben neben dem Tor", cache.getHint());
        // logs
        assertEquals(6, cache.logs.size());
        final cgLog log = cache.logs.get(5);
        assertEquals("Geoteufel", log.author);
        assertEquals(parseTime("2011-09-11T07:00:00Z"), log.date);
        assertEquals(-1, log.found);
        assertEquals("Sehr schöne Runde und wir haben wieder etwas Neues über Hockenheim gelernt. Super Tarnung.\nTFTC, Geoteufel", log.log);

        // following info is not contained in pocket query gpx file
        assertNull(cache.attributes);
    }

    private static long parseTime(final String time) {
        try {
            return LOG_DATE_FORMAT.parse(time).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }

    private static void assertGc31j2hWaypoints(final cgCache cache) {
        assertNotNull(cache.waypoints);
        assertEquals(2, cache.waypoints.size());
        cgWaypoint wp = cache.waypoints.get(0);
        assertEquals("GC31J2H", wp.geocode);
        assertEquals("00", wp.getPrefix());
        assertEquals("---", wp.lookup);
        assertEquals("Parkplatz", wp.name);
        assertEquals("Kostenfreies Parken (je nach Parkreihe Parkscheibe erforderlich)", wp.note);
        assertEquals(WaypointType.PKG, wp.typee);
        assertEquals(49.317517, wp.coords.getLatitude(), 0.000001);
        assertEquals(8.545083, wp.coords.getLongitude(), 0.000001);

        wp = cache.waypoints.get(1);
        assertEquals("GC31J2H", wp.geocode);
        assertEquals("S1", wp.getPrefix());
        assertEquals("---", wp.lookup);
        assertEquals("Station 1", wp.name);
        assertEquals("Ein zweiter Wegpunkt, der nicht wirklich existiert sondern nur zum Testen gedacht ist.", wp.note);
        assertEquals(WaypointType.STAGE, wp.typee);
        assertEquals(49.317500, wp.coords.getLatitude(), 0.000001);
        assertEquals(8.545100, wp.coords.getLongitude(), 0.000001);
    }

    public static void testGetWaypointsFileForGpx() {
        assertEquals(new File("1234567-wpts.gpx"), GPXParser.getWaypointsFileForGpx(new File("1234567.gpx")));
        assertEquals(new File("/mnt/sdcard/1234567-wpts.gpx"), GPXParser.getWaypointsFileForGpx(new File("/mnt/sdcard/1234567.gpx")));
        assertEquals(new File("/mnt/sdcard/1-wpts.gpx"), GPXParser.getWaypointsFileForGpx(new File("/mnt/sdcard/1.gpx")));
        assertEquals(new File("/mnt/sd.card/1-wpts.gpx"), GPXParser.getWaypointsFileForGpx(new File("/mnt/sd.card/1.gpx")));
        assertEquals(new File("1234567.9-wpts.gpx"), GPXParser.getWaypointsFileForGpx(new File("1234567.9.gpx")));
        assertEquals(new File("1234567-wpts.GPX"), GPXParser.getWaypointsFileForGpx(new File("1234567.GPX")));
        assertEquals(new File("gpx.gpx-wpts.gpx"), GPXParser.getWaypointsFileForGpx(new File("gpx.gpx.gpx")));
        assertNull(GPXParser.getWaypointsFileForGpx(new File("123.gpy")));
        assertNull(GPXParser.getWaypointsFileForGpx(new File("gpx")));
        assertNull(GPXParser.getWaypointsFileForGpx(new File(".gpx")));
        assertNull(GPXParser.getWaypointsFileForGpx(new File("/mnt/sdcard/.gpx")));
    }

    private List<cgCache> readGPX(int... resourceIds) throws IOException {
        final GPX10Parser parser = new GPX10Parser(1);
        for (int resourceId : resourceIds) {
            final Resources res = getInstrumentation().getContext().getResources();
            final InputStream instream = res.openRawResource(resourceId);
            try {
                assertTrue(parser.parse(instream, new Handler()));
            } finally {
                instream.close();
            }
        }
        Collection<cgCache> caches = parser.getParsedCaches();
        assertNotNull(caches);
        List<cgCache> cacheList = new ArrayList<cgCache>(caches);
        // TODO: may need to sort by geocode when a test imports more than one cache
        return cacheList;
    }

    public static void testParseDateWithFractionalSeconds() {
        // was experienced in GSAK file
        String dateString = "2011-08-13T02:52:18.103Z";
        try {
            GPXParser.parseDate(dateString);
        } catch (ParseException e) {
            fail();
            e.printStackTrace();
        }
    }
}
