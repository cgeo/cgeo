package cgeo.geocaching.activity.waypoint;

import cgeo.geocaching.EditWaypointActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.test.CgeoTestUtils;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.Suppress;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class AddWaypointActivityTest {

    private Geocache testCache;

    @Before
    public void setUp() throws Exception {
        testCache = CgeoTestUtils.createTestCache();
        DataStore.saveCache(testCache, Collections.singleton(LoadFlags.SaveFlag.CACHE));
    }

    @After
    public void tearDown() {
        CgeoTestUtils.removeCacheCompletely(testCache.getGeocode());
    }

    @Suppress // this test will fail on CI since it does not have FINE-LOCATION granted. see #13798
    @Test
    public void checkPermissions() {
        assertThat(ContextCompat.checkSelfPermission(ApplicationProvider.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION))
                .isEqualTo(PackageManager.PERMISSION_GRANTED);
    }

    @Suppress // this test will fail on CI since it does not have FINE-LOCATION granted. see #13798
    @Test
    public void testAddWayPointHasTypeSelection() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class,
                intent -> intent.putExtra(Intents.EXTRA_GEOCODE, testCache.getGeocode()),
                scenario -> onView(withId(R.id.type)).check(matches(isDisplayed())));
    }

    @Suppress // this test will fail on CI since it does not have FINE-LOCATION granted. see #13798
    @Test
    public void testDefaultWaypointTypeForTraditional() {
        onView(withId(R.id.type)).check(matches(withChild(withText(WaypointType.WAYPOINT.getL10n()))));
        onView(withId(R.id.name)).check(matches(withText(WaypointType.WAYPOINT.getNameForNewWaypoint() + " 1")));
    }

    @Suppress // this test will fail on CI since it does not have FINE-LOCATION granted. see #13798
    @Test
    public void testFieldsAreEmpty() {
        onView(withId(R.id.note)).check(matches(withText("")));
        onView(withId(R.id.user_note)).check(matches(withText("")));
        onView(withId(R.id.bearing)).check(matches(withText("")));
        onView(withId(R.id.distance)).check(matches(withText("")));
    }

    @Suppress // this test will fail on CI since it does not have FINE-LOCATION granted. see #13798
    @Test
    public void testNewWaypointNotVisited() {
        onView(withId(R.id.wpt_visited_checkbox)).check(matches(isNotChecked()));
    }

    // disabled because of issues with testing on Lollipop

    @Suppress // this test will fail on CI since it does not have FINE-LOCATION granted. see #13798
    @Test
    public void testSwitchingWaypointTypeChangesWaypointName() {
        final WaypointType waypointType = WaypointType.FINAL;

        // verify we don't have a final type yet
        onView(withId(R.id.name)).check(matches(not(withText(waypointType.getL10n()))));

        // open type selector
        onView(withId(R.id.type)).perform(click());

        // select final type
        onData(hasToString(startsWith(waypointType.getL10n()))).inAdapterView(isClickable()).perform(click());

        // verify changed name
        onView(withId(R.id.name)).check(matches(withText(waypointType.getL10n())));
    }
}
