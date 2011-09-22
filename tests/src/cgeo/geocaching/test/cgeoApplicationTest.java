package cgeo.geocaching.test;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgCacheWrap;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.mock.GC1ZXX2;
import cgeo.geocaching.test.mock.GC2CJPF;
import cgeo.geocaching.test.mock.MockedCache;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import junit.framework.Assert;

/**
 * The c:geo application test. It can be used for tests that require an
 * application and/or context.
 */

public class cgeoApplicationTest extends ApplicationTestCase<cgeoapplication> {

    private cgSettings settings = null;
    private cgBase base = null;

    public cgeoApplicationTest() {
        super(cgeoapplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // init environment
        createApplication();
        final Context context = this.getContext();
        final SharedPreferences prefs = context.getSharedPreferences(
                cgSettings.preferences, Context.MODE_PRIVATE);

        // create required c:geo objects
        settings = new cgSettings(context, prefs);
        base = new cgBase(this.getApplication(), settings, prefs);
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
        final Map<String, String> params = new HashMap<String, String>();
        params.put("geocode", "GC1RMM2");

        final UUID id = base.searchByGeocode(params, 0, true);
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
        }
    }

    /**
     * Test whether the parsing of the testcaches still give the same results for bearing and distance
     */
    @MediumTest
    public void testParsedCachesDistanceAndBearing() {
        Geopoint coordsGC2CJPF = getCoords(new GC2CJPF());
        Geopoint coordsGC1ZXX2 = getCoords(new GC1ZXX2());

        Float expectedDistance = Float.valueOf("6.5839443");
        Float distanceGC2CJPFtoGC1ZXX2 = Float.valueOf(coordsGC2CJPF.distanceTo(coordsGC1ZXX2));
        Float distanceGC1ZXX2toGC2CJPF = Float.valueOf(coordsGC1ZXX2.distanceTo(coordsGC2CJPF));

        Float expectedBearingGC2CJPFtoGC1ZXX2 = Float.valueOf("151.18116760253906");
        Float bearingGC2CJPFtoGC1ZXX2 = Float.valueOf(coordsGC2CJPF.bearingTo(coordsGC1ZXX2));

        Float expectedBearingGC1ZXX2toGC2CJPF = Float.valueOf("331.21807861328125");
        Float bearingGC1ZXX2toGC2CJPF = Float.valueOf(coordsGC1ZXX2.bearingTo(coordsGC2CJPF));

        Assert.assertEquals(expectedDistance, distanceGC2CJPFtoGC1ZXX2);
        Assert.assertEquals(expectedDistance, distanceGC1ZXX2toGC2CJPF);
        Assert.assertEquals(expectedBearingGC2CJPFtoGC1ZXX2, bearingGC2CJPFtoGC1ZXX2);
        Assert.assertEquals(expectedBearingGC1ZXX2toGC2CJPF, bearingGC1ZXX2toGC2CJPF);

        // Distance both ways should give the same result (distance from a to b should be equal to the distance from b to a)
        Assert.assertEquals(distanceGC2CJPFtoGC1ZXX2, distanceGC1ZXX2toGC2CJPF);

        // bearing from a to b should be opposite of b to a (or: difference should be 180 degrees)
        float expectedDifference = 180f;
        float difference = bearingGC1ZXX2toGC2CJPF.floatValue() - bearingGC2CJPFtoGC1ZXX2.floatValue();

        // floats are not exact, so round the results
        Assert.assertEquals(Math.round(expectedDifference), Math.round(difference));
    }

    private Geopoint getCoords(MockedCache cache) {
        cgCacheWrap caches = base.parseCache(cache.getData(), 0);
        cgCache parsedCache = caches.cacheList.get(0);
        return parsedCache.coords;
    }
}
