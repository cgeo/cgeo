package cgeo.geocaching;

import cgeo.geocaching.enumerations.WaypointType;

import android.test.AndroidTestCase;

public class WaypointTest extends AndroidTestCase {

    public static void testOrder() {
        final Waypoint cache = new Waypoint("Final", WaypointType.FINAL, false);
        final Waypoint trailhead = new Waypoint("Trail head", WaypointType.TRAILHEAD, false);
        final Waypoint stage = new Waypoint("stage", WaypointType.STAGE, false);
        final Waypoint puzzle = new Waypoint("puzzle", WaypointType.PUZZLE, false);
        final Waypoint own = new Waypoint("own", WaypointType.OWN, true);
        final Waypoint parking = new Waypoint("parking", WaypointType.PARKING, false);

        assertTrue(trailhead.compareTo(puzzle) < 0);
        assertTrue(trailhead.compareTo(stage) < 0);
        assertTrue(trailhead.compareTo(cache) < 0);

        assertTrue(stage.compareTo(cache) < 0);
        assertTrue(puzzle.compareTo(cache) < 0);

        assertTrue(trailhead.compareTo(own) < 0);
        assertTrue(puzzle.compareTo(own) < 0);
        assertTrue(stage.compareTo(own) < 0);
        assertTrue(cache.compareTo(own) < 0);

        assertTrue(parking.compareTo(puzzle) < 0);
        assertTrue(parking.compareTo(stage) < 0);
        assertTrue(parking.compareTo(cache) < 0);
        assertTrue(parking.compareTo(own) < 0);
        assertTrue(parking.compareTo(trailhead) < 0);
	}

    public static void testGeocode() {
        Waypoint waypoint = new Waypoint("Test waypoint", WaypointType.PARKING, false);
        waypoint.setGeocode("p1");
        assertEquals("P1", waypoint.getGeocode());
    }
}
