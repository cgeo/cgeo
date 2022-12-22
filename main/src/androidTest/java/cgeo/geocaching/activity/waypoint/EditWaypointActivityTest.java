package cgeo.geocaching.activity.waypoint;

import cgeo.geocaching.R;

import androidx.test.filters.Suppress;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class EditWaypointActivityTest extends AbstractEditWaypointActivityTest {

    @Suppress
    public void testFieldsAreNotEmpty() {
        final String name = getWaypoint().getName();
        assertThat(name).isNotEmpty();
        onView(withId(R.id.name)).check(matches(withText(name)));

        final String note = getWaypoint().getNote();
        assertThat(note).isNotEmpty();
        onView(withId(R.id.note)).check(matches(withText(note)));

        final String userNote = getWaypoint().getUserNote();
        assertThat(userNote).isNotEmpty();
        onView(withId(R.id.user_note)).check(matches(withText(userNote)));

        onView(withId(R.id.type)).check(matches(withChild(withText(getWaypoint().getWaypointType().getL10n()))));
    }

}
