package cgeo.geocaching.log;

import cgeo.geocaching.Intents;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.test.CgeoTemporaryCacheRule;
import cgeo.geocaching.test.CgeoTestUtils;

import android.content.Intent;
import android.os.SystemClock;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

@MediumTest
public class LogCacheActivityTest {

    private static final String MISSING_GEOCODE = "TEST_MISSING_18743";
    private static final String DRAFT_TEXT = "offline draft from LogCacheActivityTest";
    private static final long WAIT_TIMEOUT_MILLIS = 5000;
    private static final long POLL_INTERVAL_MILLIS = 50;

    @Rule
    public final CgeoTemporaryCacheRule cacheRule = new CgeoTemporaryCacheRule();

    @Before
    public void setUp() {
        CgeoTestUtils.removeCacheCompletely(MISSING_GEOCODE);
        DataStore.clearLogOffline(MISSING_GEOCODE);
        DataStore.clearLogOffline(cacheRule.getCache().getGeocode());
    }

    @After
    public void tearDown() {
        DataStore.clearLogOffline(MISSING_GEOCODE);
        DataStore.clearLogOffline(cacheRule.getCache().getGeocode());
        CgeoTestUtils.removeCacheCompletely(MISSING_GEOCODE);
    }

    @Test
    public void missingCacheFinishesWithoutSaving() {
        try (ActivityScenario<LogCacheActivity> scenario = launchLogActivity(MISSING_GEOCODE)) {
            waitForDestroyed(scenario);
        }

        assertThat(DataStore.loadLogOffline(MISSING_GEOCODE)).isNull();
    }

    @Test
    public void initializedActivityStillSavesChangedDraftOnNormalFinish() {
        final String geocode = cacheRule.getCache().getGeocode();
        try (ActivityScenario<LogCacheActivity> scenario = launchLogActivity(geocode)) {
            scenario.onActivity(activity -> {
                activity.binding.log.setText(DRAFT_TEXT);
                activity.finish();
            });
            waitForDestroyed(scenario);
        }

        final OfflineLogEntry savedLog = waitForOfflineLog(geocode);
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.log).isEqualTo(DRAFT_TEXT);
    }

    private static ActivityScenario<LogCacheActivity> launchLogActivity(final String geocode) {
        final Intent intent = new Intent(ApplicationProvider.getApplicationContext(), LogCacheActivity.class);
        intent.putExtra(Intents.EXTRA_GEOCODE, geocode);
        return ActivityScenario.launch(intent);
    }

    private static void waitForDestroyed(final ActivityScenario<LogCacheActivity> scenario) {
        final long timeout = SystemClock.elapsedRealtime() + WAIT_TIMEOUT_MILLIS;
        while (SystemClock.elapsedRealtime() < timeout) {
            if (scenario.getState() == Lifecycle.State.DESTROYED) {
                return;
            }
            SystemClock.sleep(POLL_INTERVAL_MILLIS);
        }
        assertThat(scenario.getState())
                .as("Activity did not reach DESTROYED within %s ms", WAIT_TIMEOUT_MILLIS)
                .isEqualTo(Lifecycle.State.DESTROYED);
    }

    private static OfflineLogEntry waitForOfflineLog(final String geocode) {
        final long timeout = SystemClock.elapsedRealtime() + WAIT_TIMEOUT_MILLIS;
        OfflineLogEntry result;
        do {
            result = DataStore.loadLogOffline(geocode);
            if (result != null && DRAFT_TEXT.equals(result.log)) {
                return result;
            }
            SystemClock.sleep(POLL_INTERVAL_MILLIS);
        } while (SystemClock.elapsedRealtime() < timeout);
        assertThat(result)
                .as("Expected offline log was not saved within %s ms", WAIT_TIMEOUT_MILLIS)
                .isNotNull();
        assertThat(result.log)
                .as("Expected offline log text was not saved within %s ms", WAIT_TIMEOUT_MILLIS)
                .isEqualTo(DRAFT_TEXT);
        return result;
    }
}
