package cgeo.geocaching.activity.waypoint;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.WaypointType;

import com.google.android.apps.common.testing.ui.espresso.action.ViewActions;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;

import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;

import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isClickable;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isNotChecked;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withChild;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;

import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class AddWaypointActivityTest extends AbstractAddWaypointActivityTest {

    public static void testAddWayPointHasTypeSelection() {
        onView(withId(R.id.type)).check(matches(isDisplayed()));
    }

    public static void testDefaultWaypointTypeForTraditional() {
        onView(withId(R.id.type)).check(matches(withChild(withText(WaypointType.WAYPOINT.getL10n()))));
        onView(withId(R.id.name)).check(matches(withText(WaypointType.WAYPOINT.getL10n() + " 1")));
    }

    public static void testFieldsAreEmpty() {
        onView(withId(R.id.note)).check(matches(withText("")));
        onView(withId(R.id.bearing)).check(matches(withText("")));
        onView(withId(R.id.distance)).check(matches(withText("")));
    }

    public static void testNewWaypointNotVisited() {
        onView(withId(R.id.wpt_visited_checkbox)).check(matches(isNotChecked()));
    }

    public static void testSwitchingWaypointTypeChangesWaypointName() {
        WaypointType waypointType = WaypointType.FINAL;

        // verify we don't have a final type yet
        onView(withId(R.id.name)).check(matches(not(withText(waypointType.getL10n()))));

        // open type selector
        onView(withId(R.id.type)).perform(ViewActions.click());

        // select final type
        onData(hasToString(startsWith(waypointType.getL10n()))).inAdapterView(isClickable()).perform(click());

        // verify changed name
        onView(withId(R.id.name)).check(matches(withText(waypointType.getL10n())));
    }
}
