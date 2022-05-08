package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.WaypointType;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class WaypointUserNoteCombinerTest {

    @Test
    public void testGetNoteWithUserNote() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        wp.setNote("Note");
        wp.setUserNote("User Note");
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        final String mergedNote = combiner.getCombinedNoteAndUserNote();
        assertThat(mergedNote).isEqualTo("Note\n--\nUser Note");
    }

    @Test
    public void testGetNoteWithUserNoteUserWaypoint() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, true);
        wp.setNote("");
        wp.setUserNote("User Note");
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        final String mergedNote = combiner.getCombinedNoteAndUserNote();
        assertThat(mergedNote).isEqualTo("User Note");
    }

    @Test
    public void testGetNoteWithUserNoteEmptyNote() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        wp.setNote("");
        wp.setUserNote("User Note");
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        final String mergedNote = combiner.getCombinedNoteAndUserNote();
        assertThat(mergedNote).isEqualTo("\n--\nUser Note");
    }

    @Test
    public void testGetNoteWithUserNoteNullNote() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        wp.setUserNote("User Note");
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        final String mergedNote = combiner.getCombinedNoteAndUserNote();
        assertThat(mergedNote).isEqualTo("\n--\nUser Note");
    }

    @Test
    public void testGetNoteWithUserNoteMigratedNote() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        wp.setNote("Note\n--\nUser Note 1");
        wp.setUserNote("User Note 2");
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        final String mergedNote = combiner.getCombinedNoteAndUserNote();
        assertThat(mergedNote).isEqualTo("Note\n--\nUser Note 1\n--\nUser Note 2");
    }

    @Test
    public void testUpdateNoteAndUserNote() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        combiner.updateNoteAndUserNote("provider note\n--\nuser note");
        assertThat(wp.getNote()).isEqualTo("provider note");
        assertThat(wp.getUserNote()).isEqualTo("user note");
    }

    @Test
    public void testUpdateNoteAndUserNoteNull() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        wp.setNote("Note");
        wp.setUserNote("User Note");
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        combiner.updateNoteAndUserNote(null);
        assertThat(wp.getNote()).isEqualTo("Note");
        assertThat(wp.getUserNote()).isEqualTo("User Note");
    }

    @Test
    public void testUpdateNoteAndUserNoteEmpty() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        wp.setNote("Note");
        wp.setUserNote("User Note");
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        combiner.updateNoteAndUserNote("");
        assertThat(wp.getNote()).isEmpty();
        assertThat(wp.getUserNote()).isEmpty();
    }

    @Test
    public void testUpdateNoteAndUserNoteEmptyUserNote() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        combiner.updateNoteAndUserNote("provider note");
        assertThat(wp.getNote()).isEqualTo("provider note");
        assertThat(wp.getUserNote()).isEmpty();
    }

    @Test
    public void testUpdateNoteAndUserNoteEmptyProviderNote() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        combiner.updateNoteAndUserNote("\n--\nuser note");
        assertThat(wp.getNote()).isEmpty();
        assertThat(wp.getUserNote()).isEqualTo("user note");
    }

    @Test
    public void testUpdateNoteAndUserNoteCombinedUserNote() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        combiner.updateNoteAndUserNote("provider note\n--\nuser note 1\n--\nuser note 2");
        assertThat(wp.getNote()).isEqualTo("provider note");
        assertThat(wp.getUserNote()).isEqualTo("user note 1\n--\nuser note 2");
    }

    @Test
    public void testUpdateNoteAndUserNoteTrimmedNote() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, false);
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        combiner.updateNoteAndUserNote("--\nuser note 1\n--\nuser note 2");
        assertThat(wp.getNote()).isEmpty();
        assertThat(wp.getUserNote()).isEqualTo("user note 1\n--\nuser note 2");
    }

    @Test
    public void testUpdateNoteAndUserNoteUserWaypoint() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, true);
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        combiner.updateNoteAndUserNote("provider note\n--\nuser note");
        assertThat(wp.getNote()).isEmpty();
        assertThat(wp.getUserNote()).isEqualTo("provider note\n--\nuser note");
    }

    @Test
    public void testUpdateNoteAndUserNoteUserWaypointCombinedNote() {
        final Waypoint wp = new Waypoint("Stage 1", WaypointType.STAGE, true);
        final WaypointUserNoteCombiner combiner = new WaypointUserNoteCombiner(wp);
        combiner.updateNoteAndUserNote("\n--\nuser note");
        assertThat(wp.getNote()).isEmpty();
        assertThat(wp.getUserNote()).isEqualTo("\n--\nuser note");
    }
}
