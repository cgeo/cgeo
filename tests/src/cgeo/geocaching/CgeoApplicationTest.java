package cgeo.geocaching;

import cgeo.CGeoTestCase;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.connector.gc.MapTokens;
import cgeo.geocaching.connector.gc.Tile;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LiveMapStrategy.Strategy;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.loaders.RecaptchaReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;
import cgeo.geocaching.test.RegExPerformanceTest;
import cgeo.geocaching.test.mock.GC1ZXX2;
import cgeo.geocaching.test.mock.GC2CJPF;
import cgeo.geocaching.test.mock.GC2JVEH;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;
import cgeo.test.Compare;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.GregorianCalendar;

import junit.framework.Assert;

/**
 * The c:geo application test. It can be used for tests that require an
 * application and/or context.
 */

public class CgeoApplicationTest extends CGeoTestCase {

    private static final MapTokens INVALID_TOKEN = null;

    /**
     * The name 'test preconditions' is a convention to signal that if this test
     * doesn't pass, the test case was not set up properly and it might explain
     * any and all failures in other tests. This is not guaranteed to run before
     * other tests, as junit uses reflection to find the tests.
     */
    @SuppressWarnings("static-method")
    @SmallTest
    public void testPreconditions() {
        assertEquals(StatusCode.NO_ERROR, GCLogin.getInstance().login());
    }

    /**
     * Test {@link GCParser#searchTrackable(String, String, String)}
     */
    @MediumTest
    public static void testSearchTrackableNotExisting() {
        final Trackable tb = GCParser.searchTrackable("123456", null, null);
        assertNull(tb);
    }

    /**
     * Test {@link GCParser#searchTrackable(String, String, String)}
     */
    @MediumTest
    public static void testSearchTrackable() {
        final Trackable tb = GCParser.searchTrackable("TB2J1VZ", null, null);
        assertNotNull(tb);
        assert (tb != null); // eclipse bug
        // fix data
        assertEquals("aefffb86-099f-444f-b132-605436163aa8", tb.getGuid());
        assertEquals("TB2J1VZ", tb.getGeocode());
        assertEquals("http://www.geocaching.com/images/wpttypes/21.gif", tb.getIconUrl());
        assertEquals("blafoo's Children Music CD", tb.getName());
        assertEquals("Travel Bug Dog Tag", tb.getType());
        assertEquals(new GregorianCalendar(2009, 8 - 1, 24).getTime(), tb.getReleased());
        assertEquals("Niedersachsen, Germany", tb.getOrigin());
        assertEquals("blafoo", tb.getOwner());
        assertEquals("0564a940-8311-40ee-8e76-7e91b2cf6284", tb.getOwnerGuid());
        assertEquals("Kinder erfreuen.<br /><br />Make children happy.", tb.getGoal());
        assertTrue(tb.getDetails().startsWith("Auf der CD sind"));
        assertEquals("http://imgcdn.geocaching.com/track/display/38382780-87a7-4393-8393-78841678ee8c.jpg", tb.getImage());
        // Following data can change over time
        assertTrue(tb.getDistance() >= 10617.8f);
        assertTrue(tb.getLogs().size() >= 10);
        assertTrue(Trackable.SPOTTED_CACHE == tb.getSpottedType() || Trackable.SPOTTED_USER == tb.getSpottedType());
        // no assumption possible: assertEquals("faa2d47d-19ea-422f-bec8-318fc82c8063", tb.getSpottedGuid());
        // no assumption possible: assertEquals("Nice place for a break cache", tb.getSpottedName());

        // we can't check specifics in the log entries since they change, but we can verify data was parsed
        for (LogEntry log : tb.getLogs()) {
            assertTrue(log.date > 0);
            assertTrue(StringUtils.isNotEmpty(log.author));
            if (log.type == LogType.PLACED_IT || log.type == LogType.RETRIEVED_IT) {
                assertTrue(StringUtils.isNotEmpty(log.cacheName));
                assertTrue(StringUtils.isNotEmpty(log.cacheGuid));
            } else {
                assertTrue(log.type != LogType.UNKNOWN);
            }
        }
    }

