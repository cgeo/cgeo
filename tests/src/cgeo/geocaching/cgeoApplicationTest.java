package cgeo.geocaching;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCacheWrap;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.test.mock.GC1ZXX2;
import cgeo.geocaching.test.mock.GC2CJPF;
import cgeo.geocaching.test.mock.MockedCache;

import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;
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
        final UUID id = base.searchByGeocode("GC1RMM2", null, 0, true);
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
            cgCacheWrap caches = base.parseCache(cache.getData(), 0);
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
        cgCacheWrap caches = base.parseCache(MockedCache.readCachePage("GCV2R9"), 0);
        cgCache cache = caches.cacheList.get(0);
        Assert.assertEquals("California, United States", cache.getLocation());
    }
}
