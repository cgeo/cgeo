package cgeo.geocaching.waypointactivity;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isNotChecked;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withChild;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.WaypointType;

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
}
