package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.RegExPerformanceTest;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.CancellableHandler;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import java.util.ArrayList;
import java.util.Date;

import junit.framework.Assert;

public class cgBaseTest extends AndroidTestCase {

    public static void testRegEx() {
        String page = MockedCache.readCachePage("GC2CJPF");
        Assert.assertEquals("blafoo", BaseUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME, true, "???"));
        Assert.assertEquals("Premium Member", BaseUtils.getMatch(page, GCConstants.PATTERN_MEMBER_STATUS, true, "???"));
        int cachesFound = Integer.parseInt(BaseUtils.getMatch(page, GCConstants.PATTERN_CACHES_FOUND, true, "0"));
        Assert.assertTrue(cachesFound >= 491);
    }

    public static void testReplaceWhitespaces() {
        Assert.assertEquals("foo bar baz ", BaseUtils.replaceWhitespace(new String("  foo\n\tbar   \r   baz  ")));
    }

    public static void testElevation() {
        Assert.assertEquals(125.663703918457, cgBase.getElevation(new Geopoint(48.0, 2.0)), 0.1);
    }

    public static void testCompareCaches(ICache expected, cgCache actual) {
        Assert.assertEquals(expected.getGeocode(), actual.getGeocode());
        Assert.assertTrue(expected.getType() == actual.getType());
        Assert.assertEquals(expected.getOwner(), actual.getOwner());
        Assert.assertEquals(expected.getDifficulty(), actual.getDifficulty());
        Assert.assertEquals(expected.getTerrain(), actual.getTerrain());
        Assert.assertEquals(expected.getLatitude(), actual.getLatitude());
        Assert.assertEquals(expected.getLongitude(), actual.getLongitude());
        assertTrue(actual.isReliableLatLon());
        Assert.assertEquals(expected.isDisabled(), actual.isDisabled());
        Assert.assertEquals(expected.isOwn(), actual.isOwn());
        Assert.assertEquals(expected.isArchived(), actual.isArchived());
        Assert.assertEquals(expected.isPremiumMembersOnly(), actual.isPremiumMembersOnly());
        Assert.assertEquals(expected.getOwnerReal(), actual.getOwnerReal());
        Assert.assertEquals(expected.getSize(), actual.getSize());
        Assert.assertEquals(expected.getHint(), actual.getHint());
        Assert.assertTrue(actual.getDescription().startsWith(expected.getDescription()));
        Assert.assertEquals(expected.getShortDescription(), actual.getShortDescription());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getCacheId(), actual.getCacheId());
        Assert.assertEquals(expected.getGuid(), actual.getGuid());
        Assert.assertEquals(expected.getLocation(), actual.getLocation());
        Assert.assertEquals(expected.getPersonalNote(), actual.getPersonalNote());
        Assert.assertEquals(expected.isFound(), actual.isFound());
        Assert.assertEquals(expected.isFavorite(), actual.isFavorite());
        Assert.assertTrue(expected.getFavoritePoints() <= actual.getFavoritePoints());
        Assert.assertEquals(expected.isWatchlist(), actual.isWatchlist());
        Date date1 = expected.getHiddenDate();
        Date date2 = actual.getHiddenDate();
        Assert.assertEquals(date1.toString(), date2.toString());
        for (String attribute : expected.getAttributes()) {
            Assert.assertTrue(actual.getAttributes().contains(attribute));
        }
        for (LogType logType : expected.getLogCounts().keySet()) {
            Assert.assertTrue(actual.getLogCounts().get(logType) >= expected.getLogCounts().get(logType));
        }

        int actualInventorySize = null != actual.getInventory() ? actual.getInventory().size() : 0;
        int expectedInventorySize = null != expected.getInventory() ? expected.getInventory().size() : 0;
        // don't check inventory, it's to volatile
        // Assert.assertEquals(expectedInventorySize, actualInventorySize);

        int actualSpoilersSize = null != actual.getSpoilers() ? actual.getSpoilers().size() : 0;
        int expectedSpoilersSize = null != expected.getSpoilers() ? expected.getSpoilers().size() : 0;
        Assert.assertEquals(expectedSpoilersSize, actualSpoilersSize);
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
            Settings.setGcCustomDate(mockedCache.getDateFormat());
            ParseResult parseResult = cgBase.parseCacheFromText(mockedCache.getData(), 0, null);
            cgCache parsedCache = cgBase.getFirstElementFromSet(parseResult.cacheList);
            cgBaseTest.testCompareCaches(mockedCache, parsedCache);
        }
        Settings.setGcCustomDate(gcCustomDate);
    }

    public static void testHumanDistance() {
        assertEquals("?", cgBase.getHumanDistance(null));
        if (Settings.isUseMetricUnits()) {
            assertEquals("123 km", cgBase.getHumanDistance(123.456f));
        }
        else {
            assertEquals("77 mi", cgBase.getHumanDistance(123.456f));
        }
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
        cache.setWaypoints(new ArrayList<cgWaypoint>());
        cache.parseWaypointsFromNote();
        assertEquals(expected.length, cache.getWaypoints().size());
        for (int i = 0; i < expected.length; i++) {
            assertTrue(expected[i].isEqualTo(cache.getWaypoint(i).getCoords()));
        }
    }

    public static cgCache createCache(int index) {
        final MockedCache mockedCache = RegExPerformanceTest.MOCKED_CACHES.get(index);
        // to get the same results we have to use the date format used when the mocked data was created
        Settings.setGcCustomDate(mockedCache.getDateFormat());
        final ParseResult parseResult = cgBase.parseCacheFromText(mockedCache.getData(), 0, null);
        return cgBase.getFirstElementFromSet(parseResult.cacheList);
    }
}