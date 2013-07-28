package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;

import android.os.Handler;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GeocacheTest extends AndroidTestCase {

    final static private class MockedEventCache extends Geocache {
        public MockedEventCache(final Date date) {
            setHidden(date);
            setType(CacheType.EVENT);
        }
    }

    public static void testCanBeAddedToCalendar() {
        final Date today = new Date();
        final Geocache cacheToday = new MockedEventCache(today);
        assertTrue(cacheToday.canBeAddedToCalendar());

        final Date yesterday = new Date(today.getTime() - 86400 * 1000);
        final MockedEventCache cacheYesterday = new MockedEventCache(yesterday);
        assertFalse(cacheYesterday.canBeAddedToCalendar());
    }

    public static void testEquality() {
        final Geocache one = new Geocache();
        final Geocache two = new Geocache();

        // identity
        assertTrue(one.equals(one));

        // different objects without geocode shall not be equal
        assertFalse(one.equals(two));

        one.setGeocode("geocode");
        two.setGeocode("geocode");

        // different objects with same geocode shall be equal
        assertTrue(one.equals(two));
    }

    public static void testGeocodeUppercase() {
        final Geocache cache = new Geocache();
        cache.setGeocode("gc1234");
        assertEquals("GC1234", cache.getGeocode());
    }

    public static void testUpdateWaypointFromNote() {
        assertWaypointsParsed("Test N51 13.888 E007 03.444", 1);
    }

    public static void testUpdateWaypointsFromNote() {
        assertWaypointsParsed("Test N51 13.888 E007 03.444 Test N51 13.233 E007 03.444 Test N51 09.123 E007 03.444", 3);
    }

    private static void assertWaypointsParsed(String note, int expectedWaypoints) {
        Geocache cache = new Geocache();
        cache.setGeocode("Test" + System.nanoTime());
        cache.setWaypoints(new ArrayList<Waypoint>(), false);
        for (int i = 0; i < 2; i++) {
            cache.setPersonalNote(note);
            cache.parseWaypointsFromNote();
            final List<Waypoint> waypoints = cache.getWaypoints();
            assertNotNull(waypoints);
            assertEquals(expectedWaypoints, waypoints.size());
            final Waypoint waypoint = waypoints.get(0);
            assertEquals(new Geopoint("N51 13.888 E007 03.444"), waypoint.getCoords());
            //            assertEquals("Test", waypoint.getNote());
            assertEquals(cgeoapplication.getInstance().getString(R.string.cache_personal_note) + " 1", waypoint.getName());
            cache.store(StoredList.TEMPORARY_LIST_ID, null);
        }
        cache.drop(new Handler());
    }

    public static void testMergeDownloadedStored() {

        Geocache stored = new Geocache();
        stored.setGeocode("GC12345");
        stored.setDetailed(true);
        stored.setDisabled(true);
        stored.setType(CacheType.TRADITIONAL);
        stored.setCoords(new Geopoint(40.0, 8.0));
        stored.setDescription("Test1");

        Geocache download = new Geocache();
        download.setGeocode("GC12345");
        download.setDetailed(true);
        download.setDisabled(false);
        download.setType(CacheType.MULTI);
        download.setCoords(new Geopoint(41.0, 9.0));
        download.setDescription("Test2");

        download.gatherMissingFrom(stored);

        assertTrue("Detailed not merged correctly", download.isDetailed());
        assertFalse("Disabled not merged correctly", download.isDisabled());
        assertEquals("Type not merged correctly", CacheType.MULTI, download.getType());
        assertEquals("Longitude not merged correctly", 9.0, download.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 41.0, download.getCoords().getLatitude(), 0.1);
        assertEquals("Description not merged correctly", "Test2", download.getDescription());
    }

    public static void testMergeLivemapStored() {

        Geocache stored = new Geocache();
        stored.setGeocode("GC12345");
        stored.setDetailed(true);
        stored.setDisabled(true);
        stored.setType(CacheType.TRADITIONAL);
        stored.setCoords(new Geopoint(40.0, 8.0));

        Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");
        livemap.setType(CacheType.MULTI);
        livemap.setCoords(new Geopoint(41.0, 9.0));
        livemap.setZoomlevel(12);

        livemap.gatherMissingFrom(stored);

        assertTrue("Detailed not merged correctly", livemap.isDetailed());
        assertTrue("Disabled not merged correctly", livemap.isDisabled());
        assertEquals("Type not merged correctly", CacheType.TRADITIONAL, livemap.getType());
        assertEquals("Longitude not merged correctly", 8.0, livemap.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 40.0, livemap.getCoords().getLatitude(), 0.1);
        assertEquals("Zoomlevel not merged correctly", stored.getZoomLevel(), livemap.getZoomLevel());
    }

    public static void testMergeLivemapZoomin() {

        Geocache livemapFirst = new Geocache();
        livemapFirst.setGeocode("GC12345");
        livemapFirst.setType(CacheType.TRADITIONAL);
        livemapFirst.setCoords(new Geopoint(40.0, 8.0));
        livemapFirst.setZoomlevel(11);

        Geocache livemapSecond = new Geocache();
        livemapSecond.setGeocode("GC12345");
        livemapSecond.setType(CacheType.MULTI);
        livemapSecond.setCoords(new Geopoint(41.0, 9.0));
        livemapSecond.setZoomlevel(12);

        livemapSecond.gatherMissingFrom(livemapFirst);

        assertEquals("Type not merged correctly", CacheType.MULTI, livemapSecond.getType());
        assertEquals("Longitude not merged correctly", 9.0, livemapSecond.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 41.0, livemapSecond.getCoords().getLatitude(), 0.1);
        assertEquals("Zoomlevel not merged correctly", 12, livemapSecond.getZoomLevel());
    }

    public static void testMergeLivemapZoomout() {

        Geocache livemapFirst = new Geocache();
        livemapFirst.setGeocode("GC12345");
        livemapFirst.setType(CacheType.TRADITIONAL);
        livemapFirst.setCoords(new Geopoint(40.0, 8.0));
        livemapFirst.setZoomlevel(12);

        Geocache livemapSecond = new Geocache();
        livemapSecond.setGeocode("GC12345");
        livemapSecond.setType(CacheType.MULTI);
        livemapSecond.setCoords(new Geopoint(41.0, 9.0));
        livemapSecond.setZoomlevel(11);

        livemapSecond.gatherMissingFrom(livemapFirst);

        assertEquals("Type not merged correctly", CacheType.TRADITIONAL, livemapSecond.getType());
        assertEquals("Longitude not merged correctly", 8.0, livemapSecond.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 40.0, livemapSecond.getCoords().getLatitude(), 0.1);
        assertEquals("Zoomlevel not merged correctly", 12, livemapSecond.getZoomLevel());
    }

    public static void testMergePopupLivemap() {

        Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");
        livemap.setCoords(new Geopoint(40.0, 8.0));
        livemap.setFound(true);
        livemap.setZoomlevel(12);

        Geocache popup = new Geocache();
        popup.setGeocode("GC12345");
        popup.setType(CacheType.MULTI);

        popup.gatherMissingFrom(livemap);

        assertEquals("Type not merged correctly", CacheType.MULTI, popup.getType());
        assertEquals("Longitude not merged correctly", 8.0, popup.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 40.0, popup.getCoords().getLatitude(), 0.1);
        assertTrue("Found not merged correctly", popup.isFound());
        assertEquals("Zoomlevel not merged correctly", 12, popup.getZoomLevel());
    }

    public static void testMergeLivemapBMSearched() {

        Geocache bmsearched = new Geocache();
        bmsearched.setGeocode("GC12345");

        Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");
        livemap.setCoords(new Geopoint(40.0, 8.0));
        livemap.setZoomlevel(12);

        livemap.gatherMissingFrom(bmsearched);

        assertEquals("Longitude not merged correctly", 8.0, livemap.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 40.0, livemap.getCoords().getLatitude(), 0.1);
        assertEquals("Zoomlevel not merged correctly", 12, livemap.getZoomLevel());
    }
}
