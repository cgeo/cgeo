package cgeo.geocaching.activity.waypoint;

import cgeo.geocaching.EditWaypointActivity_;

public abstract class AbstractAddWaypointActivityTest extends AbstractWaypointActivityTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityIntent(new EditWaypointActivity_.IntentBuilder_(getInstrumentation().getContext()).geocode(getCache().getGeocode()).get());
        getActivity();
    }
}
