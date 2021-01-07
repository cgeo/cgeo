package cgeo.geocaching.models;

import cgeo.CGeoTestCase;
import cgeo.geocaching.enumerations.WaypointType;

class WaypointTest extends CGeoTestCase {

    public static void testGetDefaultWaypointName() {
        final Geocache cache = new Geocache();
        // all test cases check numbering across different waypoint types

        // simplest case: number is at the end, add 1
        cache.addOrChangeWaypoint(new Waypoint("", null, "Test 1", "", "", WaypointType.OWN), false);
        final String new1 = Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT);
        assertEquals(WaypointType.WAYPOINT.getNameForNewWaypoint() + " 2", new1);

        // a bit more tricky: number somewhere in the middle
        cache.addOrChangeWaypoint(new Waypoint("", null, "Test 12 - Error?", "", "", WaypointType.OWN), false);
        final String new2 = Waypoint.getDefaultWaypointName(cache, WaypointType.OWN);
        assertEquals(WaypointType.OWN.getNameForNewWaypoint() + " 13", new2);

        // even more tricky: two numbers in the name - should find the higher one
        cache.addOrChangeWaypoint(new Waypoint("", null, "Test 20 - Error 18?", "", "", WaypointType.TRAILHEAD), false);
        final String new3 = Waypoint.getDefaultWaypointName(cache, WaypointType.PUZZLE);
        assertEquals(WaypointType.PUZZLE.getNameForNewWaypoint() + " 21", new3);

    }


}
