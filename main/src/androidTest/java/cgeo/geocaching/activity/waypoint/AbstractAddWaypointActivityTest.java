package cgeo.geocaching.activity.waypoint;

// TODO: this class will no longer be needed when GUI-Tests are switched to new Espresso.
// TODO: This can only be done after #13761 is resolved
public abstract class AbstractAddWaypointActivityTest extends AbstractWaypointActivityTest {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
//        setActivityIntent(new EditWaypointActivity.IntentBuilder_(getInstrumentation().getContext()).geocode(getCache().getGeocode()).get());
//        getActivity();
    }
}
