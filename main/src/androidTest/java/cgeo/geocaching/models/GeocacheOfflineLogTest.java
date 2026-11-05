package cgeo.geocaching.models;

import cgeo.geocaching.AboutActivity;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.storage.DataStore;
import static cgeo.geocaching.enumerations.LoadFlags.REMOVE_ALL;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test uses the about activity to avoid side effects like network and GPS being triggered by other activities.
 */
public class GeocacheOfflineLogTest {

    private static final String GEOCODE = "TEST-OFFLINELOG";
    private static final long OLD_VISIT_DATE = 1_600_000_000_000L;
    private static final long LOG_DATE = 1_700_000_000_000L;

    @Rule
    public ActivityScenarioRule<AboutActivity> activityRule = new ActivityScenarioRule<>(AboutActivity.class);

    @Before
    public void setUp() {
        cleanUp();
    }

    @After
    public void tearDown() {
        cleanUp();
    }

    private static void cleanUp() {
        DataStore.clearLogOffline(GEOCODE);
        DataStore.removeCache(GEOCODE, REMOVE_ALL);
    }

    private static Geocache createCache(final long visitedDate) {
        final Geocache cache = new Geocache();
        cache.setGeocode(GEOCODE);
        cache.setDetailed(true);
        cache.getLists().add(StoredList.STANDARD_LIST_ID);
        cache.setVisitedDate(visitedDate);
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
        return cache;
    }

    private void storeOfflineLog(final Geocache cache, final LogType logType, final long date) {
        final OfflineLogEntry logEntry = new OfflineLogEntry.Builder()
                .setCacheGeocode(GEOCODE)
                .setLogType(logType)
                .setDate(date)
                .setLog("offline log")
                .build();
        activityRule.getScenario().onActivity(activity -> cache.storeLogOffline(activity, logEntry));
    }

    private static long visitedDateInDb() {
        return DataStore.loadCache(GEOCODE, LoadFlags.LOAD_ALL_DB_ONLY).getVisitedDate();
    }

    private void assertVisitedDateSetByOfflineLog(final LogType logType) {
        final Geocache cache = createCache(OLD_VISIT_DATE);

        storeOfflineLog(cache, logType, LOG_DATE);

        assertThat(cache.hasLogOffline()).isTrue();
        assertThat(cache.getVisitedDate()).isEqualTo(LOG_DATE);
        assertThat(visitedDateInDb()).isEqualTo(LOG_DATE);
    }

    @Test
    public void storeLogOfflineSetsVisitedDateForFoundLog() {
        assertVisitedDateSetByOfflineLog(LogType.FOUND_IT);
    }

    @Test
    public void storeLogOfflineSetsVisitedDateForDnfLog() {
        assertVisitedDateSetByOfflineLog(LogType.DIDNT_FIND_IT);
    }

    @Test
    public void storeLogOfflineSetsVisitedDateForNonVisitLog() {
        assertVisitedDateSetByOfflineLog(LogType.NOTE);
    }

    @Test
    public void clearOfflineLogKeepsVisitedDate() {
        final Geocache cache = createCache(0);
        storeOfflineLog(cache, LogType.FOUND_IT, LOG_DATE);

        cache.clearOfflineLog(null);

        assertThat(DataStore.loadLogOffline(GEOCODE)).isNull();
        assertThat(cache.hasLogOffline()).isFalse();
        assertThat(cache.getVisitedDate()).isEqualTo(LOG_DATE);
        assertThat(visitedDateInDb()).isEqualTo(LOG_DATE);
    }
}
