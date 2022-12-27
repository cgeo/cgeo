package cgeo.geocaching.activity.waypoint;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.models.Waypoint;

// TODO: this class will no longer be needed when GUI-Tests are switched to new Espresso.
// TODO: This can only be done after #13761 is resolved
public abstract class AbstractEditWaypointActivityTest extends AbstractWaypointActivityTest {
    private Waypoint waypoint;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createWaypoint();
        getCache().addOrChangeWaypoint(waypoint, true);
//        final int waypointId = getCache().getWaypoints().get(0).getId();
//        setActivityIntent(new EditWaypointActivity.IntentBuilder_(getInstrumentation().getContext()).geocode(getCache().getGeocode()).waypointId(waypointId).get());
//        getActivity();
    }

    private void createWaypoint() {
        waypoint = new Waypoint("Test waypoint", WaypointType.PUZZLE, true);
        waypoint.setNote("Test note");
    }

    protected final Waypoint getWaypoint() {
        return waypoint;
    }
}