    /**
     * Test {@link GCParser#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static Geocache testSearchByGeocode(final String geocode) {
        final SearchResult search = Geocache.searchByGeocode(geocode, null, 0, true, null);
        assertNotNull(search);
        if (Settings.isPremiumMember() || search.getError() == null) {
            assertEquals(1, search.getGeocodes().size());
            assertTrue(search.getGeocodes().contains(geocode));
            return DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        }
        assertEquals(0, search.getGeocodes().size());
        return null;
    }

    /**
     * Test {@link Geocache#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByGeocodeNotExisting() {
        final SearchResult search = Geocache.searchByGeocode("GC123456", null, 0, true, null);
        assertNotNull(search);
        assertEquals(StatusCode.COMMUNICATION_ERROR, search.getError());
    }

    /**
     * Set the login data to the cgeo login, run the given Runnable, and restore the login.
     *
     * @param runnable
     */
    private static void withMockedLoginDo(final Runnable runnable) {
        final ImmutablePair<String, String> login = Settings.getGcCredentials();
        final String memberStatus = Settings.getMemberStatus();

        try {
            runnable.run();
        } finally {
            // restore user and password
            TestSettings.setLogin(login.left, login.right);
            Settings.setMemberStatus(memberStatus);
            GCLogin.getInstance().login();
        }
    }

    /**
     * Test {@link Geocache#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchByGeocodeNotLoggedIn() {
        withMockedLoginDo(new Runnable() {

            public void run() {
                // non premium cache
                MockedCache cache = new GC2CJPF();

                deleteCacheFromDBAndLogout(cache.getGeocode());

                SearchResult search = Geocache.searchByGeocode(cache.getGeocode(), null, StoredList.TEMPORARY_LIST_ID, true, null);
                assertNotNull(search);
                assertEquals(1, search.getGeocodes().size());
                assertTrue(search.getGeocodes().contains(cache.getGeocode()));
                final Geocache searchedCache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
                // coords must be null if the user is not logged in
                assertNull(searchedCache.getCoords());

                // premium cache. Not visible to guests
                cache = new GC2JVEH();

                deleteCacheFromDBAndLogout(cache.getGeocode());

                search = Geocache.searchByGeocode(cache.getGeocode(), null, StoredList.TEMPORARY_LIST_ID, true, null);
                assertNotNull(search);
                assertEquals(0, search.getGeocodes().size());
            }
        });
    }

    /**
     * Test {@link Geocache#searchByGeocode(String, String, int, boolean, CancellableHandler)}
     */
    @MediumTest
    public static void testSearchErrorOccured() {
        withMockedLoginDo(new Runnable() {

            public void run() {
                // non premium cache
                final MockedCache cache = new GC1ZXX2();

                deleteCacheFromDBAndLogout(cache.getGeocode());

                final SearchResult search = Geocache.searchByGeocode(cache.getGeocode(), null, StoredList.TEMPORARY_LIST_ID, true, null);
                assertNotNull(search);
                assertEquals(0, search.getGeocodes().size());
            }
        });
    }

    /**
     * mock the "exclude disabled caches" and "exclude my caches" options for the execution of the runnable
     *
     * @param runnable
     */
    private static void withMockedFilters(Runnable runnable) {
        // backup user settings
        final boolean excludeMine = Settings.isExcludeMyCaches();
        final boolean excludeDisabled = Settings.isExcludeDisabledCaches();
        try {
            // set up settings required for test
            TestSettings.setExcludeMine(false);
            TestSettings.setExcludeDisabledCaches(false);

            runnable.run();

        } finally {
            // restore user settings
            TestSettings.setExcludeMine(excludeMine);
            TestSettings.setExcludeDisabledCaches(excludeDisabled);
        }
    }

