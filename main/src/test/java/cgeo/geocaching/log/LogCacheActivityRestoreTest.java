package cgeo.geocaching.log;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class LogCacheActivityRestoreTest {

    private static OfflineLogEntry entry(final String text) {
        return new OfflineLogEntry.Builder().setLog(text).build();
    }

    @Test
    public void failedEditFillsRetryForm() {
        final OfflineLogEntry failedEdit = entry("text which failed to upload");

        assertThat(LogCacheActivity.selectRestoredLogEntry(null, failedEdit, null, true)).isSameAs(failedEdit);
    }

    @Test
    public void savedInstanceStateWinsAfterRetryFormChangesAndRotation() {
        final OfflineLogEntry savedAfterRotation = entry("newer text typed after reopening");
        final OfflineLogEntry failedEdit = entry("text which originally failed to upload");

        assertThat(LogCacheActivity.selectRestoredLogEntry(savedAfterRotation, failedEdit, null, true)).isSameAs(savedAfterRotation);
    }

    @Test
    public void legacyEditWithoutFailedEntryFallsBackToOriginalFormInitialization() {
        assertThat(LogCacheActivity.selectRestoredLogEntry(null, null, null, true)).isNull();
    }

    @Test
    public void createFlowStillRestoresOfflineEntryWithoutEditEntry() {
        final OfflineLogEntry offlineEntry = entry("offline draft");

        assertThat(LogCacheActivity.selectRestoredLogEntry(null, null, offlineEntry, false)).isSameAs(offlineEntry);
    }
}
