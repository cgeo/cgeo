package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class WaypointTest {

    @Test
    public void testOrder() {
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

    @Test
    public void testGeocode() {
        final Waypoint waypoint = new Waypoint("Test waypoint", WaypointType.PARKING, false);
        waypoint.setGeocode("p1");
        assertThat(waypoint.getGeocode()).isEqualTo("P1");
    }


    @Test
    public void testMerge() {
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

    @Test
    public void testMergeLocalOwnWPConflictsWithServerWP() {
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

    @Test
    public void testMergeFinalWPWithLocalCoords() {
        final Waypoint local = new Waypoint("Final", WaypointType.FINAL, false);
        local.setCoords(new Geopoint("N 45°49.739 E 9°45.038"));
        final Waypoint server = new Waypoint("Final", WaypointType.FINAL, false);
        server.merge(local);
        assertThat(server.getCoords()).isEqualTo(new Geopoint("N 45°49.739 E 9°45.038"));
    }

    @Test
    public void testMergeNote() {
        final Waypoint local = new Waypoint("Stage 1", WaypointType.STAGE, false);
        local.setNote("Old Note");
        local.setUserNote("Local User Note");
        final Waypoint server = new Waypoint("Stage 1", WaypointType.STAGE, false);
        server.setNote("New Note");
        server.merge(local);
        assertThat(server.getNote()).isEqualTo("New Note");
        assertThat(server.getUserNote()).isEqualTo("Local User Note");
    }

    @Test
    public void testMergeNoteCleaningUpMigratedNote() {
        final Waypoint local = new Waypoint("Stage 1", WaypointType.STAGE, false);
        local.setNote("");
        local.setUserNote("Note");
        final Waypoint server = new Waypoint("Stage 1", WaypointType.STAGE, false);
        server.setNote("Note");
        server.merge(local);
        assertThat(server.getNote()).isEqualTo("Note");
        assertThat(server.getUserNote()).isEqualTo("");
    }

    @Test
    public void testMergeNoteEmptyServerNote() {
        final Waypoint local = new Waypoint("Stage 1", WaypointType.STAGE, false);
        local.setNote("Old Note");
        local.setUserNote("Local User Note");
        final Waypoint server = new Waypoint("Stage 1", WaypointType.STAGE, false);
        server.merge(local);
        assertThat(server.getNote()).isEqualTo("");
        assertThat(server.getUserNote()).isEqualTo("Local User Note");
    }

    @Test
    public void testGetDefaultWaypointName() {
        final Geocache cache = new Geocache();
        // all test cases check numbering across different waypoint types

        // simplest case: number is at the end, add 1
        cache.addOrChangeWaypoint(new Waypoint("", null, "Test 1", "", "", WaypointType.OWN), false);
        final String new1 = Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT);
        assertThat(WaypointType.WAYPOINT.getNameForNewWaypoint() + " 2").isEqualTo(new1);

        // a bit more tricky: number somewhere in the middle
        cache.addOrChangeWaypoint(new Waypoint("", null, "Test 12 - Error?", "", "", WaypointType.OWN), false);
        final String new2 = Waypoint.getDefaultWaypointName(cache, WaypointType.OWN);
        assertThat(WaypointType.OWN.getNameForNewWaypoint() + " 13").isEqualTo(new2);

        // even more tricky: two numbers in the name - should find the higher one
        cache.addOrChangeWaypoint(new Waypoint("", null, "Test 20 - Error 18?", "", "", WaypointType.TRAILHEAD), false);
        final String new3 = Waypoint.getDefaultWaypointName(cache, WaypointType.PUZZLE);
        assertThat(WaypointType.PUZZLE.getNameForNewWaypoint() + " 21").isEqualTo(new3);

        // should not find numbers from GC-codes
        cache.addOrChangeWaypoint(new Waypoint("", null, "GC123AB", "", "", WaypointType.WAYPOINT), false);
        final String new4 = Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT);
        assertThat(WaypointType.WAYPOINT.getNameForNewWaypoint() + " 21").isEqualTo(new4);

        // should not find parts of floats
        cache.addOrChangeWaypoint(new Waypoint("", null, "Pi 3.1415", "", "", WaypointType.WAYPOINT), false);
        final String new5 = Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT);
        assertThat(WaypointType.WAYPOINT.getNameForNewWaypoint() + " 21").isEqualTo(new5);

        // parking waypoints should be named "Parking", "Parking 2" etc.
        final String new6 = Waypoint.getDefaultWaypointName(cache, WaypointType.PARKING);
        assertThat(WaypointType.PARKING.getNameForNewWaypoint()).isEqualTo(new6);
        cache.addOrChangeWaypoint(new Waypoint("", null, new6, "", "", WaypointType.PARKING), false);
        final String new7 = Waypoint.getDefaultWaypointName(cache, WaypointType.PARKING);
        assertThat(WaypointType.PARKING.getNameForNewWaypoint() + " 2").isEqualTo(new7);

        // a final waypoint should not be taken into account for other waypoint types
        cache.addOrChangeWaypoint(new Waypoint("", null, WaypointType.FINAL.getNameForNewWaypoint() + " 27", "", "", WaypointType.FINAL), false);
        final String new8 = Waypoint.getDefaultWaypointName(cache, WaypointType.PUZZLE);
        assertThat(WaypointType.PUZZLE.getNameForNewWaypoint() + " 21").isEqualTo(new8);
    }
}
