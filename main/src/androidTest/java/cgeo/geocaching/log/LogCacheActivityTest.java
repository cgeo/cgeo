package cgeo.geocaching.log;

import cgeo.geocaching.Intents;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.test.CgeoTemporaryCacheRule;
import cgeo.geocaching.test.CgeoTestUtils;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

@MediumTest
public class LogCacheActivityTest {

    private static final String MISSING_GEOCODE = "TEST_MISSING_18743";
    private static final String DRAFT_TEXT = "offline draft from LogCacheActivityTest";

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
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertThat(scenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
        }

        assertThat(DataStore.loadLogOffline(MISSING_GEOCODE)).isNull();
    }

    @Test
    public void initializedActivityStillSavesChangedDraftOnNormalFinish() throws InterruptedException {
        final String geocode = cacheRule.getCache().getGeocode();
        try (ActivityScenario<LogCacheActivity> scenario = launchLogActivity(geocode)) {
            scenario.onActivity(activity -> {
                activity.binding.log.setText(DRAFT_TEXT);
                activity.finish();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertThat(scenario.getState()).isEqualTo(Lifecycle.State.DESTROYED);
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

    private static OfflineLogEntry waitForOfflineLog(final String geocode) throws InterruptedException {
        final long timeout = System.currentTimeMillis() + 5000;
        OfflineLogEntry result;
        do {
            result = DataStore.loadLogOffline(geocode);
            if (result != null) {
                return result;
            }
            Thread.sleep(50);
        } while (System.currentTimeMillis() < timeout);
        return null;
    }
}
