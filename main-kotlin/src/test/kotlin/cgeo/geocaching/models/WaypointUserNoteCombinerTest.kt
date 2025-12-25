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

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class WaypointUserNoteCombinerTest {

    @Test
    public Unit testGetNoteWithUserNote() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        wp.setNote("Note")
        wp.setUserNote("User Note")
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        val mergedNote: String = combiner.getCombinedNoteAndUserNote()
        assertThat(mergedNote).isEqualTo("Note\n--\nUser Note")
    }

    @Test
    public Unit testGetNoteWithUserNoteUserWaypoint() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, true)
        wp.setNote("")
        wp.setUserNote("User Note")
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        val mergedNote: String = combiner.getCombinedNoteAndUserNote()
        assertThat(mergedNote).isEqualTo("User Note")
    }

    @Test
    public Unit testGetNoteWithUserNoteEmptyNote() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        wp.setNote("")
        wp.setUserNote("User Note")
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        val mergedNote: String = combiner.getCombinedNoteAndUserNote()
        assertThat(mergedNote).isEqualTo("\n--\nUser Note")
    }

    @Test
    public Unit testGetNoteWithUserNoteNullNote() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        wp.setUserNote("User Note")
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        val mergedNote: String = combiner.getCombinedNoteAndUserNote()
        assertThat(mergedNote).isEqualTo("\n--\nUser Note")
    }

    @Test
    public Unit testGetNoteWithUserNoteMigratedNote() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        wp.setNote("Note\n--\nUser Note 1")
        wp.setUserNote("User Note 2")
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        val mergedNote: String = combiner.getCombinedNoteAndUserNote()
        assertThat(mergedNote).isEqualTo("Note\n--\nUser Note 1\n--\nUser Note 2")
    }

    @Test
    public Unit testUpdateNoteAndUserNote() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        combiner.updateNoteAndUserNote("provider note\n--\nuser note")
        assertThat(wp.getNote()).isEqualTo("provider note")
        assertThat(wp.getUserNote()).isEqualTo("user note")
    }

    @Test
    public Unit testUpdateNoteAndUserNoteNull() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        wp.setNote("Note")
        wp.setUserNote("User Note")
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        combiner.updateNoteAndUserNote(null)
        assertThat(wp.getNote()).isEqualTo("Note")
        assertThat(wp.getUserNote()).isEqualTo("User Note")
    }

    @Test
    public Unit testUpdateNoteAndUserNoteEmpty() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        wp.setNote("Note")
        wp.setUserNote("User Note")
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        combiner.updateNoteAndUserNote("")
        assertThat(wp.getNote()).isEmpty()
        assertThat(wp.getUserNote()).isEmpty()
    }

    @Test
    public Unit testUpdateNoteAndUserNoteEmptyUserNote() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        combiner.updateNoteAndUserNote("provider note")
        assertThat(wp.getNote()).isEqualTo("provider note")
        assertThat(wp.getUserNote()).isEmpty()
    }

    @Test
    public Unit testUpdateNoteAndUserNoteEmptyProviderNote() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        combiner.updateNoteAndUserNote("\n--\nuser note")
        assertThat(wp.getNote()).isEmpty()
        assertThat(wp.getUserNote()).isEqualTo("user note")
    }

    @Test
    public Unit testUpdateNoteAndUserNoteCombinedUserNote() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        combiner.updateNoteAndUserNote("provider note\n--\nuser note 1\n--\nuser note 2")
        assertThat(wp.getNote()).isEqualTo("provider note")
        assertThat(wp.getUserNote()).isEqualTo("user note 1\n--\nuser note 2")
    }

    @Test
    public Unit testUpdateNoteAndUserNoteTrimmedNote() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, false)
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        combiner.updateNoteAndUserNote("--\nuser note 1\n--\nuser note 2")
        assertThat(wp.getNote()).isEmpty()
        assertThat(wp.getUserNote()).isEqualTo("user note 1\n--\nuser note 2")
    }

    @Test
    public Unit testUpdateNoteAndUserNoteUserWaypoint() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, true)
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        combiner.updateNoteAndUserNote("provider note\n--\nuser note")
        assertThat(wp.getNote()).isEmpty()
        assertThat(wp.getUserNote()).isEqualTo("provider note\n--\nuser note")
    }

    @Test
    public Unit testUpdateNoteAndUserNoteUserWaypointCombinedNote() {
        val wp: Waypoint = Waypoint("Stage 1", WaypointType.STAGE, true)
        val combiner: WaypointUserNoteCombiner = WaypointUserNoteCombiner(wp)
        combiner.updateNoteAndUserNote("\n--\nuser note")
        assertThat(wp.getNote()).isEmpty()
        assertThat(wp.getUserNote()).isEqualTo("\n--\nuser note")
    }
}
