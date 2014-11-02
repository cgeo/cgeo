package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class GeocacheTest extends CGeoTestCase {

    private static final class MockedEventCache extends Geocache {
        public MockedEventCache(final Date date) {
            setHidden(date);
            setType(CacheType.EVENT);
        }
    }

    public static void testCanBeAddedToCalendar() {
        final Date today = new Date();
        final Geocache cacheToday = new MockedEventCache(today);
        assertThat(cacheToday.canBeAddedToCalendar()).isTrue();

        final Date yesterday = new Date(today.getTime() - 86400 * 1000);
        final MockedEventCache cacheYesterday = new MockedEventCache(yesterday);
        assertThat(cacheYesterday.canBeAddedToCalendar()).isFalse();
    }

    public static void testEquality() {
        final Geocache one = new Geocache();
        final Geocache two = new Geocache();

        // identity
        assertThat(one.equals(one)).isTrue();

        // different objects without geocode shall not be equal
        assertThat(one.equals(two)).isFalse();

        one.setGeocode("geocode");
        two.setGeocode("geocode");

        // different objects with same geocode shall be equal
        assertThat(one.equals(two)).isTrue();
    }

    public static void testGeocodeUppercase() {
        final Geocache cache = new Geocache();
        cache.setGeocode("gc1234");
        assertThat(cache.getGeocode()).isEqualTo("GC1234");
    }

    public final void testUpdateWaypointFromNote() {
        assertWaypointsParsed("Test N51 13.888 E007 03.444", 1);
    }

    public final void testUpdateWaypointsFromNote() {
        assertWaypointsParsed("Test N51 13.888 E007 03.444 Test N51 13.233 E007 03.444 Test N51 09.123 E007 03.444", 3);
    }

    private void assertWaypointsParsed(final String note, final int expectedWaypoints) {

        recordMapStoreFlags();

        try {
            setMapStoreFlags(false, false);

            Geocache cache = new Geocache();
            final String geocode = "Test" + System.nanoTime();
            cache.setGeocode(geocode);
            cache.setWaypoints(new ArrayList<Waypoint>(), false);
            for (int i = 0; i < 2; i++) {
                cache.setPersonalNote(note);
                cache.parseWaypointsFromNote();
                final List<Waypoint> waypoints = cache.getWaypoints();
                assertThat(waypoints).isNotNull();
                assertThat(waypoints).hasSize(expectedWaypoints);
                final Waypoint waypoint = waypoints.get(0);
                assertThat(waypoint.getCoords()).isEqualTo(new Geopoint("N51 13.888 E007 03.444"));
                //            assertThat(waypoint.getNote()).isEqualTo("Test");
                assertThat(waypoint.getName()).isEqualTo(CgeoApplication.getInstance().getString(R.string.cache_personal_note) + " 1");
                cache.store(StoredList.TEMPORARY_LIST.id, null);
            }
            removeCacheCompletely(geocode);
        } finally {
            restoreMapStoreFlags();
        }
    }

    public static void testMergeDownloadedStored() {

        Geocache stored = new Geocache();
        stored.setGeocode("GC12345");
        stored.setDetailed(true);
        stored.setDisabled(true);
        stored.setType(CacheType.TRADITIONAL);
        stored.setCoords(new Geopoint(40.0, 8.0));
        stored.setDescription("Test1");
        ArrayList<String> attributes = new ArrayList<String>(1);
        attributes.add("TestAttribute");
        stored.setAttributes(attributes);
        stored.setShortDescription("Short");
        stored.setHint("Hint");

        Geocache download = new Geocache();
        download.setGeocode("GC12345");
        download.setDetailed(true);
        download.setDisabled(false);
        download.setType(CacheType.MULTI);
        download.setCoords(new Geopoint(41.0, 9.0));
        download.setDescription("Test2");

        download.gatherMissingFrom(stored);

        assertThat(download.isDetailed()).as("merged detailed").isTrue();
        assertThat(download.isDisabled()).as("merged disabled").isFalse();
        assertEquals("Type not merged correctly", CacheType.MULTI, download.getType());
        assertEquals("Longitude not merged correctly", 9.0, download.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 41.0, download.getCoords().getLatitude(), 0.1);
        assertEquals("Description not merged correctly", "Test2", download.getDescription());
        assertEquals("ShortDescription not merged correctly", "", download.getShortDescription());
        assertEquals("Attributes not merged correctly", new ArrayList<String>(0), download.getAttributes());
        assertEquals("Hint not merged correctly", "", download.getHint());
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
        livemap.setType(CacheType.MULTI, 12);
        livemap.setCoords(new Geopoint(41.0, 9.0), 12);

        livemap.gatherMissingFrom(stored);

        assertThat(livemap.isDetailed()).as("merged detailed").isTrue();
        assertThat(livemap.isDisabled()).as("merged disabled").isTrue();
        assertEquals("Type not merged correctly", CacheType.TRADITIONAL, livemap.getType());
        assertEquals("Longitude not merged correctly", 8.0, livemap.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 40.0, livemap.getCoords().getLatitude(), 0.1);
        assertEquals("Zoomlevel not merged correctly", stored.getCoordZoomLevel(), livemap.getCoordZoomLevel());
    }

    public static void testMergeLivemapZoomin() {

        Geocache livemapFirst = new Geocache();
        livemapFirst.setGeocode("GC12345");
        livemapFirst.setType(CacheType.TRADITIONAL);
        livemapFirst.setCoords(new Geopoint(40.0, 8.0), 11);

        Geocache livemapSecond = new Geocache();
        livemapSecond.setGeocode("GC12345");
        livemapSecond.setType(CacheType.MULTI);
        livemapSecond.setCoords(new Geopoint(41.0, 9.0), 12);

        livemapSecond.gatherMissingFrom(livemapFirst);

        assertEquals("Type not merged correctly", CacheType.MULTI, livemapSecond.getType());
        assertEquals("Longitude not merged correctly", 9.0, livemapSecond.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 41.0, livemapSecond.getCoords().getLatitude(), 0.1);
        assertEquals("Zoomlevel not merged correctly", 12, livemapSecond.getCoordZoomLevel());
    }

    public static void testMergeLivemapZoomout() {

        Geocache livemapFirst = new Geocache();
        livemapFirst.setGeocode("GC12345");
        livemapFirst.setType(CacheType.TRADITIONAL, 12);
        livemapFirst.setCoords(new Geopoint(40.0, 8.0), 12);

        Geocache livemapSecond = new Geocache();
        livemapSecond.setGeocode("GC12345");
        livemapSecond.setType(CacheType.MULTI, 11);
        livemapSecond.setCoords(new Geopoint(41.0, 9.0), 11);

        livemapSecond.gatherMissingFrom(livemapFirst);

        assertEquals("Type not merged correctly", CacheType.TRADITIONAL, livemapSecond.getType());
        assertEquals("Longitude not merged correctly", 8.0, livemapSecond.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 40.0, livemapSecond.getCoords().getLatitude(), 0.1);
        assertEquals("Zoomlevel not merged correctly", 12, livemapSecond.getCoordZoomLevel());
    }

    public static void testMergePopupLivemap() {

        Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");
        livemap.setCoords(new Geopoint(40.0, 8.0), 12);
        livemap.setFound(true);

        Geocache popup = new Geocache();
        popup.setGeocode("GC12345");
        popup.setType(CacheType.MULTI);

        popup.gatherMissingFrom(livemap);

        assertEquals("Type not merged correctly", CacheType.MULTI, popup.getType());
        assertEquals("Longitude not merged correctly", 8.0, popup.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 40.0, popup.getCoords().getLatitude(), 0.1);
        assertThat(popup.isFound()).overridingErrorMessage("Found not merged correctly").isTrue();
        assertEquals("Zoomlevel not merged correctly", 12, popup.getCoordZoomLevel());
    }

    public static void testMergeLivemapBMSearched() {

        Geocache bmsearched = new Geocache();
        bmsearched.setGeocode("GC12345");

        Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");
        livemap.setCoords(new Geopoint(40.0, 8.0), 12);

        livemap.gatherMissingFrom(bmsearched);

        assertEquals("Longitude not merged correctly", 8.0, livemap.getCoords().getLongitude(), 0.1);
        assertEquals("Latitude not merged correctly", 40.0, livemap.getCoords().getLatitude(), 0.1);
        assertEquals("Zoomlevel not merged correctly", 12, livemap.getCoordZoomLevel());
    }

    public static void testNameForSorting() {
        Geocache cache = new Geocache();
        cache.setName("GR8 01-01");
        assertThat(cache.getNameForSorting()).isEqualTo("GR000008 000001-000001");
    }

    public static void testGuessEventTime() {
        assertTime("text 14:20 text", 14, 20);
        assertNoTime("text 30:40 text");
        assertNoTime("text 14:90 text");
        final String timeHours = CgeoApplication.getInstance().getString(R.string.cache_time_full_hours);
        assertTime("text 16 " + timeHours, 16, 0);
        assertTime("text 16 " + StringUtils.lowerCase(timeHours), 16, 0);
        assertTime("text 16:00 " + timeHours, 16, 0);
        assertTime("text 16.00 " + timeHours, 16, 0);
        assertTime("text 14:20.", 14, 20);
        assertTime("<b>14:20</b>", 14, 20);
        assertTime("<u><em>Uhrzeit:</em></u> 17-20 " + timeHours + "</span></strong>", 17, 00);
        assertTime("von 11 bis 13 " + timeHours, 11, 00);
        assertTime("from 11 to 13 " + timeHours, 11, 00);
    }

    public static void testGuessEventTimeShortDescription() {
        Geocache cache = new Geocache();
        cache.setType(CacheType.EVENT);
        cache.setDescription(StringUtils.EMPTY);
        cache.setShortDescription("text 14:20 text");
        assertThat(cache.guessEventTimeMinutes()).isEqualTo(String.valueOf(14 * 60 + 20));
    }

    private static void assertTime(final String description, final int hours, final int minutes) {
        final Geocache cache = new Geocache();
        cache.setDescription(description);
        cache.setType(CacheType.EVENT);
        final int minutesAfterMidnight = hours * 60 + minutes;
        assertThat(cache.guessEventTimeMinutes()).isEqualTo(String.valueOf(minutesAfterMidnight));
    }

    private static void assertNoTime(final String description) {
        final Geocache cache = new Geocache();
        cache.setDescription(description);
        cache.setType(CacheType.EVENT);
        assertThat(cache.guessEventTimeMinutes()).isNull();
    }

    public static void testGetPossibleLogTypes() throws Exception {
        Geocache gcCache = new Geocache();
        gcCache.setGeocode("GC123");
        gcCache.setType(CacheType.WEBCAM);
        assertThat(gcCache.getPossibleLogTypes()).as("possible GC cache log types").contains(LogType.WEBCAM_PHOTO_TAKEN);
        assertThat(gcCache.getPossibleLogTypes()).as("possible GC cache log types").contains(LogType.NEEDS_MAINTENANCE);

        Geocache ocCache = new Geocache();
        ocCache.setGeocode("OC1234");
        ocCache.setType(CacheType.TRADITIONAL);
        assertThat(ocCache.getPossibleLogTypes()).as("traditional cache possible log types").doesNotContain(LogType.WEBCAM_PHOTO_TAKEN);
        assertThat(ocCache.getPossibleLogTypes()).as("OC cache possible log types").doesNotContain(LogType.NEEDS_MAINTENANCE);
    }

    public static void testLogTypeEventPast() throws Exception {
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DAY_OF_MONTH, -1);
        assertThat(createEventCache(today).getDefaultLogType()).isEqualTo(LogType.ATTENDED);
    }

    public static void testLogTypeEventToday() throws Exception {
        Calendar today = Calendar.getInstance();
        assertThat(createEventCache(today).getDefaultLogType()).isEqualTo(LogType.ATTENDED);
    }

    public static void testLogTypeEventFuture() throws Exception {
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DAY_OF_MONTH, 1);
        assertThat(createEventCache(today).getDefaultLogType()).isEqualTo(LogType.WILL_ATTEND);
    }

    private static Geocache createEventCache(Calendar calendar) {
        Geocache cache = new Geocache();
        cache.setType(CacheType.EVENT);
        cache.setHidden(calendar.getTime());
        return cache;
    }
}
