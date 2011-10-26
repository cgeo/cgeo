package cgeo.geocaching;

import cgeo.geocaching.test.RegExPerformanceTest;
import cgeo.geocaching.test.mock.MockedCache;

import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.Date;
import java.util.HashMap;

import junit.framework.Assert;

/**
 * The c:geo application test. It can be used for tests that require an
 * application and/or context.
 */

public class cgeoApplicationTest extends ApplicationTestCase<cgeoapplication> {

    private cgBase base = null;

    public cgeoApplicationTest() {
        super(cgeoapplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // init environment
        createApplication();

        // create required c:geo objects
        base = new cgBase(this.getApplication());
    }

    /**
     * The name 'test preconditions' is a convention to signal that if this test
     * doesn't pass, the test case was not set up properly and it might explain
     * any and all failures in other tests. This is not guaranteed to run before
     * other tests, as junit uses reflection to find the tests.
     */
    @SmallTest
    public void testPreconditions() {
    }

    /**
     * Test {@link cgBase#searchByGeocode(HashMap, int, boolean)}
     *
     * @param base
     */
    @MediumTest
    public void testSearchByGeocode() {
        final cgSearch search = base.searchByGeocode("GC1RMM2", null, 0, true, null);
        Assert.assertNotNull(search);
    }

    /**
     * Test {@link cgBase#parseCacheFromText(String, int, Handler) with "mocked" data
     * @param base
     */
    @MediumTest
    public static void testParseCacheFromText() {
        for (MockedCache cache : RegExPerformanceTest.MOCKED_CACHES) {
            cgCacheWrap caches = cgBase.parseCacheFromText(cache.getData(), 0, null);
            cgCache cacheParsed = caches.cacheList.get(0);
            Assert.assertEquals(cache.getGeocode(), cacheParsed.getGeocode());
            Assert.assertEquals(cache.getCacheType(), cacheParsed.getCacheType());
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

    public void testSearchTrackable() {
        cgTrackable tb = base.searchTrackable("TB2J1VZ", null, null);
        assertEquals("aefffb86-099f-444f-b132-605436163aa8", tb.getGuid());
        assertEquals("TB2J1VZ", tb.getGeocode());
        assertEquals("http://www.geocaching.com/images/wpttypes/21.gif", tb.getIconUrl());
        assertEquals("blafoo's Children Music CD", tb.getName());
        assertEquals("Travel Bug Dog Tag", tb.getType());
        assertEquals(new Date(2009 - 1900, 8 - 1, 24), tb.getReleased());
        assertEquals(10617.8f, tb.getDistance());
        assertEquals("Niedersachsen, Germany", tb.getOrigin());
        assertEquals("blafoo", tb.getOwner());
        assertEquals("0564a940-8311-40ee-8e76-7e91b2cf6284", tb.getOwnerGuid());
        assertEquals("Nice place for a break cache", tb.getSpottedName());
        assertEquals(cgTrackable.SPOTTED_CACHE, tb.getSpottedType());
        assertEquals("faa2d47d-19ea-422f-bec8-318fc82c8063", tb.getSpottedGuid());
        assertEquals("Kinder erfreuen.<br/><br/>Make children happy.", tb.getGoal());
        assertTrue(tb.getDetails().startsWith("Auf der CD sind"));
        assertEquals("http://img.geocaching.com/track/display/38382780-87a7-4393-8393-78841678ee8c.jpg", tb.getImage());
        assertEquals(10, tb.getLogs().size());
    }
}
