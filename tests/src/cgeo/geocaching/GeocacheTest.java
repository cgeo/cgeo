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
import java.util.Collections;
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

            final Geocache cache = new Geocache();
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

    public static void testMergeDownloaded() {
        final Geocache previous = new Geocache();
        previous.setGeocode("GC12345");
        previous.setDetailed(true);
        previous.setDisabled(true);
        previous.setType(CacheType.TRADITIONAL);
        previous.setCoords(new Geopoint(40.0, 8.0));
        previous.setDescription("Test1");
        previous.setAttributes(Collections.singletonList("TestAttribute"));
        previous.setShortDescription("Short");
        previous.setHint("Hint");
        removeCacheCompletely(previous.getGeocode());

        final Geocache download = new Geocache();
        download.setGeocode("GC12345");
        download.setDetailed(true);
        download.setDisabled(false);
        download.setType(CacheType.MULTI);
        download.setCoords(new Geopoint(41.0, 9.0));
        download.setDescription("Test2");

        download.gatherMissingFrom(previous);

        assertThat(download.isDetailed()).as("merged detailed").isTrue();
        assertThat(download.isDisabled()).as("merged disabled").isFalse();
        assertThat(download.getType()).as("merged download").isEqualTo(CacheType.MULTI);
        assertThat(download.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(41.0, 9.0));
        assertThat(download.getDescription()).as("merged description").isEqualTo("Test2");
        assertThat(download.getShortDescription()).as("merged short description").isEmpty();
        assertThat(download.getAttributes()).as("merged attributes").isEmpty();
        assertThat(download.getHint()).as("merged hint").isEmpty();
    }

    public static void testMergeDownloadedStored() {
        final Geocache stored = new Geocache();
        stored.setGeocode("GC12345");
        stored.setDetailed(true);
        stored.setDisabled(true);
        stored.setType(CacheType.TRADITIONAL);
        stored.setCoords(new Geopoint(40.0, 8.0));
        stored.setDescription("Test1");
        stored.setAttributes(Collections.singletonList("TestAttribute"));
        stored.setShortDescription("Short");
        stored.setHint("Hint");
        saveFreshCacheToDB(stored);

        final Geocache download = new Geocache();
        download.setGeocode("GC12345");
        download.setDetailed(true);
        download.setDisabled(false);
        download.setType(CacheType.MULTI);
        download.setCoords(new Geopoint(41.0, 9.0));
        download.setDescription("Test2");
        download.setAttributes(Collections.<String>emptyList());

        download.gatherMissingFrom(stored);

        assertThat(download.isDetailed()).as("merged detailed").isTrue();
        assertThat(download.isDisabled()).as("merged disabled").isFalse();
        assertThat(download.getType()).as("merged download").isEqualTo(CacheType.MULTI);
        assertThat(download.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(41.0, 9.0));
        assertThat(download.getDescription()).as("merged description").isEqualTo("Test2");
        assertThat(download.getShortDescription()).as("merged short description").isEmpty();
        assertThat(download.getAttributes()).as("merged attributes").isEmpty();
        assertThat(download.getHint()).as("merged hint").isEmpty();
    }

    public static void testMergeLivemap() {
        final Geocache previous = new Geocache();
        previous.setGeocode("GC12345");
        previous.setDetailed(true);
        previous.setDisabled(true);
        previous.setType(CacheType.TRADITIONAL);
        previous.setCoords(new Geopoint(40.0, 8.0));
        removeCacheCompletely(previous.getGeocode());

        final Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");
        livemap.setType(CacheType.MULTI, 12);
        livemap.setCoords(new Geopoint(41.0, 9.0), 12);

        livemap.gatherMissingFrom(previous);

        assertThat(livemap.isDetailed()).as("merged detailed").isTrue();
        assertThat(livemap.isDisabled()).as("merged disabled").isTrue();
        assertThat(livemap.getType()).as("merged type").isEqualTo(CacheType.TRADITIONAL);
        assertThat(livemap.getCoords()).as("merged coordinates").isEqualToComparingFieldByField(new Geopoint(40.0, 8.0));
        assertThat(livemap.getCoordZoomLevel()).as("merged zoomlevel").isEqualTo(previous.getCoordZoomLevel());
    }

    public static void testMergeLivemapStored() {
        final Geocache stored = new Geocache();
        stored.setGeocode("GC12345");
        stored.setDetailed(true);
        stored.setDisabled(true);
        stored.setType(CacheType.TRADITIONAL);
        stored.setCoords(new Geopoint(40.0, 8.0));
        saveFreshCacheToDB(stored);

        final Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");
        livemap.setType(CacheType.MULTI, 12);
        livemap.setCoords(new Geopoint(41.0, 9.0), 12);

        livemap.gatherMissingFrom(stored);

        assertThat(livemap.isDetailed()).as("merged detailed").isTrue();
        assertThat(livemap.isDisabled()).as("merged disabled").isTrue();
        assertThat(livemap.getType()).as("merged type").isEqualTo(CacheType.TRADITIONAL);
        assertThat(livemap.getCoords()).as("merged coordinates").isEqualToComparingFieldByField(new Geopoint(40.0, 8.0));
        assertThat(livemap.getCoordZoomLevel()).as("merged zoomlevel").isEqualTo(stored.getCoordZoomLevel());
    }

    public static void testMergeLivemapZoomin() {
        final Geocache livemapFirst = new Geocache();
        livemapFirst.setGeocode("GC12345");
        livemapFirst.setType(CacheType.TRADITIONAL);
        livemapFirst.setCoords(new Geopoint(40.0, 8.0), 11);

        final Geocache livemapSecond = new Geocache();
        livemapSecond.setGeocode("GC12345");
        livemapSecond.setType(CacheType.MULTI);
        livemapSecond.setCoords(new Geopoint(41.0, 9.0), 12);

        livemapSecond.gatherMissingFrom(livemapFirst);

        assertThat(livemapSecond.getType()).as("merged type").isEqualTo(CacheType.MULTI);
        assertThat(livemapSecond.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(41.0, 9.0));
        assertThat(livemapSecond.getCoordZoomLevel()).as("merged zoomlevel").isEqualTo(12);
    }

    public static void testMergeLivemapZoomout() {
        final Geocache livemapFirst = new Geocache();
        livemapFirst.setGeocode("GC12345");
        livemapFirst.setType(CacheType.TRADITIONAL, 12);
        livemapFirst.setCoords(new Geopoint(40.0, 8.0), 12);

        final Geocache livemapSecond = new Geocache();
        livemapSecond.setGeocode("GC12345");
        livemapSecond.setType(CacheType.MULTI, 11);
        livemapSecond.setCoords(new Geopoint(41.0, 9.0), 11);

        livemapSecond.gatherMissingFrom(livemapFirst);

        assertThat(livemapSecond.getType()).as("merged type").isEqualTo(CacheType.TRADITIONAL);
        assertThat(livemapSecond.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(40.0, 8.0));
        assertThat(livemapSecond.getCoordZoomLevel()).as("merged zoomlevel").isEqualTo(12);
    }

    public static void testMergePopupLivemap() {
        final Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");
        livemap.setCoords(new Geopoint(40.0, 8.0), 12);
        livemap.setFound(true);

        final Geocache popup = new Geocache();
        popup.setGeocode("GC12345");
        popup.setType(CacheType.MULTI);

        popup.gatherMissingFrom(livemap);

        assertThat(popup.getType()).as("merged type").isEqualTo(CacheType.MULTI);
        assertThat(popup.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(40.0, 8.0));
        assertThat(popup.isFound()).overridingErrorMessage("merged found").isTrue();
        assertThat(popup.getCoordZoomLevel()).as("merged zoomlevel").isEqualTo(12);
    }

    public static void testMergeLivemapBMSearched() {
        final Geocache bmsearched = new Geocache();
        bmsearched.setGeocode("GC12345");

        final Geocache livemap = new Geocache();
        livemap.setGeocode("GC12345");
        livemap.setCoords(new Geopoint(40.0, 8.0), 12);

        livemap.gatherMissingFrom(bmsearched);

        assertThat(livemap.getCoords()).as("merged coordinates").isEqualTo(new Geopoint(40.0, 8.0));
        assertThat(livemap.getCoordZoomLevel()).as("merged zoomlevel").isEqualTo(12);
    }

    public static void testNameForSorting() {
        final Geocache cache = new Geocache();
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
        assertTime("von 19.15 " + timeHours + " bis ca.20.30 " + timeHours + " statt", 19, 15);
    }

    public static void testGuessEventTimeShortDescription() {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.EVENT);
        cache.setDescription(StringUtils.EMPTY);
        cache.setShortDescription("text 14:20 text");
        assertThat(cache.guessEventTimeMinutes()).isEqualTo(14 * 60 + 20);
    }

    private static void assertTime(final String description, final int hours, final int minutes) {
        final Geocache cache = new Geocache();
        cache.setDescription(description);
        cache.setType(CacheType.EVENT);
        final int minutesAfterMidnight = hours * 60 + minutes;
        assertThat(cache.guessEventTimeMinutes()).isEqualTo(minutesAfterMidnight);
    }

    private static void assertNoTime(final String description) {
        final Geocache cache = new Geocache();
        cache.setDescription(description);
        cache.setType(CacheType.EVENT);
        assertThat(cache.guessEventTimeMinutes()).isEqualTo(-1);
    }

    public static void testGetPossibleLogTypes() throws Exception {
        final Geocache gcCache = new Geocache();
        gcCache.setGeocode("GC123");
        gcCache.setType(CacheType.WEBCAM);
        assertThat(gcCache.getPossibleLogTypes()).as("possible GC cache log types").contains(LogType.WEBCAM_PHOTO_TAKEN);
        assertThat(gcCache.getPossibleLogTypes()).as("possible GC cache log types").contains(LogType.NEEDS_MAINTENANCE);

        final Geocache ocCache = new Geocache();
        ocCache.setGeocode("OC1234");
        ocCache.setType(CacheType.TRADITIONAL);
        assertThat(ocCache.getPossibleLogTypes()).as("traditional cache possible log types").doesNotContain(LogType.WEBCAM_PHOTO_TAKEN);
        assertThat(ocCache.getPossibleLogTypes()).as("OC cache possible log types").doesNotContain(LogType.NEEDS_MAINTENANCE);
    }

    public static void testLogTypeEventPast() throws Exception {
        final Calendar today = Calendar.getInstance();
        today.add(Calendar.DAY_OF_MONTH, -1);
        assertThat(createEventCache(today).getDefaultLogType()).isEqualTo(LogType.ATTENDED);
    }

    public static void testLogTypeEventToday() throws Exception {
        final Calendar today = Calendar.getInstance();
        assertThat(createEventCache(today).getDefaultLogType()).isEqualTo(LogType.ATTENDED);
    }

    public static void testLogTypeEventFuture() throws Exception {
        final Calendar today = Calendar.getInstance();
        today.add(Calendar.DAY_OF_MONTH, 1);
        assertThat(createEventCache(today).getDefaultLogType()).isEqualTo(LogType.WILL_ATTEND);
    }

    private static Geocache createEventCache(final Calendar calendar) {
        final Geocache cache = new Geocache();
        cache.setType(CacheType.EVENT);
        cache.setHidden(calendar.getTime());
        return cache;
    }
}
