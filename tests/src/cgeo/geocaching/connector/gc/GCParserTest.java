package cgeo.geocaching.connector.gc;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.Image;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.test.RegExPerformanceTest;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.test.Compare;

import org.apache.commons.lang3.StringUtils;

import android.test.suitebuilder.annotation.MediumTest;

import java.util.ArrayList;
import java.util.List;

public class GCParserTest extends AbstractResourceInstrumentationTestCase {

    public void testUnpublishedCacheNotOwner() {
        final int cache = R.raw.cache_unpublished;
        assertUnpublished(cache);
    }

    private void assertUnpublished(final int cache) {
        final String page = getFileContent(cache);
        final SearchResult result = GCParser.parseAndSaveCacheFromText(page, null);
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getError()).isEqualTo(StatusCode.UNPUBLISHED_CACHE);
    }

    public void testPublishedCacheWithUnpublishedInDescription1() {
        assertPublishedCache(R.raw.gc430fm_published, "Cache is Unpublished");
    }

    public void testPublishedCacheWithUnpublishedInDescription2() {
        assertPublishedCache(R.raw.gc431f2_published, "Needle in a Haystack");
    }

    private void assertPublishedCache(final int cachePage, final String cacheName) {
        final String page = getFileContent(cachePage);
        final SearchResult result = GCParser.parseAndSaveCacheFromText(page, null);
        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(1);
        final Geocache cache = result.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
        assertThat(cache).isNotNull();
        assert (cache != null); // eclipse bug
        assertThat(cache.getName()).isEqualTo(cacheName);
    }

    public void testOwnCache() {
        final Geocache cache = parseCache(R.raw.own_cache);
        assertThat(cache).isNotNull();
        assertThat(cache.getSpoilers()).as("spoilers").hasSize(2);
        final Image spoiler = cache.getSpoilers().get(1);
        assertEquals("First spoiler image url wrong", "http://imgcdn.geocaching.com/cache/large/6ddbbe82-8762-46ad-8f4c-57d03f4b0564.jpeg", spoiler.getUrl());
        assertEquals("First spoiler image text wrong", "SPOILER", spoiler.getTitle());
        assertThat(spoiler.getDescription()).as("First spoiler image description").isNull();
    }

    private static Geocache createCache(int index) {
        final MockedCache mockedCache = RegExPerformanceTest.MOCKED_CACHES.get(index);
        // to get the same results we have to use the date format used when the mocked data was created
        final String oldCustomDate = Settings.getGcCustomDate();

        final SearchResult searchResult;
        try {
            Settings.setGcCustomDate(MockedCache.getDateFormat());
            searchResult = GCParser.parseAndSaveCacheFromText(mockedCache.getData(), null);
        } finally {
            Settings.setGcCustomDate(oldCustomDate);
        }

        assertThat(searchResult).isNotNull();
        assertThat(searchResult.getCount()).isEqualTo(1);

        final Geocache cache = searchResult.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
        assertThat(cache).isNotNull();
        return cache;
    }

    /**
     * Test {@link GCParser#parseCacheFromText(String, CancellableHandler)} with "mocked" data
     *
     */
    @MediumTest
    public static void testParseCacheFromTextWithMockedData() {
        final String gcCustomDate = Settings.getGcCustomDate();
        try {
            for (MockedCache mockedCache : RegExPerformanceTest.MOCKED_CACHES) {
                // to get the same results we have to use the date format used when the mocked data was created
                Settings.setGcCustomDate(MockedCache.getDateFormat());
                SearchResult searchResult = GCParser.parseAndSaveCacheFromText(mockedCache.getData(), null);
                Geocache parsedCache = searchResult.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
                assertThat(StringUtils.isNotBlank(mockedCache.getMockedDataUser())).isTrue();
                Compare.assertCompareCaches(mockedCache, parsedCache, true);
            }
        } finally {
            Settings.setGcCustomDate(gcCustomDate);
        }
    }

    public static void testWaypointsFromNote() {
        final Geocache cache = createCache(0);

        final Geopoint[] empty = new Geopoint[] {};
        final Geopoint[] one = new Geopoint[] { new Geopoint("N51 21.523", "E7 2.680") };
        assertWaypointsFromNote(cache, empty, "  ");
        assertWaypointsFromNote(cache, empty, "some random strings 1 with n 2 numbers");
        assertWaypointsFromNote(cache, empty, "Station3 some coords");
        assertWaypointsFromNote(cache, one, "Station3: N51 21.523 / E07 02.680");
        assertWaypointsFromNote(cache, one, "N51 21.523 / E07 02.680");
        assertWaypointsFromNote(cache, empty, "N51 21.523");
        assertWaypointsFromNote(cache, one, "  n 51° 21.523 - E07 02.680");
        assertWaypointsFromNote(cache, new Geopoint[] {
                new Geopoint("N51 21.523", "E7 2.680"),
                new Geopoint("N52 21.523", "E12 2.680") },
                "Station3: N51 21.523 / E07 02.680\r\n Station4: N52 21.523 / E012 02.680");
        assertWaypointsFromNote(cache, empty, "51 21 523 / 07 02 680");
        assertWaypointsFromNote(cache, empty, "N51");
        assertWaypointsFromNote(cache, empty, "N 821 O 321"); // issue 922
        assertWaypointsFromNote(cache, empty, "N 821-211 O 322+11");
        assertWaypointsFromNote(cache, empty, "von 240 meter");
        assertWaypointsFromNote(cache, new Geopoint[] {
                new Geopoint("N 51 19.844", "E 7 03.625") },
                "A=7 bis B=12 Quellen\r\nC= 66 , Quersumme von 240 m NN\r\nD= 67 , Quersumme von 223 m NN\r\nParken:\r\nN 51 19.844\r\nE 7 03.625");
        assertWaypointsFromNote(cache, new Geopoint[] {
                new Geopoint("N51 21.444", "E07 02.600"),
                new Geopoint("N51 21.789", "E07 02.800"),
                new Geopoint("N51 21.667", "E07 02.800"),
                new Geopoint("N51 21.444", "E07 02.706"),
                new Geopoint("N51 21.321", "E07 02.700"),
                new Geopoint("N51 21.123", "E07 02.477"),
                new Geopoint("N51 21.734", "E07 02.500"),
                new Geopoint("N51 21.733", "E07 02.378"),
                new Geopoint("N51 21.544", "E07 02.566") },
                "Station3: N51 21.444 / E07 02.600\r\nStation4: N51 21.789 / E07 02.800\r\nStation5: N51 21.667 / E07 02.800\r\nStation6: N51 21.444 / E07 02.706\r\nStation7: N51 21.321 / E07 02.700\r\nStation8: N51 21.123 / E07 02.477\r\nStation9: N51 21.734 / E07 02.500\r\nStation10: N51 21.733 / E07 02.378\r\nFinal: N51 21.544 / E07 02.566");
    }

    @MediumTest
    public static void testEditModifiedCoordinates() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC2ZN4G");
        // upload coordinates
        GCParser.editModifiedCoordinates(cache, new Geopoint("N51 21.544", "E07 02.566"));
        cache.dropSynchronous();
        final String page = GCParser.requestHtmlPage(cache.getGeocode(), null, "n");
        final Geocache cache2 = GCParser.parseAndSaveCacheFromText(page, null).getFirstCacheFromResult(LoadFlags.LOAD_CACHE_ONLY);
        assertThat(cache2).isNotNull();
        assert (cache2 != null); // eclipse bug
        assertThat(cache2.hasUserModifiedCoords()).isTrue();
        assertEquals(new Geopoint("N51 21.544", "E07 02.566"), cache2.getCoords());
        // delete coordinates
        GCParser.deleteModifiedCoordinates(cache2);
        cache2.dropSynchronous();
        final String page2 = GCParser.requestHtmlPage(cache.getGeocode(), null, "n");
        final Geocache cache3 = GCParser.parseAndSaveCacheFromText(page2, null).getFirstCacheFromResult(LoadFlags.LOAD_CACHE_ONLY);
        assertThat(cache3).isNotNull();
        assert (cache3 != null); // eclipse bug
        assertThat(cache3.hasUserModifiedCoords()).isFalse();
    }

    private static void assertWaypointsFromNote(final Geocache cache, Geopoint[] expected, String note) {
        cache.setPersonalNote(note);
        cache.setWaypoints(new ArrayList<Waypoint>(), false);
        cache.parseWaypointsFromNote();
        final List<Waypoint> waypoints = cache.getWaypoints();
        assertThat(waypoints).hasSize(expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertThat(expected[i].equals(waypoints.get(i).getCoords())).isTrue();
        }
    }

    public void testWaypointParsing() {
        Geocache cache = parseCache(R.raw.gc366bq);
        assertThat(cache.getWaypoints()).hasSize(13);
        //make sure that waypoints are not duplicated
        cache = parseCache(R.raw.gc366bq);
        assertThat(cache.getWaypoints()).hasSize(13);
    }

    public static void testNoteParsingWaypointTypes() {
        final Geocache cache = new Geocache();
        cache.setWaypoints(new ArrayList<Waypoint>(), false);
        cache.setPersonalNote("\"Parking area at PARKING=N 50° 40.666E 006° 58.222\n" +
                "My calculated final coordinates: FINAL=N 50° 40.777E 006° 58.111\n" +
                "Get some ice cream at N 50° 40.555E 006° 58.000\"");

        cache.parseWaypointsFromNote();
        final List<Waypoint> waypoints = cache.getWaypoints();

        assertThat(waypoints).hasSize(3);
        assertThat(waypoints.get(0).getWaypointType()).isEqualTo(WaypointType.PARKING);
        assertThat(waypoints.get(1).getWaypointType()).isEqualTo(WaypointType.FINAL);
        assertThat(waypoints.get(2).getWaypointType()).isEqualTo(WaypointType.WAYPOINT);
    }

    private Geocache parseCache(int resourceId) {
        final String page = getFileContent(resourceId);
        final SearchResult result = GCParser.parseAndSaveCacheFromText(page, null);
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();
        return result.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
    }

    public void testTrackableNotActivated() {
        final String page = getFileContent(R.raw.tb123e_html);
        final Trackable trackable = GCParser.parseTrackable(page, "TB123E");
        assertThat(trackable).isNotNull();
        assertThat(trackable.getGeocode()).isEqualTo("TB123E");
        final String expectedDetails = CgeoApplication.getInstance().getString(cgeo.geocaching.R.string.trackable_not_activated);
        assertThat(trackable.getDetails()).isEqualTo(expectedDetails);
    }
}
