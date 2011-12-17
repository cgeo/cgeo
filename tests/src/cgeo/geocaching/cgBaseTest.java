package cgeo.geocaching;

import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.RegExPerformanceTest;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.CancellableHandler;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import junit.framework.Assert;

public class cgBaseTest extends AndroidTestCase {

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
        Assert.assertEquals(expected.isDisabled(), actual.isDisabled());
        Assert.assertEquals(expected.isOwn(), actual.isOwn());
        Assert.assertEquals(expected.isArchived(), actual.isArchived());
        Assert.assertEquals(expected.isMembersOnly(), actual.isMembersOnly());
        Assert.assertEquals(expected.getOwnerReal(), actual.getOwnerReal());
        Assert.assertEquals(expected.getSize(), actual.getSize());
        Assert.assertEquals(expected.getHint(), actual.getHint());
        Assert.assertTrue(actual.getDescription().startsWith(expected.getDescription()));
        Assert.assertEquals(expected.getShortDescription(), actual.getShortDescription());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getCacheId(), actual.getCacheId());
        Assert.assertEquals(expected.getGuid(), actual.getGuid());
        // Assert.assertEquals(expected.getLocation(), actual.getLocation());
        Assert.assertEquals(expected.getPersonalNote(), actual.getPersonalNote());
        Assert.assertEquals(expected.isFound(), actual.isFound());
        Assert.assertEquals(expected.isFavorite(), actual.isFavorite());
        Assert.assertEquals(expected.getFavoritePoints(), actual.getFavoritePoints());
        Assert.assertEquals(expected.isWatchlist(), actual.isWatchlist());
        Assert.assertEquals(expected.getHiddenDate().toString(), actual.getHiddenDate().toString());
        for (String attribute : expected.getAttributes()) {
            Assert.assertTrue(actual.getAttributes().contains(attribute));
        }
        for (LogType logType : expected.getLogCounts().keySet()) {
            Assert.assertEquals(expected.getLogCounts().get(logType), actual.getLogCounts().get(logType));
        }

        int actualInventorySize = null != actual.getInventory() ? actual.getInventory().size() : 0;
        int expectInventorysize = null != expected.getInventory() ? expected.getInventory().size() : 0;
        Assert.assertEquals(expectInventorysize, actualInventorySize);

        int actualSpoilersSize = null != actual.getSpoilers() ? actual.getSpoilers().size() : 0;
        int expectSpoilerssize = null != expected.getSpoilers() ? expected.getSpoilers().size() : 0;
        Assert.assertEquals(expectSpoilerssize, actualSpoilersSize);
    }

    /**
     * Test {@link cgBase#parseCacheFromText(String, int, CancellableHandler)} with "mocked" data
     *
     * @param base
     */
    @MediumTest
    public static void testParseCacheFromTextWithMockedData() {
        for (MockedCache mockedCache : RegExPerformanceTest.MOCKED_CACHES) {
            cgCacheWrap caches = cgBase.parseCacheFromText(mockedCache.getData(), 0, null);
            cgCache parsedCache = caches.cacheList.get(0);
            cgBaseTest.testCompareCaches(mockedCache, parsedCache);
        }
    }

}