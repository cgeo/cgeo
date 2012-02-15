package cgeo.geocaching;

import cgeo.geocaching.enumerations.WaypointType;

import android.test.AndroidTestCase;

public class cgWaypointTest extends AndroidTestCase {

    public static void testOrder() {
        final cgWaypoint cache = new cgWaypoint("Final", WaypointType.FINAL, false);
        final cgWaypoint trailhead = new cgWaypoint("Trail head", WaypointType.TRAILHEAD, false);
        final cgWaypoint stage = new cgWaypoint("stage", WaypointType.STAGE, false);
        final cgWaypoint puzzle = new cgWaypoint("puzzle", WaypointType.PUZZLE, false);
        final cgWaypoint own = new cgWaypoint("own", WaypointType.OWN, true);
        final cgWaypoint parking = new cgWaypoint("parking", WaypointType.PARKING, false);

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

}
