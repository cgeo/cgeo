package cgeo.geocaching.activity.waypoint;

import android.test.suitebuilder.annotation.Suppress;

import cgeo.geocaching.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
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
