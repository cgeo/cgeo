package cgeo.geocaching.models;

import cgeo.CGeoTestCase;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class WaypointTest extends CGeoTestCase {

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

    private static void assertOrdered(final Waypoint first, final Waypoint second) {
        assertThat(Waypoint.WAYPOINT_COMPARATOR.compare(first, second)).isLessThan(0);
    }

    public static void testGeocode() {
        final Waypoint waypoint = new Waypoint("Test waypoint", WaypointType.PARKING, false);
        waypoint.setGeocode("p1");
        assertThat(waypoint.getGeocode()).isEqualTo("P1");
    }

    public static void testParseNoWaypoints() {
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
        assertThat(Waypoint.parseWaypoints(note, "Prefix")).isEmpty();
    }

    public static void testParseWaypointsOneLine() {
        final String note = "Dummy note\nn 45° 3.5 e 27° 7.5\nNothing else";
        final Collection<Waypoint> waypoints = Waypoint.parseWaypoints(note, "Prefix");
        assertThat(waypoints).hasSize(1);
        assertWaypoint(waypoints.iterator().next(), "Prefix 1", new Geopoint("N 45°3.5 E 27°7.5"));
    }

    private static void assertWaypoint(final Waypoint waypoint, final String name, final Geopoint geopoint) {
        assertThat(waypoint.getName()).isEqualTo(name);
        assertThat(waypoint.getCoords()).isEqualTo(geopoint);
    }

    public static void testParseWaypointsMultiLine() {
        final String note2 = "Waypoint on two lines\nN 45°3.5\nE 27°7.5\nNothing else";
        final Collection<Waypoint> waypoints = Waypoint.parseWaypoints(note2, "Prefix");
        assertThat(waypoints).hasSize(1);
        assertWaypoint(waypoints.iterator().next(), "Prefix 1", new Geopoint("N 45°3.5 E 27°7.5"));
    }

    /**
     * Taken from GCM4Y8
     */
    public static void testParseWaypointsMultiLineWithDuplicates() {
        final String text = "La cache si ... (N45 49.739 E9 45.038 altitudine 860 m. s.l.m.), si prosegue ...\n" +
                "Proseguendo ancora nel sentiero ... all’agriturismo La Peta (N45 50.305 E9 43.991) vi è possibilità di pranzare e soggiornare.\n" +
                "You go to Costa Serina ... sanctuary “Mother of the snow” (N45 49.739 E9 45.038); then you have a walk towards Tagliata...\n" +
                "The path is part of two paths ... is a rural restaurant called \"la Peta\" (N45 50.305 E9 43.991): here you are able to have lunch ...";

        final Collection<Waypoint> waypoints = Waypoint.parseWaypoints(text, "Prefix");
        assertThat(waypoints).hasSize(4);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        assertWaypoint(iterator.next(), "Prefix 1", new Geopoint("N 45°49.739 E 9°45.038"));
        assertWaypoint(iterator.next(), "Prefix 2", new Geopoint("N 45°50.305 E 9°43.991"));
        assertWaypoint(iterator.next(), "Prefix 3", new Geopoint("N 45°49.739 E 9°45.038"));
        assertWaypoint(iterator.next(), "Prefix 4", new Geopoint("N 45°50.305 E 9°43.991"));
    }

    public static void testMerge() {
        final Waypoint local = new Waypoint("Stage 1", WaypointType.STAGE, false);
        local.setPrefix("S1");
        local.setCoords(new Geopoint("N 45°49.739 E 9°45.038"));
        local.setNote("Note");
        local.setUserNote("User Note");
        local.setVisited(true);
        local.setId(4711);

        final Waypoint server = new Waypoint("", WaypointType.STAGE, false);
        server.setPrefix("S1");
        final ArrayList<Waypoint> newWaypoints = new ArrayList<>();
        newWaypoints.add(server);
        Waypoint.mergeWayPoints(newWaypoints, Collections.singletonList(local), false);

        assertThat(newWaypoints).hasSize(1);
        assertThat(newWaypoints).contains(server);

        assertThat(server.getPrefix()).isEqualTo("S1");
        assertThat(server.getCoords()).isEqualTo(new Geopoint("N 45°49.739 E 9°45.038"));
        assertThat(server.getNote()).isEqualTo("Note");
        assertThat(server.getUserNote()).isEqualTo("User Note");
        assertThat(server.isVisited()).isTrue();
        assertThat(server.getId()).isEqualTo(4711);
    }

    public static void testMergeLocalOwnWPConflictsWithServerWP() {
        final Waypoint local = new Waypoint("Stage 1", WaypointType.STAGE, true);
        local.setPrefix("01");
        local.setCoords(new Geopoint("N 45°49.739 E 9°45.038"));
        local.setNote("Note");
        local.setUserNote("User Note");
        local.setVisited(true);
        local.setId(4711);

        final Waypoint server = new Waypoint("Reference Point 1", WaypointType.TRAILHEAD, false);
        server.setPrefix("01");
        server.setCoords(new Geopoint("N 45°49.001 E 9°45.945"));
        server.setNote("Here turn right");

        final ArrayList<Waypoint> newWaypoints = new ArrayList<>();
        newWaypoints.add(server);
        Waypoint.mergeWayPoints(newWaypoints, Collections.singletonList(local), false);

        assertThat(newWaypoints).hasSize(2);
        assertThat(newWaypoints).contains(local);

        // server wp is untouched
        assertThat(server.getPrefix()).isEqualTo("01");
        assertThat(server.getCoords()).isEqualTo(new Geopoint("N 45°49.001 E 9°45.945"));
        assertThat(server.getNote()).isEqualTo("Here turn right");
        assertThat(server.getUserNote()).isEqualTo("");
        assertThat(server.isVisited()).isFalse();
        assertThat(server.getId()).isEqualTo(-1);
        assertThat(server.isUserDefined()).isFalse();

        // local user-defined wp got new prefix
        assertThat(local.getPrefix()).isNotEqualTo("01");
        assertThat(local.getCoords()).isEqualTo(new Geopoint("N 45°49.739 E 9°45.038"));
        assertThat(local.getNote()).isEqualTo("Note");
        assertThat(local.getUserNote()).isEqualTo("User Note");
        assertThat(local.isVisited()).isTrue();
        assertThat(local.getId()).isEqualTo(4711);
        assertThat(local.isUserDefined()).isTrue();
    }

    public static void testMergeFinalWPWithLocalCoords() {
        final Waypoint local = new Waypoint("Final", WaypointType.FINAL, false);
        local.setCoords(new Geopoint("N 45°49.739 E 9°45.038"));
        final Waypoint server = new Waypoint("Final", WaypointType.FINAL, false);
        server.merge(local);
        assertThat(server.getCoords()).isEqualTo(new Geopoint("N 45°49.739 E 9°45.038"));
    }

    public static void testMergeNote() {
        final Waypoint local = new Waypoint("Stage 1", WaypointType.STAGE, false);
        local.setNote("Old Note");
        local.setUserNote("Local User Note");
        final Waypoint server = new Waypoint("Stage 1", WaypointType.STAGE, false);
        server.setNote("New Note");
        server.merge(local);
        assertThat(server.getNote()).isEqualTo("New Note");
        assertThat(server.getUserNote()).isEqualTo("Local User Note");
    }

    public static void testMergeNoteCleaningUpMigratedNote() {
        final Waypoint local = new Waypoint("Stage 1", WaypointType.STAGE, false);
        local.setNote("");
        local.setUserNote("Note");
        final Waypoint server = new Waypoint("Stage 1", WaypointType.STAGE, false);
        server.setNote("Note");
        server.merge(local);
        assertThat(server.getNote()).isEqualTo("Note");
        assertThat(server.getUserNote()).isEqualTo("");
    }

}
