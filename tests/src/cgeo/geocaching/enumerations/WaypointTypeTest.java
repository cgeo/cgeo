package cgeo.geocaching.enumerations;

import static org.assertj.core.api.Assertions.assertThat;

import android.test.AndroidTestCase;

public class WaypointTypeTest extends AndroidTestCase {

    public static void testFindById() {
        assertThat(WaypointType.findById("random garbage")).isEqualTo(WaypointType.WAYPOINT);
    }

}
