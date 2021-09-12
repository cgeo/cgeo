package cgeo.geocaching.activity.waypoint;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.models.Geocache;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.Suppress;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

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
