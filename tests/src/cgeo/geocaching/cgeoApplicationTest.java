package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.test.RegExPerformanceTest;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.CancellableHandler;

import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.Date;

import junit.framework.Assert;

/**
 * The c:geo application test. It can be used for tests that require an
 * application and/or context.
 */

public class cgeoApplicationTest extends ApplicationTestCase<cgeoapplication> {

    public cgeoApplicationTest() {
        super(cgeoapplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // init environment
        createApplication();
    }

    /**
     * The name 'test preconditions' is a convention to signal that if this test
     * doesn't pass, the test case was not set up properly and it might explain
     * any and all failures in other tests. This is not guaranteed to run before
     * other tests, as junit uses reflection to find the tests.
     */
    @SmallTest
    public void testPreconditions() {
        // intentionally left blank
    }

    /**
     * Test {@link cgBase#searchTrackable(String, String, String)}
     */
    @MediumTest
    public static void testSearchTrackableNotExisting() {
        cgTrackable tb = cgBase.searchTrackable("123456", null, null);
        assertNotNull(tb);
    }

    /**
     * Test {@link cgBase#searchTrackable(String, String, String)}
     */
    @MediumTest
    public static void testSearchTrackable() {
        cgTrackable tb = cgBase.searchTrackable("TB2J1VZ", null, null);
        // fix data
        assertEquals("aefffb86-099f-444f-b132-605436163aa8", tb.getGuid());
        assertEquals("TB2J1VZ", tb.getGeocode());
        assertEquals("http://www.geocaching.com/images/wpttypes/21.gif", tb.getIconUrl());
        assertEquals("blafoo's Children Music CD", tb.getName());
        assertEquals("Travel Bug Dog Tag", tb.getType());
        assertEquals(new Date(2009 - 1900, 8 - 1, 24), tb.getReleased());
        assertEquals("Niedersachsen, Germany", tb.getOrigin());
        assertEquals("blafoo", tb.getOwner());
        assertEquals("0564a940-8311-40ee-8e76-7e91b2cf6284", tb.getOwnerGuid());
        assertEquals("Kinder erfreuen.<br/><br/>Make children happy.", tb.getGoal());
        assertTrue(tb.getDetails().startsWith("Auf der CD sind"));
        assertEquals("http://img.geocaching.com/track/display/38382780-87a7-4393-8393-78841678ee8c.jpg", tb.getImage());
        // Following data can change over time
        assertTrue(tb.getDistance() >= 10617.8f);
        assertTrue(tb.getLogs().size() >= 10);
        assertTrue(cgTrackable.SPOTTED_CACHE == tb.getSpottedType() || cgTrackable.SPOTTED_USER == tb.getSpottedType());
        // no assumption possible: assertEquals("faa2d47d-19ea-422f-bec8-318fc82c8063", tb.getSpottedGuid());
        // no assumption possible: assertEquals("Nice place for a break cache", tb.getSpottedName());
    }

    /**
     * Test {@link cgBase#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static cgCache testSearchByGeocode(final String geocode) {
        final ParseResult search = cgBase.searchByGeocode(geocode, null, 0, true, null);
        assertNotNull(search);
        if (Settings.isPremiumMember() || search.error == null) {
            assertEquals(1, search.getGeocodes().size());
            assertTrue(search.getGeocodes().contains(geocode));
            return cgeoapplication.getInstance().getCacheByGeocode(geocode);
        }
        assertEquals(0, search.getGeocodes().size());
        return null;
    }

    /**
     * Test {@link cgBase#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByGeocodeNotExisting() {
        final ParseResult search = cgBase.searchByGeocode("GC123456", null, 0, true, null);
        assertNotNull(search);
        assertEquals(search.error, StatusCode.UNPUBLISHED_CACHE);
    }

    /**
     * Test {@link cgBase#searchByCoords(cgSearchThread, Geopoint, String, int, boolean)}
     */
    @MediumTest
    public static void testSearchByCoords() {
        final ParseResult search = cgBase.searchByCoords(null, new Geopoint("N 52° 24.972 E 009° 35.647"), CacheType.MYSTERY, 0, false);
        assertNotNull(search);
        assertEquals(20, search.getGeocodes().size());
        assertTrue(search.getGeocodes().contains("GC1RMM2"));
    }

    /**
     * Test {@link cgBase#searchByOwner(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByOwner() {
        final ParseResult search = cgBase.searchByOwner(null, "blafoo", CacheType.MYSTERY, 0, false);
        assertNotNull(search);
        assertEquals(3, search.getGeocodes().size());
        assertTrue(search.getGeocodes().contains("GC36RT6"));
    }

    /**
     * Test {@link cgBase#searchByUsername(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByUsername() {
        final ParseResult search = cgBase.searchByUsername(null, "blafoo", CacheType.WEBCAM, 0, false);
        assertNotNull(search);
        assertEquals(3, search.totalCnt);
        assertTrue(search.getGeocodes().contains("GCP0A9"));
    }

    /**
     * Test cache parsing. Esp. useful after a GC.com update
     */
    public static void testCacheBasics() {
        for (MockedCache mockedCache : RegExPerformanceTest.MOCKED_CACHES) {
            mockedCache.setMockedDataUser(Settings.getUsername());
            cgCache parsedCache = cgeoApplicationTest.testSearchByGeocode(mockedCache.getGeocode());
            if (null != parsedCache) {
                cgBaseTest.testCompareCaches(mockedCache, parsedCache);
            }
        }
    }

    /**
     * Caches that are good test cases
     */
    public static void testCacheSpecialties() {
        cgCache GCV2R9 = cgeoApplicationTest.testSearchByGeocode("GCV2R9");
        Assert.assertEquals("California, United States", GCV2R9.getLocation());

        cgCache GC1ZXEZ = cgeoApplicationTest.testSearchByGeocode("GC1ZXEZ");
        Assert.assertEquals("Ms.Marple/Mr.Stringer", GC1ZXEZ.getOwnerReal());
    }

}

