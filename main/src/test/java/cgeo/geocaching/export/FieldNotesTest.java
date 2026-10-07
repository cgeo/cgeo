package cgeo.geocaching.export;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class FieldNotesTest {

    // 2024-05-01T10:20:30Z
    private static final long DATE = 1714558830000L;

    private static FieldNotes hybridFieldNotes() {
        final FieldNotes fieldNotes = new FieldNotes();
        fieldNotes.add(cache("GC12345"), log(LogType.FOUND_IT, "TFTC"));
        fieldNotes.add(cache("OC1234"), log(LogType.NOTE, "Say \"hello\""));
        fieldNotes.add(cache("OP5678"), log(LogType.FOUND_IT, "Dzięki"));
        fieldNotes.add(cache("OC9999"), log(LogType.NEEDS_ARCHIVE, "no OC name for this type"));
        return fieldNotes;
    }

    private static Geocache cache(final String geocode) {
        final Geocache cache = new Geocache();
        cache.setGeocode(geocode);
        return cache;
    }

    private static LogEntry log(final LogType type, final String text) {
        return new LogEntry.Builder().setLogType(type).setDate(DATE).setLog(text).build();
    }

    @Test
    public void testExportFileContainsAllPlatformsWithGcTypeNames() {
        final FieldNotes fieldNotes = hybridFieldNotes();
        assertThat(fieldNotes.size()).isEqualTo(4);
        assertThat(fieldNotes.getContent()).isEqualTo(
                "GC12345,2024-05-01T10:20:30Z,Found it,\"TFTC\"\n"
                        + "OC1234,2024-05-01T10:20:30Z,Write note,\"Say 'hello'\"\n"
                        + "OP5678,2024-05-01T10:20:30Z,Found it,\"Dzięki\"\n"
                        + "OC9999,2024-05-01T10:20:30Z,Needs Archived,\"no OC name for this type\"\n");
    }

    @Test
    public void testPlatformContentIsFilteredAndUsesPlatformTypeNames() {
        final String content = hybridFieldNotes().getContent(geocode -> geocode.startsWith("OC"), logType -> logType.ocType);
        // only OC caches, "Comment" instead of "Write note", types without an OC name are left out
        assertThat(content).isEqualTo("OC1234,2024-05-01T10:20:30Z,Comment,\"Say 'hello'\"\n");
    }

    @Test
    public void testPlatformContentCanBeEmpty() {
        assertThat(hybridFieldNotes().getContent(geocode -> geocode.startsWith("OU"), logType -> logType.ocType)).isEmpty();
    }
}
