package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;

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
        cache.setGeocode("Test");
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
    }
}
