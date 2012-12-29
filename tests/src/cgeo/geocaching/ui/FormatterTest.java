package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.cgWaypoint;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.WaypointType;

import android.test.AndroidTestCase;

public class FormatterTest extends AndroidTestCase {

    public static void testParkingWaypoint() {
        assertFormatting(new cgWaypoint("you can park here", WaypointType.PARKING, false), WaypointType.PARKING.getL10n());
    }

    public static void testOriginalWaypoint() {
        assertFormatting(new cgWaypoint("an original", WaypointType.ORIGINAL, false), WaypointType.ORIGINAL.getL10n());
    }

    public static void testOwnWaypoint() {
        cgWaypoint own = new cgWaypoint("my own", WaypointType.OWN, true);
        own.setPrefix(cgWaypoint.PREFIX_OWN);
        assertFormatting(own, cgeoapplication.getInstance().getString(R.string.waypoint_custom));
    }

    private static void assertFormatting(cgWaypoint waypoint, String expected) {
        assertEquals(expected, Formatter.formatWaypointInfo(waypoint));
    }

}
