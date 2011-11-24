package cgeo.geocaching;

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

    /**
     * Test {@link cgBase#parseCacheFromText(String, int, CancellableHandler)} with "mocked" data
     * 
     * @param base
     */
    @MediumTest
    public static void testParseCacheFromText() {
        for (MockedCache cache : RegExPerformanceTest.MOCKED_CACHES) {
            cgCacheWrap caches = cgBase.parseCacheFromText(cache.getData(), 0, null);
            cgCache cacheParsed = caches.cacheList.get(0);
            Assert.assertEquals(cache.getGeocode(), cacheParsed.getGeocode());
            Assert.assertTrue(cache.getType() == cacheParsed.getType());
            Assert.assertEquals(cache.getOwner(), cacheParsed.getOwner());
            Assert.assertEquals(cache.getDifficulty(), cacheParsed.getDifficulty());
            Assert.assertEquals(cache.getTerrain(), cacheParsed.getTerrain());
            Assert.assertEquals(cache.getLatitude(), cacheParsed.getLatitude());
            Assert.assertEquals(cache.getLongitude(), cacheParsed.getLongitude());
            Assert.assertEquals(cache.isDisabled(), cacheParsed.isDisabled());
            Assert.assertEquals(cache.isOwn(), cacheParsed.isOwn());
            Assert.assertEquals(cache.isArchived(), cacheParsed.isArchived());
            Assert.assertEquals(cache.isMembersOnly(), cacheParsed.isMembersOnly());
            Assert.assertEquals(cache.getOwnerReal(), cacheParsed.getOwnerReal());
            Assert.assertEquals(cache.getSize(), cacheParsed.getSize());
            Assert.assertEquals(cache.getHint(), cacheParsed.getHint());
            Assert.assertTrue(cacheParsed.getDescription().startsWith(cache.getDescription()));
            Assert.assertEquals(cache.getShortDescription(), cacheParsed.getShortDescription());
            Assert.assertEquals(cache.getName(), cacheParsed.getName());
            Assert.assertEquals(cache.getCacheId(), cacheParsed.getCacheId());
            Assert.assertEquals(cache.getGuid(), cacheParsed.getGuid());
            Assert.assertEquals(cache.getLocation(), cacheParsed.getLocation());
            Assert.assertEquals(cache.getPersonalNote(), cacheParsed.getPersonalNote());
            Assert.assertEquals(cache.isFound(), cacheParsed.isFound());
            Assert.assertEquals(cache.isFavorite(), cacheParsed.isFavorite());
            Assert.assertEquals(cache.getFavoritePoints(), cacheParsed.getFavoritePoints());
            Assert.assertEquals(cache.isWatchlist(), cacheParsed.isWatchlist());
            Assert.assertEquals(cache.getHiddenDate().toString(), cacheParsed.getHiddenDate().toString());
            for (String attribute : cache.getAttributes()) {
                Assert.assertTrue(cacheParsed.getAttributes().contains(attribute));
            }
            for (Integer key : cache.getLogCounts().keySet()) {
                Assert.assertEquals(cache.getLogCounts().get(key), cacheParsed.getLogCounts().get(key));
            }
            if (null != cache.getInventory() || null != cacheParsed.getInventory()) {
                Assert.assertEquals(cache.getInventory().size(), cacheParsed.getInventory().size());
            }
            if (null != cache.getSpoilers() || null != cacheParsed.getSpoilers()) {
                Assert.assertEquals(cache.getSpoilers().size(), cacheParsed.getSpoilers().size());
            }

        }
    }

    public static void testParseLocationWithLink() {
        cgCacheWrap caches = cgBase.parseCacheFromText(MockedCache.readCachePage("GCV2R9"), 0, null);
        assertEquals(1, caches.cacheList.size());
        cgCache cache = caches.cacheList.get(0);
        Assert.assertEquals("California, United States", cache.getLocation());
    }

}