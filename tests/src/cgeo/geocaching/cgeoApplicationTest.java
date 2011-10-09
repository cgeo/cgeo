package cgeo.geocaching;

import cgeo.geocaching.test.mock.GC1ZXX2;
import cgeo.geocaching.test.mock.GC2CJPF;
import cgeo.geocaching.test.mock.MockedCache;

import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
        final UUID id = base.searchByGeocode("GC1RMM2", null, 0, true, null);
        Assert.assertNotNull(id);
    }

/**
* Test {@link cgBase#parseCache(String, int) with "mocked" data
* @param base
*/
    @MediumTest
    public void testParseCache() {
        final List<MockedCache> cachesToTest = new ArrayList<MockedCache>();
        cachesToTest.add(new GC2CJPF());
        cachesToTest.add(new GC1ZXX2());

        for (MockedCache cache : cachesToTest) {
            cgCacheWrap caches = base.parseCache(cache.getData(), 0, null);
            cgCache cacheParsed = caches.cacheList.get(0);
            Assert.assertEquals(cache.getGeocode(), cacheParsed.getGeocode());
            Assert.assertEquals(cache.getType(), cacheParsed.getType());
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
        }
    }

    public void testParseLocationWithLink() {
        cgCacheWrap caches = base.parseCache(MockedCache.readCachePage("GCV2R9"), 0, null);
        cgCache cache = caches.cacheList.get(0);
        Assert.assertEquals("California, United States", cache.getLocation());
    }

    public void testSearchTrackable() {
        cgTrackable tb = base.searchTrackable("TB2J1VZ", null, null);
        Assert.assertEquals("aefffb86-099f-444f-b132-605436163aa8", tb.getGuid());
        Assert.assertEquals("TB2J1VZ", tb.getGeocode());
        Assert.assertEquals("http://www.geocaching.com/images/wpttypes/21.gif", tb.getIconUrl());
        Assert.assertEquals("blafoo's Children Music CD", tb.getName());
        Assert.assertEquals("Travel Bug Dog Tag", tb.getType());
        Assert.assertEquals(new Date(2009 - 1900, 8 - 1, 24), tb.getReleased());
        Assert.assertEquals(10617.8f, tb.getDistance());
        Assert.assertEquals("Niedersachsen, Germany", tb.getOrigin());
        Assert.assertEquals("blafoo", tb.getOwner());
        Assert.assertEquals("0564a940-8311-40ee-8e76-7e91b2cf6284", tb.getOwnerGuid());
        Assert.assertEquals("Nice place for a break cache", tb.getSpottedName());
        Assert.assertEquals(cgTrackable.SPOTTED_CACHE, tb.getSpottedType());
        Assert.assertEquals("faa2d47d-19ea-422f-bec8-318fc82c8063", tb.getSpottedGuid());
        Assert.assertEquals("Kinder erfreuen.<br/><br/>Make children happy.", tb.getGoal());
        Assert.assertTrue(tb.getDetails().startsWith("Auf der CD sind"));
        Assert.assertEquals("http://img.geocaching.com/track/display/38382780-87a7-4393-8393-78841678ee8c.jpg", tb.getImage());
        Assert.assertEquals(10, tb.getLogs().size());
    }

}
