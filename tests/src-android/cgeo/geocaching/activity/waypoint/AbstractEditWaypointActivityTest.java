package cgeo.geocaching.activity.waypoint;

import cgeo.geocaching.EditWaypointActivity_;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.models.Waypoint;

public abstract class AbstractEditWaypointActivityTest extends AbstractWaypointActivityTest {
    private Waypoint waypoint;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createWaypoint();
        getCache().addOrChangeWaypoint(waypoint, true);
        final int waypointId = getCache().getWaypoints().get(0).getId();
        setActivityIntent(new EditWaypointActivity_.IntentBuilder_(getInstrumentation().getContext()).geocode(getCache().getGeocode()).waypointId(waypointId).get());
        getActivity();
    }

    private void createWaypoint() {
        waypoint = new Waypoint("Test waypoint", WaypointType.PUZZLE, true);
        waypoint.setNote("Test note");
    }

    protected final Waypoint getWaypoint() {
        return waypoint;
    }
}
