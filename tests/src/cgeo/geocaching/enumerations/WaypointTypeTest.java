package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class WaypointTypeTest extends AndroidTestCase {

    public void testFindById() {
        assertEquals(WaypointType.WAYPOINT, WaypointType.findById("random garbage"));
    }

}
