package cgeo.geocaching;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;

import android.test.AndroidTestCase;

import java.util.Collection;

public class WaypointTest extends AndroidTestCase {

    public static void testOrder() {
        final Waypoint cache = new Waypoint("Final", WaypointType.FINAL, false);
        final Waypoint trailhead = new Waypoint("Trail head", WaypointType.TRAILHEAD, false);
        final Waypoint stage = new Waypoint("stage", WaypointType.STAGE, false);
        final Waypoint puzzle = new Waypoint("puzzle", WaypointType.PUZZLE, false);
        final Waypoint own = new Waypoint("own", WaypointType.OWN, true);
        final Waypoint parking = new Waypoint("parking", WaypointType.PARKING, false);

        assertOrdered(trailhead, puzzle);
        assertOrdered(trailhead, stage);
        assertOrdered(trailhead, cache);

        assertOrdered(stage, cache);
        assertOrdered(puzzle, cache);

        assertOrdered(trailhead, own);
        assertOrdered(puzzle, own);
        assertOrdered(stage, own);
        assertOrdered(cache, own);

        assertOrdered(parking, puzzle);
        assertOrdered(parking, stage);
        assertOrdered(parking, cache);
        assertOrdered(parking, own);
        assertOrdered(parking, trailhead);
    }

    private static void assertOrdered(Waypoint first, Waypoint second) {
        assertTrue(Waypoint.WAYPOINT_COMPARATOR.compare(first, second) < 0);
    }

    public static void testGeocode() {
        final Waypoint waypoint = new Waypoint("Test waypoint", WaypointType.PARKING, false);
        waypoint.setGeocode("p1");
        assertEquals("P1", waypoint.getGeocode());
    }

    public static void testParseNoWaypointFromNote() {
        final String note = "1 T 126\n" +
                "2 B 12\n" +
                "3 S 630\n" +
                "4c P 51\n" +
                "L 1\n" +
                "E 14\n" +
                "J 11\n" +
                "U 12\n" +
                "D 1\n" +
                "M 7\n" +
                "N 5\n" +
                "5 IFG 257";
        assertTrue(Waypoint.parseWaypointsFromNote(note).isEmpty());
    }

    public static void testParseWaypointFromNote() {
        final String note = "Dummy note\nn 45° 3.5 e 27° 7.5\nNothing else";
        final Collection<Waypoint> waypoints = Waypoint.parseWaypointsFromNote(note);
        assertEquals(1, waypoints.size());
        final Geopoint coords = waypoints.iterator().next().getCoords();
        assertEquals(45, coords.getLatDeg());
        assertEquals(3.5, coords.getLatMinRaw());
        assertEquals(27, coords.getLonDeg());
        assertEquals(7.5, coords.getLonMinRaw());
        final String note2 = "Waypoint on two lines\nN 45°3.5\nE 27°7.5\nNothing else";
        final Collection<Waypoint> waypoints2 = Waypoint.parseWaypointsFromNote(note2);
        assertEquals(1, waypoints2.size());
        assertEquals(coords, waypoints2.iterator().next().getCoords());
    }
}
