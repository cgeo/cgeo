package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.CancellableHandler;

import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.Date;

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
    public static void testSearchByGeocode() {
        final cgSearch search = cgBase.searchByGeocode("GC1RMM2", null, 0, true, null);
        assertNotNull(search);
        assertEquals(1, search.getGeocodes().size());
        assertTrue(search.getGeocodes().contains("GC1RMM2"));
    }

    /**
     * Test {@link cgBase#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByGeocodeNotExisting() {
        final cgSearch search = cgBase.searchByGeocode("GC123456", null, 0, true, null);
        assertNull(search);
    }

    /**
     * Test {@link cgBase#searchByCoords(cgSearchThread, Geopoint, String, int, boolean)}
     */
    @MediumTest
    public static void testSearchByCoords() {
        final cgSearch search = cgBase.searchByCoords(null, new Geopoint("N 52° 24.972 E 009° 35.647"), CacheType.MYSTERY, 0, false);
        assertNotNull(search);
        assertEquals(20, search.getGeocodes().size());
        assertTrue(search.getGeocodes().contains("GC1RMM2"));
    }

    /**
     * Test {@link cgBase#searchByOwner(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByOwner() {
        final cgSearch search = cgBase.searchByOwner(null, "blafoo", CacheType.MYSTERY, 0, false);
        assertNotNull(search);
        assertEquals(3, search.getGeocodes().size());
        assertTrue(search.getGeocodes().contains("GC36RT6"));
    }

    /**
     * Test {@link cgBase#searchByUsername(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByUsername() {
        final cgSearch search = cgBase.searchByUsername(null, "blafoo", CacheType.WEBCAM, 0, false);
        assertNotNull(search);
        assertEquals(3, search.totalCnt);
        assertTrue(search.getGeocodes().contains("GCP0A9"));
    }

}

