// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.models

import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint

import java.util.ArrayList
import java.util.Collections

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class WaypointTest {

    @Test
    public Unit testOrder() {
        val cache: Waypoint = Waypoint("Final", WaypointType.FINAL, false)
        val trailhead: Waypoint = Waypoint("Trail head", WaypointType.TRAILHEAD, false)
        val stage: Waypoint = Waypoint("stage", WaypointType.STAGE, false)
        val puzzle: Waypoint = Waypoint("puzzle", WaypointType.PUZZLE, false)
        val own: Waypoint = Waypoint("own", WaypointType.OWN, true)
        val parking: Waypoint = Waypoint("parking", WaypointType.PARKING, false)

        assertOrdered(trailhead, puzzle)
        assertOrdered(trailhead, stage)
        assertOrdered(trailhead, cache)

        assertOrdered(stage, cache)
        assertOrdered(puzzle, cache)

        assertOrdered(trailhead, own)
        assertOrdered(puzzle, own)
        assertOrdered(stage, own)
        assertOrdered(cache, own)

        assertOrdered(parking, puzzle)
        assertOrdered(parking, stage)
        assertOrdered(parking, cache)
        assertOrdered(parking, own)
        assertOrdered(parking, trailhead)
    }

    private static Unit assertOrdered(final Waypoint first, final Waypoint second) {
        assertThat(Waypoint.WAYPOINT_COMPARATOR.compare(first, second)).isLessThan(0)
    }

    @Test
    public Unit testGeocode() {
        val waypoint: Waypoint = Waypoint("Test waypoint", WaypointType.PARKING, false)
        waypoint.setGeocode("p1")
        assertThat(waypoint.getGeocode()).isEqualTo("P1")
    }


    @Test
    public Unit testMerge() {
        val local: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        local.setPrefix("S1")
        local.setCoords(Geopoint("N 45°49.739 E 9°45.038"))
        local.setNote("Note")
        local.setUserNote("User Note")
        local.setVisited(true)
        local.setId(4711)

        val server: Waypoint = Waypoint("", WaypointType.STAGE, false)
        server.setPrefix("S1")
        val newWaypoints: ArrayList<Waypoint> = ArrayList<>()
        newWaypoints.add(server)
        Waypoint.mergeWayPoints(newWaypoints, Collections.singletonList(local), false)

        assertThat(newWaypoints).hasSize(1)
        assertThat(newWaypoints).contains(server)

        assertThat(server.getPrefix()).isEqualTo("S1")
        assertThat(server.getCoords()).isEqualTo(Geopoint("N 45°49.739 E 9°45.038"))
        assertThat(server.getNote()).isEqualTo("")
        assertThat(server.getUserNote()).isEqualTo("User Note")
        assertThat(server.isVisited()).isTrue()
        assertThat(server.getId()).isEqualTo(4711)
    }

    @Test
    public Unit testMergeLocalOwnWPConflictsWithServerWP() {
        val local: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, true)
        local.setPrefix("01")
        local.setCoords(Geopoint("N 45°49.739 E 9°45.038"))
        local.setNote("Note")
        local.setUserNote("User Note")
        local.setVisited(true)
        local.setId(4711)

        val server: Waypoint = Waypoint("Reference Point 1", WaypointType.TRAILHEAD, false)
        server.setPrefix("01")
        server.setCoords(Geopoint("N 45°49.001 E 9°45.945"))
        server.setNote("Here turn right")

        val newWaypoints: ArrayList<Waypoint> = ArrayList<>()
        newWaypoints.add(server)
        Waypoint.mergeWayPoints(newWaypoints, Collections.singletonList(local), false)

        assertThat(newWaypoints).hasSize(2)
        assertThat(newWaypoints).contains(local)

        // server wp is untouched
        assertThat(server.getPrefix()).isEqualTo("01")
        assertThat(server.getCoords()).isEqualTo(Geopoint("N 45°49.001 E 9°45.945"))
        assertThat(server.getNote()).isEqualTo("Here turn right")
        assertThat(server.getUserNote()).isEqualTo("")
        assertThat(server.isVisited()).isFalse()
        assertThat(server.getId()).isEqualTo(-1)
        assertThat(server.isUserDefined()).isFalse()

        // local user-defined wp got prefix
        assertThat(local.getPrefix()).isNotEqualTo("01")
        assertThat(local.getCoords()).isEqualTo(Geopoint("N 45°49.739 E 9°45.038"))
        assertThat(local.getNote()).isEqualTo("Note")
        assertThat(local.getUserNote()).isEqualTo("User Note")
        assertThat(local.isVisited()).isTrue()
        assertThat(local.getId()).isEqualTo(4711)
        assertThat(local.isUserDefined()).isTrue()
    }

    @Test
    public Unit testMergeFinalWPWithLocalCoords() {
        val local: Waypoint = Waypoint("Final", WaypointType.FINAL, false)
        local.setCoords(Geopoint("N 45°49.739 E 9°45.038"))
        val server: Waypoint = Waypoint("Final", WaypointType.FINAL, false)
        server.merge(local)
        assertThat(server.getCoords()).isEqualTo(Geopoint("N 45°49.739 E 9°45.038"))
    }

    @Test
    public Unit testMergeStageWPWithLocalCoords() {
        val local: Waypoint = Waypoint("STAGE", WaypointType.STAGE, false)
        local.setCoords(Geopoint("N 45°49.739 E 9°45.038"))
        val server: Waypoint = Waypoint("STAGE", WaypointType.STAGE, false)
        server.merge(local)
        assertThat(server.getCoords()).isEqualTo(Geopoint("N 45°49.739 E 9°45.038"))
    }

    @Test
    public Unit testMergeStageWPWithServerCoords() {
        val local: Waypoint = Waypoint("STAGE", WaypointType.STAGE, false)
        val server: Waypoint = Waypoint("STAGE", WaypointType.STAGE, false)
        server.setCoords(Geopoint("N 45°49.739 E 9°45.038"))
        server.merge(local)
        assertThat(server.getCoords()).isEqualTo(Geopoint("N 45°49.739 E 9°45.038"))
    }

    @Test
    public Unit testMergeNote() {
        val local: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        local.setNote("Old Note")
        local.setUserNote("Local User Note")
        val server: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        server.setNote("New Note")
        server.merge(local)
        assertThat(server.getNote()).isEqualTo("New Note")
        assertThat(server.getUserNote()).isEqualTo("Local User Note")
    }

    @Test
    public Unit testMergeNoteCleaningUpMigratedNote() {
        val local: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        local.setNote("")
        local.setUserNote("Note")
        val server: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        server.setNote("Note")
        server.merge(local)
        assertThat(server.getNote()).isEqualTo("Note")
        assertThat(server.getUserNote()).isEqualTo("")
    }

    @Test
    public Unit testMergeNoteEmptyNoteServerWP() {
        val local: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        local.setNote("Old Note")
        local.setUserNote("Local User Note")
        val server: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        server.merge(local)
        assertThat(server.getNote()).isEqualTo("")
        assertThat(server.getUserNote()).isEqualTo("Local User Note")
    }

    @Test
    public Unit testMergeNoteEmptyNoteUserWP() {
        val local: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, true)
        local.setNote("Old Note")
        local.setUserNote("Local User Note")
        val server: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, true)
        server.merge(local)
        assertThat(server.getNote()).isEqualTo("Old Note")
        assertThat(server.getUserNote()).isEqualTo("Local User Note")
    }

    @Test
    public Unit testGetDefaultWaypointName() {
        val cache: Geocache = Geocache()
        // all test cases check numbering across different waypoint types

        // simplest case: number is at the end, add 1
        cache.addOrChangeWaypoint(Waypoint("", null, "Test 1", "", "", WaypointType.OWN), false)
        val new1: String = Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT)
        assertThat(WaypointType.WAYPOINT.getNameForNewWaypoint() + " 2").isEqualTo(new1)

        // a bit more tricky: number somewhere in the middle
        cache.addOrChangeWaypoint(Waypoint("", null, "Test 12 - Error?", "", "", WaypointType.OWN), false)
        val new2: String = Waypoint.getDefaultWaypointName(cache, WaypointType.OWN)
        assertThat(WaypointType.OWN.getNameForNewWaypoint() + " 13").isEqualTo(new2)

        // even more tricky: two numbers in the name - should find the higher one
        cache.addOrChangeWaypoint(Waypoint("", null, "Test 20 - Error 18?", "", "", WaypointType.TRAILHEAD), false)
        val new3: String = Waypoint.getDefaultWaypointName(cache, WaypointType.PUZZLE)
        assertThat(WaypointType.PUZZLE.getNameForNewWaypoint() + " 21").isEqualTo(new3)

        // should not find numbers from GC-codes
        cache.addOrChangeWaypoint(Waypoint("", null, "GC123AB", "", "", WaypointType.WAYPOINT), false)
        val new4: String = Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT)
        assertThat(WaypointType.WAYPOINT.getNameForNewWaypoint() + " 21").isEqualTo(new4)

        // should not find parts of floats
        cache.addOrChangeWaypoint(Waypoint("", null, "Pi 3.1415", "", "", WaypointType.WAYPOINT), false)
        val new5: String = Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT)
        assertThat(WaypointType.WAYPOINT.getNameForNewWaypoint() + " 21").isEqualTo(new5)

        // parking waypoints should be named "Parking", "Parking 2" etc.
        val new6: String = Waypoint.getDefaultWaypointName(cache, WaypointType.PARKING)
        assertThat(WaypointType.PARKING.getNameForNewWaypoint()).isEqualTo(new6)
        cache.addOrChangeWaypoint(Waypoint("", null, new6, "", "", WaypointType.PARKING), false)
        val new7: String = Waypoint.getDefaultWaypointName(cache, WaypointType.PARKING)
        assertThat(WaypointType.PARKING.getNameForNewWaypoint() + " 2").isEqualTo(new7)

        // a final waypoint should not be taken into account for other waypoint types
        cache.addOrChangeWaypoint(Waypoint("", null, WaypointType.FINAL.getNameForNewWaypoint() + " 27", "", "", WaypointType.FINAL), false)
        val new8: String = Waypoint.getDefaultWaypointName(cache, WaypointType.PUZZLE)
        assertThat(WaypointType.PUZZLE.getNameForNewWaypoint() + " 21").isEqualTo(new8)
    }
}
