package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.CgeoApplicationTest;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.test.CgeoTestUtils;
import cgeo.geocaching.test.R;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.test.Compare;
import static cgeo.geocaching.connector.gc.GCParser.deleteModifiedCoordinates;
import static cgeo.geocaching.connector.gc.GCParser.editModifiedCoordinates;
import static cgeo.geocaching.connector.gc.GCParser.requestHtmlPage;
import static cgeo.geocaching.enumerations.LoadFlags.LOAD_CACHE_ONLY;

import androidx.annotation.RawRes;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GCParserTest {

    @SmallTest
    @Test
    public void testUnpublishedCacheNotOwner() {
        final int cache = R.raw.cache_unpublished;
        assertUnpublished(cache);
    }

    private void assertUnpublished(final int cache) {
        final String page = CgeoTestUtils.getFileContent(cache);
        final SearchResult result = GCParser.parseAndSaveCacheFromText(GCConnector.getInstance(), page, null);
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getError()).isEqualTo(StatusCode.UNPUBLISHED_CACHE);
    }

    @SmallTest
    @Test
    public void testPublishedCacheWithUnpublishedInDescription1() {
        assertPublishedCache(R.raw.gc430fm_published, "Cache is Unpublished");
    }

    @SmallTest
    @Test
    public void testPublishedCacheWithUnpublishedInDescription2() {
        assertPublishedCache(R.raw.gc431f2_published, "Needle in a Haystack");
    }

    private void assertPublishedCache(final int cachePage, final String cacheName) {
        final String page = CgeoTestUtils.getFileContent(cachePage);
        final SearchResult result = GCParser.parseAndSaveCacheFromText(GCConnector.getInstance(), page, null);
        assertThat(result).isNotNull();
        assertThat(result.getCount()).isEqualTo(1);
        final Geocache cache = result.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
        assertThat(cache).isNotNull();
        assertThat(cache.getName()).isEqualTo(cacheName);
    }

    @MediumTest
    @Test
    public void testOwnCache() {
        final Geocache cache = parseCache(R.raw.own_cache);
        assertThat(cache).isNotNull();
        assertThat(cache.getSpoilers()).as("spoilers").hasSize(2);
        final Image spoiler = cache.getSpoilers().get(1);
        assertThat("http://imgcdn.geocaching.com/cache/large/6ddbbe82-8762-46ad-8f4c-57d03f4b0564.jpeg").as("First spoiler image url wrong").isEqualTo(spoiler.getUrl());
        assertThat("SPOILER").as("First spoiler image text wrong").isEqualTo(spoiler.getTitle());
        assertThat(spoiler.getDescription()).as("First spoiler image description").isNull();
    }

    private static Geocache createCache(final int index) {
        final MockedCache mockedCache = MockedCache.MOCKED_CACHES.get(index);
        // to get the same results we have to use the date format used when the mocked data was created
        final String oldCustomDate = Settings.getGcCustomDate();

        final SearchResult searchResult;
        try {
            Settings.setGcCustomDate(MockedCache.getDateFormat());
            searchResult = GCParser.parseAndSaveCacheFromText(GCConnector.getInstance(), mockedCache.getData(), null);
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
     * Test {@link GCParser#parseAndSaveCacheFromText(IConnector, String, DisposableHandler)} with "mocked" data
     */
    @MediumTest
    @Test
    public void testParseCacheFromTextWithMockedData() {
        final String gcCustomDate = Settings.getGcCustomDate();
        try {
            for (final MockedCache mockedCache : MockedCache.MOCKED_CACHES) {
                // to get the same results we have to use the date format used when the mocked data was created
                Settings.setGcCustomDate(MockedCache.getDateFormat());
                final SearchResult searchResult = GCParser.parseAndSaveCacheFromText(GCConnector.getInstance(), mockedCache.getData(), null);
                final Geocache parsedCache = searchResult.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
                assertThat(parsedCache).isNotNull();
                assertThat(StringUtils.isNotBlank(mockedCache.getMockedDataUser())).isTrue();
                // Workaround for issue #3777
                if (mockedCache.getGeocode().equals("GC3XX5J") && Settings.getUserName().equals("mucek4")) {
                    parsedCache.setFound(true);
                }
                Compare.assertCompareCaches(mockedCache, parsedCache, true);
            }
        } finally {
            Settings.setGcCustomDate(gcCustomDate);
        }
    }

    @MediumTest
    @Test
    public void testWaypointsFromNote() {
        final Geocache cache = createCache(0);

        final Geopoint[] empty = {};
        final Geopoint[] one = {new Geopoint("N51 21.523", "E7 2.680")};
        assertWaypointsFromNote(cache, empty, "  ");
        assertWaypointsFromNote(cache, empty, "some random strings 1 with n 2 numbers");
        assertWaypointsFromNote(cache, empty, "Station3 some coords");
        assertWaypointsFromNote(cache, one, "Station3: N51 21.523 / E07 02.680");
        assertWaypointsFromNote(cache, one, "N51 21.523 / E07 02.680");
        assertWaypointsFromNote(cache, empty, "N51 21.523");
        assertWaypointsFromNote(cache, one, "  n 51° 21.523 - E07 02.680");
        assertWaypointsFromNote(cache, new Geopoint[]{
                        new Geopoint("N51 21.523", "E7 2.680"),
                        new Geopoint("N52 21.523", "E12 2.680")},
                "Station3: N51 21.523 / E07 02.680\r\n Station4: N52 21.523 / E012 02.680");
        assertWaypointsFromNote(cache, empty, "51 21 523 / 07 02 680");
        assertWaypointsFromNote(cache, empty, "N51");
        assertWaypointsFromNote(cache, empty, "N 821 O 321"); // issue 922
        assertWaypointsFromNote(cache, empty, "N 821-211 O 322+11");
        assertWaypointsFromNote(cache, empty, "von 240 meter");
        assertWaypointsFromNote(cache, new Geopoint[]{
                        new Geopoint("N 51 19.844", "E 7 03.625")},
                "A=7 bis B=12 Quellen\r\nC= 66 , Quersumme von 240 m NN\r\nD= 67 , Quersumme von 223 m NN\r\nParken:\r\nN 51 19.844\r\nE 7 03.625");
        assertWaypointsFromNote(cache, new Geopoint[]{
                        new Geopoint("N51 21.444", "E07 02.600"),
                        new Geopoint("N51 21.789", "E07 02.800"),
                        new Geopoint("N51 21.667", "E07 02.800"),
                        new Geopoint("N51 21.444", "E07 02.706"),
                        new Geopoint("N51 21.321", "E07 02.700"),
                        new Geopoint("N51 21.123", "E07 02.477"),
                        new Geopoint("N51 21.734", "E07 02.500"),
                        new Geopoint("N51 21.733", "E07 02.378"),
                        new Geopoint("N51 21.544", "E07 02.566")},
                "Station3: N51 21.444 / E07 02.600\r\nStation4: N51 21.789 / E07 02.800\r\nStation5: N51 21.667 / E07 02.800\r\nStation6: N51 21.444 / E07 02.706\r\nStation7: N51 21.321 / E07 02.700\r\nStation8: N51 21.123 / E07 02.477\r\nStation9: N51 21.734 / E07 02.500\r\nStation10: N51 21.733 / E07 02.378\r\nFinal: N51 21.544 / E07 02.566");
        assertWaypointsFromNote(cache, new Geopoint[]{
                        new Geopoint("S 51° 21.444′ W 007° 02.600′"),
                        new Geopoint("S 51° 21′ W 007° 02′"),
                        new Geopoint("S 51° W 007°"),
                        new Geopoint("S 52° 21′ 43.44″ W 008° 02′ 06.60″"),
                        new Geopoint("S 52° 21′ 43″ W 008° 02′ 06″"),
                        new Geopoint("-53.21544° 8.0206°"),
                        new Geopoint("-53° 8°"),
                        new Geopoint("32U E 458301 N 5434062")},
                "S1 S 51° 21.444′ W 007° 02.600′\r\nS2 S 51° 21′ W 007° 02′\r\nS3 S 51° W 007°\r\nS4 S 52° 21′ 43.44″ W 008° 02′ 06.60″\r\nS5 S 52° 21′ 43″ W 008° 02′ 06″\r\nS6 -53.21544° 8.0206°\r\nS7 -53° 8°\r\nS8 32U E 458301 N 5434062");
    }

    @MediumTest
    @Test
    public void testEditModifiedCoordinates() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC2ZN4G");
        // upload coordinates
        editModifiedCoordinates(cache, new Geopoint("N51 21.544", "E07 02.566"));
        cache.dropSynchronous();
        final String page = requestHtmlPage(cache.getGeocode(), null, "n");
        final Geocache cache2 = GCParser.parseAndSaveCacheFromText(GCConnector.getInstance(), page, null).getFirstCacheFromResult(LOAD_CACHE_ONLY);
        assertThat(cache2).isNotNull();
        assertThat(cache2.hasUserModifiedCoords()).isTrue();
        assertThat(cache2.getCoords()).isEqualTo(new Geopoint("N51 21.544", "E07 02.566"));
        // delete coordinates
        deleteModifiedCoordinates(cache2);
        cache2.dropSynchronous();
        final String page2 = requestHtmlPage(cache.getGeocode(), null, "n");
        final Geocache cache3 = GCParser.parseAndSaveCacheFromText(GCConnector.getInstance(), page2, null).getFirstCacheFromResult(LOAD_CACHE_ONLY);
        assertThat(cache3).isNotNull();
        assertThat(cache3.hasUserModifiedCoords()).isFalse();
    }

    private void assertWaypointsFromNote(final Geocache cache, final Geopoint[] expected, final String note) {
        cache.setPersonalNote(note);
        cache.setWaypoints(new ArrayList<>(), false);
        cache.addWaypointsFromNote();
        final List<Waypoint> waypoints = cache.getWaypoints();
        assertThat(waypoints).hasSize(expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertThat(waypoints.get(i).getCoords()).isEqualTo(expected[i]);
        }
    }

    @MediumTest
    @Test
    public void testWaypointParsing() {
        Geocache cache = parseCache(R.raw.gc366bq);
        assertThat(cache).isNotNull();
        assertThat(cache.getWaypoints()).hasSize(14);
        //make sure that waypoints are not duplicated
        cache = parseCache(R.raw.gc366bq);
        assertThat(cache).isNotNull();
        assertThat(cache.getWaypoints()).hasSize(14);
    }

    @MediumTest
    @Test
    public void testWaypointParsingEmptyCoords() {
        final Geocache cache = parseCache(R.raw.gc366bq);
        assertThat(cache).isNotNull();
        int countEmptyCoords = 0;
        for (final Waypoint wp : cache.getWaypoints()
        ) {
            if (wp.isOriginalCoordsEmpty()) {
                countEmptyCoords++;
            }
        }
        assertThat(countEmptyCoords).isEqualTo(1);
    }

    @MediumTest
    @Test
    public void testNoteParsingWaypointTypes() {
        final Geocache cache = new Geocache();
        cache.setWaypoints(new ArrayList<>(), false);
        cache.setPersonalNote("\"Parking area at PARKING=N 50° 40.666E 006° 58.222\n" + "My calculated final coordinates: FINAL=N 50° 40.777E 006° 58.111\n" + "Get some ice cream at N 50° 40.555E 006° 58.000\"");

        cache.addWaypointsFromNote();
        final List<Waypoint> waypoints = cache.getWaypoints();

        assertThat(waypoints).hasSize(3);
        assertThat(waypoints.get(0).getWaypointType()).isEqualTo(WaypointType.PARKING);
        assertThat(waypoints.get(1).getWaypointType()).isEqualTo(WaypointType.FINAL);
        assertThat(waypoints.get(2).getWaypointType()).isEqualTo(WaypointType.WAYPOINT);
    }

    @Nullable
    private Geocache parseCache(@RawRes final int resourceId) {
        final String page = CgeoTestUtils.getFileContent(resourceId);
        final SearchResult result = GCParser.parseAndSaveCacheFromText(GCConnector.getInstance(), page, null);
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isFalse();
        return result.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
    }

    @MediumTest
    @Test
    public void testTrackableNotActivated() {
        final String page = CgeoTestUtils.getFileContent(R.raw.tb123e_html);
        final Trackable trackable = GCParser.parseTrackable(page, "TB123E");
        assertThat(trackable).isNotNull();
        assertThat(trackable.getGeocode()).isEqualTo("TB123E");
        final String expectedDetails = CgeoApplication.getInstance().getString(cgeo.geocaching.R.string.trackable_not_activated);
        assertThat(trackable.getDetails()).isEqualTo(expectedDetails);
    }

    @MediumTest
    @Test
    public void testOnlineCacheUrl() {
        assertThat(StringUtils.right(Objects.requireNonNull(new CgeoApplicationTest().searchByGeocode("GC5EF16")).getDescription(), 150)).as("related web page appended to description").contains("http://eventimgruenen.de/");
    }

    @MediumTest
    @Test
    public void testOnlineEventDate() {
        assertThat(Objects.requireNonNull(new CgeoApplicationTest().searchByGeocode("GC68TJE")).getHiddenDate()).isEqualTo("2016-01-23");
    }

    @MediumTest
    @Test
    public void testOnlineWatchCount() {
        assertThat(Objects.requireNonNull(new CgeoApplicationTest().searchByGeocode("GCK25B")).getWatchlistCount()).as("Geocaching HQ watch count").isGreaterThan(50);
    }

    @MediumTest
    @Test
    public void testSpoilerDescriptionForOwner() {
        final Geocache cache = parseCache(R.raw.gc352y3_owner_view);
        assertThat(cache).isNotNull();
        final List<Image> spoilers = cache.getSpoilers();

        assertThat(spoilers).hasSize(2);

        Image spoiler = spoilers.get(0);
        assertThat(spoiler.getTitle()).isEqualTo("Grundschule Moltkestra&#223;e");

        spoiler = spoilers.get(1);
        assertThat(spoiler.getTitle()).isEqualTo("SPOILER");
        assertThat(spoiler.getDescription()).isEqualTo("Suche diese Schraube");
    }

    @MediumTest
    @Test
    public void testSpoilerWithoutTitleAndLinkToLargerImage() {
        final Geocache cache = parseCache(R.raw.gc6xyb6);
        assertThat(cache).isNotNull();
        final List<Image> spoilers = cache.getSpoilers();

        assertThat(spoilers).hasSize(1);

        final Image spoiler = spoilers.get(0);
        assertThat(spoiler.getTitle()).isEqualTo("");
        assertThat(spoiler.getDescription()).isEqualTo("FOTO SPOILER");
        assertThat(spoiler.getUrl()).isEqualTo("https://img.geocaching.com/cache/large/124a14b5-87dd-42c6-8c83-52c184e07389.jpg");
    }

    @MediumTest
    @Test
    public void testSpoilerBackgroundImage() {
        final Geocache cache = parseCache(R.raw.gc45w92);
        assertThat(cache).isNotNull();
        final List<Image> spoilers = cache.getSpoilers();
        // We know that the background image has been parsed last
        final Image spoiler = spoilers.get(spoilers.size() - 1);
        assertThat(spoiler.getUrl()).isEqualTo("https://www.dropbox.com/s/1kakwnpny8698hm/QR_Hintergrund.jpg?dl=1");
    }

    @MediumTest
    @Test
    public void testFullScaleImageUrl() {
        assertThat(GCParser.fullScaleImageUrl("https://www.dropbox.com/s/1kakwnpny8698hm/QR_Hintergrund.jpg?dl=1"))
                .isEqualTo("https://www.dropbox.com/s/1kakwnpny8698hm/QR_Hintergrund.jpg?dl=1");
        assertThat(GCParser.fullScaleImageUrl("http://imgcdn.geocaching.com/track/display/33cee358-f692-4f90-ace0-80c5a2c60a5c.jpg"))
                .isEqualTo("http://imgcdn.geocaching.com/track/33cee358-f692-4f90-ace0-80c5a2c60a5c.jpg");
    }

    @MediumTest
    @Test
    public void testGetUsername() {
        assertThat(GCParser.getUsername(MockedCache.readCachePage("GC2CJPF"))).isEqualTo("abft");
    }
}
