package cgeo.geocaching.activity;

import cgeo.geocaching.EditWaypointActivity;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.test.CgeoTemporaryCacheRule;
import cgeo.geocaching.test.CgeoTestUtils;
import cgeo.geocaching.test.NotForIntegrationTests;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
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

import org.junit.Rule;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class WaypointActivityTest {

    @Rule
    public CgeoTemporaryCacheRule tradi = new CgeoTemporaryCacheRule();
    @Rule
    public CgeoTemporaryCacheRule mystery = new CgeoTemporaryCacheRule(
            c -> c.setType(CacheType.MYSTERY));
    @Rule
    public CgeoTemporaryCacheRule multi = new CgeoTemporaryCacheRule(
            c -> c.setType(CacheType.MULTI));

    // this test will fail on CI since it does not have FINE-LOCATION granted. see #13798
    @NotForIntegrationTests
    @Test
    public void checkPermissions() {
        assertThat(ContextCompat.checkSelfPermission(ApplicationProvider.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION))
                .isEqualTo(PackageManager.PERMISSION_GRANTED);
    }

    @Test
    public void addWayPointHasTypeSelection() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class,
                intent -> intent.putExtra(Intents.EXTRA_GEOCODE, tradi.getCache().getGeocode()),
                scenario -> onView(withId(R.id.type)).check(matches(isDisplayed())));
    }

    @Test
    public void defaultWaypointTypeForTraditional() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> waypointForCache(intent, tradi.getCache()),
                scenario -> {
                    onView(withId(R.id.type)).check(matches(withChild(withText(WaypointType.WAYPOINT.getL10n()))));
                    onView(withId(R.id.name)).check(matches(withText(WaypointType.WAYPOINT.getNameForNewWaypoint() + " 1")));
                }
        );
    }

    @Test
    public void fieldsAreEmpty() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> waypointForCache(intent, tradi.getCache()),
                scenario -> {
                    onView(withId(R.id.note)).check(matches(withText("")));
                    onView(withId(R.id.user_note)).check(matches(withText("")));
                    onView(withId(R.id.bearing)).check(matches(withText("")));
                    onView(withId(R.id.distance)).check(matches(withText("")));
                }
        );
    }

    @Test
    public void newWaypointNotVisited() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> intent.putExtra(Intents.EXTRA_GEOCODE, tradi.getCache().getGeocode()),
                scenario -> onView(withId(R.id.wpt_visited_checkbox)).check(matches(isNotChecked()))
        );
    }

    // disabled because of issues with testing on Lollipop

    @Test
    public void switchingWaypointTypeChangesWaypointName() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> waypointForCache(intent, tradi.getCache()),
                scenario -> {
                    final WaypointType waypointType = WaypointType.FINAL;

                    // verify we don't have a final type yet
                    onView(withId(R.id.name)).check(matches(not(withText(waypointType.getL10n()))));

                    // open type selector
                    onView(withId(R.id.type)).perform(click());

                    // select final type
                    onData(hasToString(startsWith(waypointType.getL10n()))).inAdapterView(isClickable()).perform(click());

                    // verify changed name
                    onView(withId(R.id.name)).check(matches(withText(waypointType.getNameForNewWaypoint())));
                }
        );
    }

    @Test
    public void mysteryDefaultWaypointFinal() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> waypointForCache(intent, mystery.getCache()),
                scenario -> {
                    final ViewInteraction waypointTypeSelector = onView(withId(R.id.type));
                    waypointTypeSelector.check(matches(isDisplayed()));
                    waypointTypeSelector.check(matches(withChild(withText(WaypointType.FINAL.getL10n()))));
                });
    }

    @Test
    public void multiDefaultWaypointStage() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> waypointForCache(intent, multi.getCache()),
                scenario -> {
                    final ViewInteraction waypointTypeSelector = onView(withId(R.id.type));
                    waypointTypeSelector.check(matches(isDisplayed()));
                    waypointTypeSelector.check(matches(withChild(withText(WaypointType.STAGE.getL10n()))));
                });
    }

    @Test
    public void openExistingWaypoint() {
        final Waypoint waypoint = createWaypoint();

        tradi.getCache().addOrChangeWaypoint(waypoint, true);
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> {
            waypointForCache(intent, multi.getCache());
            intent.putExtra(Intents.EXTRA_WAYPOINT_ID, waypoint.getId());
        }, scenario -> {
            final String name = waypoint.getName();
            assertThat(name).isNotEmpty();
            onView(withId(R.id.name)).check(matches(withText(name)));

            final String note = waypoint.getNote();
            assertThat(note).isNotEmpty();
            onView(withId(R.id.user_note)).check(matches(withText(containsString(note.trim()))));

            onView(withId(R.id.type)).check(matches(withChild(withText(waypoint.getWaypointType().getL10n()))));

        });
    }

    private static void waypointForCache(final Intent intent, final Geocache cache) {
        intent.putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode());
    }

    private static Waypoint createWaypoint() {
        final Waypoint waypoint = new Waypoint("Test waypoint", WaypointType.PUZZLE, true);
        waypoint.setNote("Test note");
        return waypoint;
    }

}
