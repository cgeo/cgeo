package cgeo.geocaching;

import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.RegExPerformanceTest;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.CancellableHandler;

import org.apache.commons.lang3.StringUtils;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.ArrayList;

public class cgBaseTest extends AndroidTestCase {

    public static void testRegEx() {
        String page = MockedCache.readCachePage("GC2CJPF");
        assertEquals("blafoo", BaseUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME, true, "???"));
        assertTrue(page.contains("id=\"ctl00_hlRenew\"") || "Premium Member".equals(BaseUtils.getMatch(page, GCConstants.PATTERN_MEMBER_STATUS, true, "???")));
        int cachesFound = Integer.parseInt(BaseUtils.getMatch(page, GCConstants.PATTERN_CACHES_FOUND, true, "0").replaceAll("[,.]", ""));
        assertTrue(cachesFound >= 491);
    }

    public static void testReplaceWhitespaces() {
        assertEquals("foo bar baz ", BaseUtils.replaceWhitespace(new String("  foo\n\tbar   \r   baz  ")));
    }

    public static void testElevation() {
        assertEquals(125.663703918457, (new Geopoint(48.0, 2.0)).getElevation(), 0.1);
    }

    public static void testCompareCaches(ICache expected, cgCache actual, boolean all) {
        assertEquals(expected.getGeocode(), actual.getGeocode());
        assertTrue(expected.getType() == actual.getType());
        assertEquals(expected.getOwner(), actual.getOwner());
        assertEquals(expected.getDifficulty(), actual.getDifficulty());
        assertEquals(expected.getTerrain(), actual.getTerrain());
        assertEquals(expected.isDisabled(), actual.isDisabled());
        assertEquals(expected.isArchived(), actual.isArchived());
        assertEquals(expected.getSize(), actual.getSize());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getGuid(), actual.getGuid());
        assertTrue(expected.getFavoritePoints() <= actual.getFavoritePoints());
        assertEquals(expected.getHiddenDate().toString(), actual.getHiddenDate().toString());
        assertEquals(expected.isPremiumMembersOnly(), actual.isPremiumMembersOnly());

        if (all) {
            assertEquals(expected.getLatitude(), actual.getLatitude());
            assertEquals(expected.getLongitude(), actual.getLongitude());
            assertTrue(actual.isReliableLatLon());
            assertEquals(expected.isOwn(), actual.isOwn());
            assertEquals(expected.getOwnerReal(), actual.getOwnerReal());
            assertEquals(expected.getHint(), actual.getHint());
            assertTrue(actual.getDescription().startsWith(expected.getDescription()));
            assertEquals(expected.getShortDescription(), actual.getShortDescription());
            assertEquals(expected.getCacheId(), actual.getCacheId());
            assertEquals(expected.getLocation(), actual.getLocation());
            assertEquals(expected.getPersonalNote(), actual.getPersonalNote());
            assertEquals(expected.isFound(), actual.isFound());
            assertEquals(expected.isFavorite(), actual.isFavorite());
            assertEquals(expected.isWatchlist(), actual.isWatchlist());

            for (String attribute : expected.getAttributes()) {
                assertTrue(actual.getAttributes().contains(attribute));
            }
            for (LogType logType : expected.getLogCounts().keySet()) {
                assertTrue(actual.getLogCounts().get(logType) >= expected.getLogCounts().get(logType));
            }

            // the inventory can differ to often, therefore we don't compare them

            int actualSpoilersSize = null != actual.getSpoilers() ? actual.getSpoilers().size() : 0;
            int expectedSpoilersSize = null != expected.getSpoilers() ? expected.getSpoilers().size() : 0;
            assertEquals(expectedSpoilersSize, actualSpoilersSize);
        }
    }

    /**
     * Test {@link cgBase#parseCacheFromText(String, int, CancellableHandler)} with "mocked" data
     *
     */
    @MediumTest
    public static void testParseCacheFromTextWithMockedData() {
        String gcCustomDate = Settings.getGcCustomDate();
        for (MockedCache mockedCache : RegExPerformanceTest.MOCKED_CACHES) {
            // to get the same results we have to use the date format used when the mocked data was created
            Settings.setGcCustomDate(MockedCache.getDateFormat());
            SearchResult searchResult = cgBase.parseCacheFromText(mockedCache.getData(), null);
            cgCache parsedCache = searchResult.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
            assertTrue(StringUtils.isNotBlank(mockedCache.getMockedDataUser()));
            cgBaseTest.testCompareCaches(mockedCache, parsedCache, true);
        }
        Settings.setGcCustomDate(gcCustomDate);
    }

    public static void testWaypointsFromNote() {
        final cgCache cache = cgBaseTest.createCache(0);
        assertNotNull(cache);

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

    private static void assertWaypointsFromNote(final cgCache cache, Geopoint[] expected, String note) {
        cache.setPersonalNote(note);
        cache.setWaypoints(new ArrayList<cgWaypoint>(), false);
        cache.parseWaypointsFromNote();
        assertEquals(expected.length, cache.getWaypoints().size());
        for (int i = 0; i < expected.length; i++) {
            assertTrue(expected[i].isEqualTo(cache.getWaypoint(i).getCoords()));
        }
    }

    public static cgCache createCache(int index) {
        final MockedCache mockedCache = RegExPerformanceTest.MOCKED_CACHES.get(index);
        // to get the same results we have to use the date format used when the mocked data was created
        Settings.setGcCustomDate(MockedCache.getDateFormat());
        final SearchResult searchResult = cgBase.parseCacheFromText(mockedCache.getData(), null);
        return searchResult.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
    }
}