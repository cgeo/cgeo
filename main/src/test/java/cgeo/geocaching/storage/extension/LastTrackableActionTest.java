package cgeo.geocaching.storage.extension;

import cgeo.geocaching.log.LogTypeTrackable;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class LastTrackableActionTest {

    @Test
    public void droppedUsesConfiguredDefault() {
        assertThat(LastTrackableAction.resolveNextAction(LogTypeTrackable.DROPPED_OFF, false))
                .isEqualTo(LogTypeTrackable.DO_NOTHING);
        assertThat(LastTrackableAction.resolveNextAction(LogTypeTrackable.DROPPED_OFF, true))
                .isEqualTo(LogTypeTrackable.VISITED);
    }

    @Test
    public void missingOrInvalidActionUsesConfiguredDefault() {
        assertThat(LastTrackableAction.resolveNextAction(null, false))
                .isEqualTo(LogTypeTrackable.DO_NOTHING);
        assertThat(LastTrackableAction.resolveNextAction(null, true))
                .isEqualTo(LogTypeTrackable.VISITED);
        assertThat(LastTrackableAction.resolveNextAction(LogTypeTrackable.UNKNOWN, false))
                .isEqualTo(LogTypeTrackable.DO_NOTHING);
        assertThat(LastTrackableAction.resolveNextAction(LogTypeTrackable.UNKNOWN, true))
                .isEqualTo(LogTypeTrackable.VISITED);
    }

    @Test
    public void reusableActionRemainsUnchanged() {
        assertThat(LastTrackableAction.resolveNextAction(LogTypeTrackable.DO_NOTHING, true))
                .isEqualTo(LogTypeTrackable.DO_NOTHING);
        assertThat(LastTrackableAction.resolveNextAction(LogTypeTrackable.VISITED, false))
                .isEqualTo(LogTypeTrackable.VISITED);
    }
}