    /**
     * Test {@link GCParser#searchByCoords(Geopoint, CacheType, boolean, RecaptchaReceiver)}
     */
    @MediumTest
    public static void testSearchByCoords() {
        withMockedFilters(new Runnable() {

            @Override
            public void run() {
                final SearchResult search = GCParser.searchByCoords(new Geopoint("N 50° 06.654 E 008° 39.777"), CacheType.MYSTERY, false, null);
                assertNotNull(search);
                assertTrue(20 <= search.getGeocodes().size());
                assertTrue(search.getGeocodes().contains("GC1HBMY"));
            }
        });
    }

    /**
     * Test {@link GCParser#searchByOwner(String, CacheType, boolean, RecaptchaReceiver)}
     */
    @MediumTest
    public static void testSearchByOwner() {
        withMockedFilters(new Runnable() {

            @Override
            public void run() {
                final SearchResult search = GCParser.searchByOwner("blafoo", CacheType.MYSTERY, false, null);
                assertNotNull(search);
                assertEquals(3, search.getGeocodes().size());
                assertTrue(search.getGeocodes().contains("GC36RT6"));
            }
        });
    }

    /**
     * Test {@link GCParser#searchByUsername(String, CacheType, boolean, RecaptchaReceiver)}
     */
    @MediumTest
    public static void testSearchByUsername() {
        withMockedFilters(new Runnable() {

            @Override
            public void run() {
                final SearchResult search = GCParser.searchByUsername("blafoo", CacheType.WEBCAM, false, null);
                assertNotNull(search);
                assertEquals(4, search.getTotalCountGC());
                assertTrue(search.getGeocodes().contains("GCP0A9"));
            }
        });
    }

    /**
     * Test {@link ConnectorFactory#searchByViewport(Viewport, String)}
     */
    @MediumTest
    public static void testSearchByViewport() {
        withMockedFilters(new Runnable() {

            @Override
            public void run() {
                // backup user settings
                final Strategy strategy = Settings.getLiveMapStrategy();
                final CacheType cacheType = Settings.getCacheType();

                try {
                    // set up settings required for test
                    TestSettings.setExcludeMine(false);
                    Settings.setCacheType(CacheType.ALL);

                    final GC2CJPF mockedCache = new GC2CJPF();
                    deleteCacheFromDB(mockedCache.getGeocode());

                    final MapTokens tokens = GCLogin.getInstance().getMapTokens();
                    final Viewport viewport = new Viewport(mockedCache, 0.003, 0.003);

                    // check coords for DETAILED
                    Settings.setLiveMapStrategy(Strategy.DETAILED);
                    SearchResult search = ConnectorFactory.searchByViewport(viewport, tokens);
                    assertNotNull(search);
                    assertTrue(search.getGeocodes().contains(mockedCache.getGeocode()));
                    Geocache parsedCache = DataStore.loadCache(mockedCache.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);

                    assertEquals(Settings.isPremiumMember(), mockedCache.getCoords().equals(parsedCache.getCoords()));
                    assertEquals(Settings.isPremiumMember(), parsedCache.isReliableLatLon());

                    // check update after switch strategy to FAST
                    Settings.setLiveMapStrategy(Strategy.FAST);
                    Tile.Cache.removeFromTileCache(mockedCache);

                    search = ConnectorFactory.searchByViewport(viewport, tokens);
                    assertNotNull(search);
                    assertTrue(search.getGeocodes().contains(mockedCache.getGeocode()));
                    parsedCache = DataStore.loadCache(mockedCache.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);

                    assertEquals(Settings.isPremiumMember(), mockedCache.getCoords().equals(parsedCache.getCoords()));
                    assertEquals(Settings.isPremiumMember(), parsedCache.isReliableLatLon());

                } finally {
                    // restore user settings
                    Settings.setLiveMapStrategy(strategy);
                    Settings.setCacheType(cacheType);
                }
            }
        });
    }

