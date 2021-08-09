package cgeo.geocaching.activity.waypoint;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.WaypointType;

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

import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class AddWaypointActivityTest extends AbstractAddWaypointActivityTest {

    @Suppress
    public static void testAddWayPointHasTypeSelection() {
        onView(withId(R.id.type)).check(matches(isDisplayed()));
    }

    // disabled because of issues with testing on Lollipop

    @Suppress
    public static void testDefaultWaypointTypeForTraditional() {
        onView(withId(R.id.type)).check(matches(withChild(withText(WaypointType.WAYPOINT.getL10n()))));
        onView(withId(R.id.name)).check(matches(withText(WaypointType.WAYPOINT.getL10n() + " 1")));
    }

    @Suppress
    public static void testFieldsAreEmpty() {
        onView(withId(R.id.note)).check(matches(withText("")));
        onView(withId(R.id.user_note)).check(matches(withText("")));
        onView(withId(R.id.bearing)).check(matches(withText("")));
        onView(withId(R.id.distance)).check(matches(withText("")));
    }

    @Suppress
    public static void testNewWaypointNotVisited() {
        onView(withId(R.id.wpt_visited_checkbox)).check(matches(isNotChecked()));
    }

    // disabled because of issues with testing on Lollipop

    @Suppress
    public static void testSwitchingWaypointTypeChangesWaypointName() {
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
