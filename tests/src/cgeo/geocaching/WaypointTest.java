package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;

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
        assertThat(Waypoint.WAYPOINT_COMPARATOR.compare(first, second) < 0).isTrue();
    }

    public static void testGeocode() {
        final Waypoint waypoint = new Waypoint("Test waypoint", WaypointType.PARKING, false);
        waypoint.setGeocode("p1");
        assertThat(waypoint.getGeocode()).isEqualTo("P1");
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
        assertThat(Waypoint.parseWaypointsFromNote(note).isEmpty()).isTrue();
    }

    public static void testParseWaypointFromNote() {
        final String note = "Dummy note\nn 45° 3.5 e 27° 7.5\nNothing else";
        final Collection<Waypoint> waypoints = Waypoint.parseWaypointsFromNote(note);
        assertThat(waypoints).hasSize(1);
        final Geopoint coords = waypoints.iterator().next().getCoords();
        assertThat(coords.getLatDeg()).isEqualTo(45);
        assertThat(coords.getLatMinRaw()).isEqualTo(3.5);
        assertThat(coords.getLonDeg()).isEqualTo(27);
        assertThat(coords.getLonMinRaw()).isEqualTo(7.5);
        final String note2 = "Waypoint on two lines\nN 45°3.5\nE 27°7.5\nNothing else";
        final Collection<Waypoint> waypoints2 = Waypoint.parseWaypointsFromNote(note2);
        assertThat(waypoints2).hasSize(1);
        assertThat(waypoints2.iterator().next().getCoords()).isEqualTo(coords);
    }
}