    /**
     * Test {@link ConnectorFactory#searchByViewport(Viewport, String)}
     */
    @MediumTest
    public static void testSearchByViewportNotLoggedIn() {
        withMockedLoginDo(new Runnable() {

            public void run() {
                final Strategy strategy = Settings.getLiveMapStrategy();
                final Strategy testStrategy = Strategy.FAST; // FASTEST, FAST or DETAILED for tests
                Settings.setLiveMapStrategy(testStrategy);
                final CacheType cacheType = Settings.getCacheType();

                try {

                    // non premium cache
                    MockedCache cache = new GC2CJPF();
                    deleteCacheFromDBAndLogout(cache.getGeocode());
                    Tile.Cache.removeFromTileCache(cache);
                    Settings.setCacheType(CacheType.ALL);

                    Viewport viewport = new Viewport(cache, 0.003, 0.003);
                    SearchResult search = ConnectorFactory.searchByViewport(viewport, INVALID_TOKEN);

                    assertNotNull(search);
                    assertTrue(search.getGeocodes().contains(cache.getGeocode()));
                    // coords differ
                    final Geocache cacheFromViewport = DataStore.loadCache(cache.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                    Log.d("cgeoApplicationTest.testSearchByViewportNotLoggedIn: Coords expected = " + cache.getCoords());
                    Log.d("cgeoApplicationTest.testSearchByViewportNotLoggedIn: Coords actual = " + cacheFromViewport.getCoords());
                    assertFalse(cache.getCoords().isEqualTo(cacheFromViewport.getCoords(), 1e-3));
                    // depending on the chosen strategy the coords can be reliable or not
                    assertEquals(testStrategy == Strategy.DETAILED, cacheFromViewport.isReliableLatLon());

                    // premium cache
                    cache = new GC2JVEH();
                    deleteCacheFromDBAndLogout(cache.getGeocode());

                    viewport = new Viewport(cache, 0.003, 0.003);
                    search = ConnectorFactory.searchByViewport(viewport, INVALID_TOKEN);

                    assertNotNull(search);
                    // depending on the chosen strategy the cache is part of the search or not
                    assertEquals(testStrategy == Strategy.DETAILED, search.getGeocodes().contains(cache.getGeocode()));

                } finally {
                    Settings.setLiveMapStrategy(strategy);
                    Settings.setCacheType(cacheType);
                }
            }
        });
    }

    /**
     * Test cache parsing. Esp. useful after a GC.com update
     */
    public static void testSearchByGeocodeBasis() {
        for (MockedCache mockedCache : RegExPerformanceTest.MOCKED_CACHES) {
            String oldUser = mockedCache.getMockedDataUser();
            try {
                mockedCache.setMockedDataUser(Settings.getUsername());
                Geocache parsedCache = CgeoApplicationTest.testSearchByGeocode(mockedCache.getGeocode());
                if (null != parsedCache) {
                    Compare.assertCompareCaches(mockedCache, parsedCache, true);
                }
            } finally {
                mockedCache.setMockedDataUser(oldUser);
            }
        }
    }

    /**
     * Caches that are good test cases
     */
    public static void testSearchByGeocodeSpecialties() {
        final Geocache GCV2R9 = CgeoApplicationTest.testSearchByGeocode("GCV2R9");
        Assert.assertEquals("California, United States", GCV2R9.getLocation());

        final Geocache GC1ZXEZ = CgeoApplicationTest.testSearchByGeocode("GC1ZXEZ");
        Assert.assertEquals("Ms.Marple/Mr.Stringer", GC1ZXEZ.getOwnerUserId());
    }

    /** Remove cache from DB and cache to ensure that the cache is not loaded from the database */
    private static void deleteCacheFromDBAndLogout(String geocode) {
        deleteCacheFromDB(geocode);

        GCLogin.getInstance().logout();
        // Modify login data to avoid an automatic login again
        TestSettings.setLogin("c:geo", "c:geo");
        Settings.setMemberStatus("Basic member");
    }

}
