package cgeo.geocaching.activity.waypoint;

import android.support.test.espresso.ViewInteraction;
import android.test.suitebuilder.annotation.Suppress;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.models.Geocache;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class AddWaypointMultiTest extends AbstractAddWaypointActivityTest {

    @Override
    protected Geocache createTestCache() {
        final Geocache cache = super.createTestCache();
        cache.setType(CacheType.MULTI);
        return cache;
    }

    @Suppress
    public static void testMysteryDefaultWaypointFinal() {
        final ViewInteraction waypointTypeSelector = onView(withId(R.id.type));
        waypointTypeSelector.check(matches(isDisplayed()));
        waypointTypeSelector.check(matches(withChild(withText(WaypointType.STAGE.getL10n()))));
    }

}
