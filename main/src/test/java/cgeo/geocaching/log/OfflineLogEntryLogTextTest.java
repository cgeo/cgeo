package cgeo.geocaching.log;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class OfflineLogEntryLogTextTest {

    @Test
    public void offlineLogPreservesLeadingNewlines() {
        // #6631: a one-click offline log stores the signature verbatim, keeping its leading newlines
        final OfflineLogEntry entry = new OfflineLogEntry.Builder().setLog("\n\n\nmy signature").build();
        assertThat(entry.log).isEqualTo("\n\n\nmy signature");
    }

    @Test
    public void offlineLogPreservesTrailingWhitespace() {
        final OfflineLogEntry entry = new OfflineLogEntry.Builder().setLog("text\n\n").build();
        assertThat(entry.log).isEqualTo("text\n\n");
    }

    @Test
    public void regularLogStillGetsTrimmed() {
        // online/parsed logs keep the existing trimming behaviour
        final LogEntry entry = new LogEntry.Builder().setLog("\n\n\nhello").build();
        assertThat(entry.log).isEqualTo("hello");
    }
}
