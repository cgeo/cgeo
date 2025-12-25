// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.activity

import cgeo.geocaching.EditWaypointActivity
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.ProjectionType
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.test.CgeoTemporaryCacheRule
import cgeo.geocaching.test.CgeoTestUtils
import cgeo.geocaching.test.NotForIntegrationTests

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager

import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText

import org.junit.Rule
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers.hasToString
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.startsWith

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class WaypointActivityTest {

    @Rule
    var tradi: CgeoTemporaryCacheRule = CgeoTemporaryCacheRule()
    @Rule
    var mystery: CgeoTemporaryCacheRule = CgeoTemporaryCacheRule(
            c -> c.setType(CacheType.MYSTERY))
    @Rule
    var multi: CgeoTemporaryCacheRule = CgeoTemporaryCacheRule(
            c -> c.setType(CacheType.MULTI))

    // this test will fail on CI since it does not have FINE-LOCATION granted. see #13798
    @NotForIntegrationTests
    @Test
    public Unit checkPermissions() {
        assertThat(ContextCompat.checkSelfPermission(ApplicationProvider.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION))
                .isEqualTo(PackageManager.PERMISSION_GRANTED)
    }

    @Test
    public Unit addWayPointHasTypeSelection() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class,
                intent -> intent.putExtra(Intents.EXTRA_GEOCODE, tradi.getCache().getGeocode()),
                scenario -> onView(withId(R.id.type)).check(matches(isDisplayed())))
    }

    @Test
    public Unit defaultWaypointTypeForTraditional() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> waypointForCache(intent, tradi.getCache()),
                scenario -> {
                    onView(withId(R.id.type)).check(matches(withChild(withText(WaypointType.WAYPOINT.getL10n()))))
                    onView(withId(R.id.name)).check(matches(withText(WaypointType.WAYPOINT.getNameForNewWaypoint() + " 1")))
                }
        )
    }

    @Test
    public Unit fieldsAreEmpty() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> waypointForCache(intent, tradi.getCache()),
                scenario -> {
                    onView(withId(R.id.note)).check(matches(withText("")))
                    onView(withId(R.id.user_note)).check(matches(withText("")))
                }
        )
    }

    @Test
    public Unit newWaypointNotVisited() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> intent.putExtra(Intents.EXTRA_GEOCODE, tradi.getCache().getGeocode()),
                scenario -> onView(withId(R.id.wpt_visited_checkbox)).check(matches(isNotChecked()))
        )
    }

    // disabled because of issues with testing on Lollipop

    @Test
    public Unit switchingWaypointTypeChangesWaypointName() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> waypointForCache(intent, tradi.getCache()),
                scenario -> {
                    val waypointType: WaypointType = WaypointType.FINAL

                    // verify we don't have a final type yet
                    onView(withId(R.id.name)).check(matches(not(withText(waypointType.getL10n()))))

                    // open type selector
                    onView(withId(R.id.type)).perform(click())

                    // select final type
                    onData(hasToString(startsWith(waypointType.getL10n()))).inAdapterView(isClickable()).perform(click())

                    // verify changed name
                    onView(withId(R.id.name)).check(matches(withText(waypointType.getNameForNewWaypoint())))
                }
        )
    }

    @Test
    public Unit mysteryDefaultWaypointFinal() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> waypointForCache(intent, mystery.getCache()),
                scenario -> {
                    val waypointTypeSelector: ViewInteraction = onView(withId(R.id.type))
                    waypointTypeSelector.check(matches(isDisplayed()))
                    waypointTypeSelector.check(matches(withChild(withText(WaypointType.FINAL.getL10n()))))
                })
    }

    @Test
    public Unit multiDefaultWaypointStage() {
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> waypointForCache(intent, multi.getCache()),
                scenario -> {
                    val waypointTypeSelector: ViewInteraction = onView(withId(R.id.type))
                    waypointTypeSelector.check(matches(isDisplayed()))
                    waypointTypeSelector.check(matches(withChild(withText(WaypointType.STAGE.getL10n()))))
                })
    }

    @Test
    public Unit openExistingWaypoint() {
        val waypoint: Waypoint = createWaypoint()

        tradi.getCache().addOrChangeWaypoint(waypoint, true)
        CgeoTestUtils.executeForActivity(EditWaypointActivity.class, intent -> {
            waypointForCache(intent, multi.getCache())
            intent.putExtra(Intents.EXTRA_WAYPOINT_ID, waypoint.getId())
        }, scenario -> {
            val name: String = waypoint.getName()
            assertThat(name).isNotEmpty()
            onView(withId(R.id.name)).check(matches(withText(name)))

            val note: String = waypoint.getNote()
            assertThat(note).isNotEmpty()
            onView(withId(R.id.user_note)).check(matches(withText(containsString(note.trim()))))

            onView(withId(R.id.type)).check(matches(withChild(withText(waypoint.getWaypointType().getL10n()))))

            onView(withId(R.id.projection_type)).check(matches(withChild(withText(ProjectionType.NO_PROJECTION.getL10n()))))

        })
    }

    private static Unit waypointForCache(final Intent intent, final Geocache cache) {
        intent.putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode())
    }

    private static Waypoint createWaypoint() {
        val waypoint: Waypoint = Waypoint("Test waypoint", WaypointType.PUZZLE, true)
        waypoint.setNote("Test note")
        return waypoint
    }

}
