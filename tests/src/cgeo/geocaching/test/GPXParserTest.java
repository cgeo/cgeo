package cgeo.geocaching.test;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgLog;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.GPX10Parser;
import cgeo.geocaching.files.GPXParser;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.res.Resources;
import android.os.Handler;
import android.test.InstrumentationTestCase;

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

    private cgCache testGPXVersion(final int resourceId) throws Exception {
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

    public void testGPXVersion101() throws Exception {
        final cgCache cache = testGPXVersion(R.raw.gc1bkp3_gpx101);
        assertNotNull(cache.attributes);
        assertEquals(10, cache.attributes.size());
    }

    public void testOC() throws Exception {
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

    public void testGc31j2h() throws Exception {
        final List<cgCache> caches = readGPX(R.raw.gc31j2h);
        assertEquals(1, caches.size());
        final cgCache cache = caches.get(0);

        assertGc31j2h(cache);
        assertSame(cache, caches.get(0));

        // no waypoints without importing waypoint file
        assertNull(cache.waypoints);
    }

    public void testGc31j2hWpts() throws Exception {
        List<cgCache> caches = readGPX(R.raw.gc31j2h);
        assertEquals(1, caches.size());
        cgCache cache = caches.get(0);
        // add to stored caches
        cgeoapplication.getInstance().addCacheToSearch(new cgSearch(), cache);

        caches = readGPX(R.raw.gc31j2h_wpts);
        assertEquals(1, caches.size()); // one cache was updated with 2 waypoints
        cache = caches.get(0);
        assertGc31j2h(cache);
        assertGc31j2hWaypoints(cache);

        // re-importing waypoints should not lead to double waypoints
        caches = readGPX(R.raw.gc31j2h_wpts);
        assertEquals(1, caches.size());
        cache = caches.get(0);
        assertGc31j2h(cache);
        assertGc31j2hWaypoints(cache);
    }

    public void testGc31j2hWptsWithoutCache() throws Exception {
        // remove cache from DB and cache
        cgeoapplication.getInstance().dropStored(1);
        cgeoapplication.getInstance().removeCacheFromCache("GC31J2H");

        final List<cgCache> caches = readGPX(R.raw.gc31j2h_wpts);
        assertEquals(0, caches.size());
    }

    public void testConvertWaypointSym2Type() {
        assertEquals(WaypointType.WAYPOINT, GPXParser.convertWaypointSym2Type("unknown sym"));

        assertEquals(WaypointType.PKG, GPXParser.convertWaypointSym2Type("parking area"));
        assertEquals(WaypointType.STAGE, GPXParser.convertWaypointSym2Type("stages of a multicache"));
        assertEquals(WaypointType.PUZZLE, GPXParser.convertWaypointSym2Type("question to answer"));
        assertEquals(WaypointType.TRAILHEAD, GPXParser.convertWaypointSym2Type("trailhead"));
        assertEquals(WaypointType.FLAG, GPXParser.convertWaypointSym2Type("final location"));
        assertEquals(WaypointType.WAYPOINT, GPXParser.convertWaypointSym2Type("reference point"));
    }

    private void assertGc31j2h(final cgCache cache) throws ParseException {
        assertEquals("GC31J2H", cache.getGeocode());
        assertEquals("Hockenheimer City-Brunnen", cache.getName());
        assertTrue("Hockenheimer City-Brunnen by vptsz, Multi-cache (2/1)", cache.getShortDescription().startsWith("Kurzer informativer Multi entlang der Brunnen"));
        assertTrue(cache.getDescription().startsWith("Cachemobile können kostenfrei am Messplatz geparkt werden."));
        assertTrue(cache.hasTrackables());
        assertEquals(2.0f, cache.getDifficulty().floatValue(), 0.01f);
        assertEquals(1.0f, cache.getTerrain().floatValue(), 0.01f);
        assertEquals("N 49° 19.122", cache.getLatitude());
        assertEquals("E 008° 32.739", cache.getLongitude());
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
        assertEquals(LOG_DATE_FORMAT.parse("2011-09-11T07:00:00Z").getTime(), log.date);
        assertEquals(-1, log.found);
        assertEquals("Sehr schöne Runde und wir haben wieder etwas Neues über Hockenheim gelernt. Super Tarnung.\nTFTC, Geoteufel", log.log);

        // following info is not contained in pocket query gpx file
        assertNull(cache.attributes);
    }

    private void assertGc31j2hWaypoints(final cgCache cache) {
        assertNotNull(cache.waypoints);
        assertEquals(2, cache.waypoints.size());
        cgWaypoint wp = cache.waypoints.get(0);
        assertEquals("GC31J2H", wp.geocode);
        assertEquals("00", wp.getPrefix());
        assertEquals("---", wp.lookup);
        assertEquals("Parkplatz", wp.name);
        assertEquals("Kostenfreies Parken (je nach Parkreihe Parkscheibe erforderlich)", wp.note);
        assertEquals(WaypointType.PKG.id, wp.type);
        assertEquals(49.317517, wp.coords.getLatitude(), 0.000001);
        assertEquals(8.545083, wp.coords.getLongitude(), 0.000001);
        assertEquals("N 49° 19.051", wp.latitudeString);
        assertEquals("E 008° 32.705", wp.longitudeString);

        wp = cache.waypoints.get(1);
        assertEquals("GC31J2H", wp.geocode);
        assertEquals("S1", wp.getPrefix());
        assertEquals("---", wp.lookup);
        assertEquals("Station 1", wp.name);
        assertEquals("Ein zweiter Wegpunkt, der nicht wirklich existiert sondern nur zum Testen gedacht ist.", wp.note);
        assertEquals(WaypointType.STAGE.id, wp.type);
        assertEquals(49.317500, wp.coords.getLatitude(), 0.000001);
        assertEquals(8.545100, wp.coords.getLongitude(), 0.000001);
    }

    private List<cgCache> readGPX(final int resourceId) throws IOException {
        Collection<cgCache> caches = null;
        final Resources res = getInstrumentation().getContext().getResources();
        final InputStream instream = res.openRawResource(resourceId);
        try {
            final GPX10Parser parser = new GPX10Parser(1);
            caches = parser.parse(instream, new Handler());
        } finally {
            instream.close();
        }
        assertNotNull(caches);
        List<cgCache> cacheList = new ArrayList<cgCache>(caches);
        // TODO: may need to sort by geocode when a test imports more than one cache
        return cacheList;
    }

}
