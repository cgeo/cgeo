package cgeo.geocaching;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.connector.gc.GCMemberState;
import cgeo.geocaching.connector.gc.GCParser;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.test.CgeoTestUtils;
import cgeo.geocaching.test.mock.GC2JVEH;
import cgeo.geocaching.test.mock.GC3FJ5F;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.TextUtils;
import cgeo.test.Compare;

import androidx.annotation.Nullable;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import java.util.GregorianCalendar;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * The c:geo application test. It can be used for tests that require an
 * application and/or context.
 */
public class CgeoApplicationTest {

    @Test
    @MediumTest
    public void testRegEx() {
        final String page = MockedCache.readCachePage("GC2CJPF");
        assertThat(TextUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME, true, "???")).isEqualTo("abft");
    }

    /**
     * The name 'test preconditions' is a convention to signal that if this test
     * doesn't pass, the test case was not set up properly and it might explain
     * any and all failures in other tests. This is not guaranteed to run before
     * other tests, as junit uses reflection to find the tests.
     */
    @Test
    @SmallTest
    public void testPreconditions() {
        assertThat(GCLogin.getInstance().login()).as("User and password must be provided").isEqualTo(StatusCode.NO_ERROR);
        assertThat(Settings.isGCPremiumMember()).as("User must be premium member for some of the tests to succeed").isTrue();
    }

    /**
     * Test {@link GCParser#searchTrackable(String, String, String)}
     */
    @Test
    @MediumTest
    public void testSearchTrackableNotExisting() {
        final Trackable tb = GCParser.searchTrackable("123456", null, null);
        assertThat(tb).isNull();
    }

    /**
     * Test {@link GCParser#searchTrackable(String, String, String)}
     */
    @Test
    @MediumTest
    public void testSearchTrackable() {
        final Trackable tb = GCParser.searchTrackable("TB2J1VZ", null, null);
        assertThat(tb).isNotNull();
        // fix data
        assertThat(tb.getGuid()).isEqualTo("aefffb86-099f-444f-b132-605436163aa8");
        assertThat(tb.getGeocode()).isEqualTo("TB2J1VZ");
        assertThat(tb.getIconUrl()).endsWith("://www.geocaching.com/images/WptTypes/21.gif");
        assertThat(tb.getName()).isEqualTo("blafoo's Children Music CD");
        assertThat(tb.getType()).isEqualTo("Travel Bug Dog Tag");
        assertThat(tb.getReleased()).isEqualTo(new GregorianCalendar(2009, 8 - 1, 24).getTime());
        assertThat(tb.getLogDate()).isNull();
        assertThat(tb.getLogType()).isNull();
        assertThat(tb.getLogGuid()).isNull();
        assertThat(tb.getOrigin()).isEqualTo("Niedersachsen, Germany");
        assertThat(tb.getOwner()).isEqualTo("blafoo");
        assertThat(tb.getOwnerGuid()).isEqualTo("0564a940-8311-40ee-8e76-7e91b2cf6284");
        assertThat(tb.getGoal()).isEqualTo("Kinder erfreuen.<br><br>Make children happy.");
        assertThat(tb.getDetails()).startsWith("Auf der CD sind");
        // the host of the image can vary
        assertThat(tb.getImage()).endsWith("geocaching.com/track/large/38382780-87a7-4393-8393-78841678ee8c.jpg");
        // Following data can change over time
        assertThat(tb.getDistance()).isGreaterThanOrEqualTo(10617.8f);
        assertThat(tb.getLogs().size()).isGreaterThanOrEqualTo(10);
        assertThat(tb.getSpottedType() == Trackable.SPOTTED_CACHE || tb.getSpottedType() == Trackable.SPOTTED_USER || tb.getSpottedType() == Trackable.SPOTTED_UNKNOWN).isTrue();
        // no assumption possible: assertThat(tb.getSpottedGuid()).isEqualTo("faa2d47d-19ea-422f-bec8-318fc82c8063");
        // no assumption possible: assertThat(tb.getSpottedName()).isEqualTo("Nice place for a break cache");

        // we can't check specifics in the log entries since they change, but we can verify data was parsed
        for (final LogEntry log : tb.getLogs()) {
            assertThat(log.date).isGreaterThan(0);
            assertThat(log.author).isNotEmpty();
            if (log.logType == LogType.PLACED_IT || log.logType == LogType.RETRIEVED_IT) {
                assertThat(log.cacheName).isNotEmpty();
                assertThat(log.cacheGuid).isNotEmpty();
            } else {
                assertThat(log.logType).isNotEqualTo(LogType.UNKNOWN);
            }
        }
    }

    /**
     * Test log and spotted states for {@link GCParser#searchTrackable(String, String, String)}
     * Other parameters are not fully covered in this test case, as we already have {@link CgeoApplicationTest#testSearchTrackable()}
     */
    @Test
    @MediumTest
    public void testSearchTrackableSpottedLogState() {
        final Trackable tb = GCParser.searchTrackable("TB4BPQK", null, null);
        assertThat(tb).isNotNull();
        // some very basic constant data
        assertThat(tb.getGuid()).isEqualTo("6d634cc3-f156-4b74-a4b8-823304683765");
        assertThat(tb.getName()).isEqualTo("Alberta travel nord to south");
        assertThat(tb.getGeocode()).isEqualTo("TB4BPQK");

        assertThat(tb.getOwner()).isEqualTo("Die Eybacher");
        assertThat(tb.getOwnerGuid()).isEqualTo("5888ea6c-413b-4b60-959d-d3d729ad642b");

        // Following data can potentially change over time. However, is's very unlikely as the trackable is lost since years
        assertThat(tb.getSpottedType()).isEqualTo(Trackable.SPOTTED_USER);
        assertThat(tb.getSpottedGuid()).isEqualTo("83858f68-ba77-4342-ad89-83aebcf37f86");
        assertThat(tb.getSpottedName()).isEqualTo("cachertimsi");

        final LogEntry lastLog = tb.getLogs().get(0);
        assertThat(lastLog.author).isEqualTo("cachertimsi");
        assertThat(lastLog.date).isEqualTo(new GregorianCalendar(2013, 11 - 1, 5).getTimeInMillis());
        assertThat(lastLog.logType).isEqualTo(LogType.RETRIEVED_IT);
        assertThat(lastLog.cacheName).isEqualTo("TB / Coin Hotel Fehmarn");
        assertThat(lastLog.cacheGuid).isEqualTo("e93eeddd-a3f0-4bf1-a056-6acc1c5dff1f");
        assertThat(lastLog.serviceLogId).isEqualTo("817608e9-850d-428a-9318-442a14b7b631");
        assertThat(lastLog.log).isEqualTo("Das tb Hotel war sehr schön");
    }

    /**
     * Test {@link Geocache#searchByGeocode(String, String, boolean, DisposableHandler)}
     */
    @Nullable
    public Geocache searchByGeocode(final String geocode) {
        final SearchResult search = Geocache.searchByGeocode(geocode, null, true, null);
        assertThat(search).isNotNull();
        if (Settings.isGCPremiumMember() || search.getError() == StatusCode.NO_ERROR || search.getError() == StatusCode.PREMIUM_ONLY) {
            assertThat(search.getGeocodes()).containsExactly(geocode);
            return DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        }
        assertThat(search.getGeocodes()).isEmpty();
        return null;
    }

    /**
     * Test {@link Geocache#searchByGeocode(String, String, boolean, DisposableHandler)}
     */
    @Test
    @MediumTest
    public void testSearchByGeocodeNotExisting() {
        final SearchResult search = Geocache.searchByGeocode("GC1", null, true, null);
        assertThat(search).isNotNull();
        assertThat(search.getError()).isEqualTo(StatusCode.CACHE_NOT_FOUND);
    }

    /**
     * Set the login data to the cgeo login, run the given Runnable, and restore the login.
     */
    private static void withMockedLoginDo(final Runnable runnable) {
        final Credentials credentials = Settings.getGcCredentials();
        final GCMemberState memberStatus = Settings.getGCMemberStatus();

        try {
            runnable.run();
        } finally {
            // restore user and password
            TestSettings.setLogin(credentials);
            Settings.setGCMemberStatus(memberStatus);
            GCLogin.getInstance().login();
        }
    }

    /**
     * Test {@link Geocache#searchByGeocode(String, String, boolean, DisposableHandler)}
     */
    @MediumTest
    @Test
    public void testSearchByGeocodeNotLoggedIn() {
        withMockedLoginDo(() -> {
            // non premium cache
            MockedCache cache = new GC3FJ5F();

            deleteCacheFromDBAndLogout(cache.getGeocode());

            SearchResult search = Geocache.searchByGeocode(cache.getGeocode(), null, true, null);
            assertThat(search).isNotNull();
            assertThat(search.getGeocodes()).containsExactly(cache.getGeocode());
            Geocache searchedCache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
            // coords must be null if the user is not logged in
            assertThat(searchedCache).isNotNull();
            assertThat(searchedCache.getCoords()).isNull();

            // premium cache. Not fully visible to guests
            cache = new GC2JVEH();

            deleteCacheFromDBAndLogout(cache.getGeocode());

            search = Geocache.searchByGeocode(cache.getGeocode(), null, true, null);
            assertThat(search).isNotNull();
            searchedCache = search.getFirstCacheFromResult(LoadFlags.LOAD_CACHE_OR_DB);
            assertThat(searchedCache).isNotNull();
            assertThat(searchedCache.getCoords()).isNull();
            assertThat(searchedCache.getName()).isEqualTo(cache.getName());
            assertThat(searchedCache.getDifficulty()).isEqualTo(cache.getDifficulty());
            assertThat(searchedCache.getTerrain()).isEqualTo(cache.getTerrain());
            assertThat(searchedCache.getGeocode()).isEqualTo(cache.getGeocode());
            assertThat(searchedCache.getSize()).isEqualTo(cache.getSize());
            assertThat(searchedCache.getType()).isEqualTo(cache.getType());
            // it's not possible in guest sessions to distinguish whether a PM-only cache is disabled or archived
            assertThat(searchedCache.isDisabled()).isEqualTo(cache.isDisabled() || cache.isArchived());
        });
    }

    /**
     * mock the "exclude disabled caches" and "exclude my caches" options for the execution of the runnable
     */
    private static void withMockedFilters(final Runnable runnable) {
        runnable.run();
    }

    @MediumTest
    @Test
    public void testSearchByCoords() {
        withMockedFilters(() -> {
            final SearchResult search = GCParser.searchByCoords(GCConnector.getInstance(), new Geopoint("N 50° 06.654 E 008° 39.777"));
            assertThat(search).isNotNull();
            assertThat(search.getGeocodes().size()).isGreaterThanOrEqualTo(20);
            assertThat(search.getGeocodes()).contains("GC1HBMY");
        });
    }

    @MediumTest
    @Test
    public void testSearchByOwner() {
        withMockedFilters(() -> {
            final SearchResult search = GCParser.searchByOwner(GCConnector.getInstance(), "Lineflyer");
            assertThat(search).isNotNull();
            assertThat(search.getGeocodes().size()).isGreaterThanOrEqualTo(20);
            assertThat(search.getGeocodes()).contains("GC7J99X");
        });
    }

    @MediumTest
    @Test
    public void testSearchByUsername() {
        withMockedFilters(() -> {
            final SearchResult search = GCParser.searchByUsername(GCConnector.getInstance(), "blafoo", null, false);
            assertThat(search).isNotNull();
            // we cannot check for a specific geocode, as we cannot know which caches 'blafoo' has recently found.
            assertThat(search.getGeocodes().size()).isGreaterThanOrEqualTo(20);
        });
    }

    /**
     * Test {@link ConnectorFactory#searchByViewport(Viewport)}
     */
    @MediumTest
    @Test
    public void testSearchByViewport() {
        withMockedFilters(() -> {

            final GC3FJ5F mockedCache = new GC3FJ5F();
            CgeoTestUtils.deleteCacheFromDB(mockedCache.getGeocode());

            final Viewport viewport = new Viewport(mockedCache, 0.003, 0.003);

            // check coords
            final SearchResult search = ConnectorFactory.searchByViewport(viewport);
            assertThat(search).isNotNull();
            assertThat(search.getGeocodes()).contains(mockedCache.getGeocode());
            final Geocache parsedCache = DataStore.loadCache(mockedCache.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
            assert parsedCache != null;
            assertThat(parsedCache).isNotNull();
            assertThat(mockedCache.getCoords()).isEqualTo(parsedCache.getCoords());

        });
    }

    /**
     * Test cache parsing. Esp. useful after a GC.com update
     */
    @MediumTest
    @Test
    public void testSearchByGeocodeBasis() {
        for (final MockedCache mockedCache : MockedCache.MOCKED_CACHES) {
            final String oldUser = mockedCache.getMockedDataUser();
            try {
                mockedCache.setMockedDataUser(Settings.getUserName());
                final Geocache parsedCache = new CgeoApplicationTest().searchByGeocode(mockedCache.getGeocode());
                Compare.assertCompareCaches(mockedCache, parsedCache, true);
            } finally {
                mockedCache.setMockedDataUser(oldUser);
            }
        }
    }

    /**
     * Caches that are good test cases
     */
    @MediumTest
    @Test
    public void testSearchByGeocodeSpecialties() {
        final Geocache gcv2r9 = new CgeoApplicationTest().searchByGeocode("GCV2R9");
        assertThat(gcv2r9).isNotNull();
        assertThat(gcv2r9.getLocation()).isEqualTo("California, United States");

        final Geocache gc1zxez = new CgeoApplicationTest().searchByGeocode("GC1ZXEZ");
        assertThat(gc1zxez).isNotNull();
        assertThat(gc1zxez.getOwnerUserId()).isEqualTo("Ms.Marple/Mr.Stringer");
        assertThat(gc1zxez.getOwnerGuid()).isEqualTo("b66a625c-0266-43a7-9e7c-efecb9b2929a");
    }

    /**
     * Remove cache from DB and cache to ensure that the cache is not loaded from the database
     */
    private void deleteCacheFromDBAndLogout(final String geocode) {
        CgeoTestUtils.deleteCacheFromDB(geocode);

        GCLogin.getInstance().logout();
        // Modify login data to avoid an automatic login again
        TestSettings.setLogin(new Credentials("c:geo", "c:geo"));
        Settings.setGCMemberStatus(GCMemberState.BASIC);
    }
}
