package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class WaypointTypeTest extends AndroidTestCase {

    public static void testFindById() {
        assertEquals(WaypointType.WAYPOINT, WaypointType.findById("random garbage"));
    }

}
