package cgeo.geocaching.export;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class FieldNotesTest {

    // 2024-05-01T10:20:30Z
    private static final long DATE = 1714558830000L;

    private static Geocache cache(final String geocode) {
        final Geocache cache = new Geocache();
        cache.setGeocode(geocode);
        return cache;
    }

    private static LogEntry log(final LogType type, final String text) {
        return new LogEntry.Builder().setLogType(type).setDate(DATE).setLog(text).build();
    }

    @Test
    public void testGcRecordsFirstAndNoRecordLeftOut() {
        final FieldNotes fieldNotes = new FieldNotes();
        fieldNotes.add(cache("OC1234"), log(LogType.FOUND_IT, "first OC"));
        fieldNotes.add(cache("GC12345"), log(LogType.FOUND_IT, "first GC"));
        fieldNotes.add(cache("OP5678"), log(LogType.DIDNT_FIND_IT, "OP"));
        fieldNotes.add(cache("GCABCD"), log(LogType.NOTE, "second GC"));
        fieldNotes.add(cache("OC9999"), log(LogType.FOUND_IT, "second OC"));

        assertThat(fieldNotes.size()).isEqualTo(5);
        assertThat(fieldNotes.getContent()).isEqualTo(
                "GC12345,2024-05-01T10:20:30Z,Found it,\"first GC\"\n"
                        + "GCABCD,2024-05-01T10:20:30Z,Write note,\"second GC\"\n"
                        + "OC1234,2024-05-01T10:20:30Z,Found it,\"first OC\"\n"
                        + "OP5678,2024-05-01T10:20:30Z,Didn't find it,\"OP\"\n"
                        + "OC9999,2024-05-01T10:20:30Z,Found it,\"second OC\"\n");
    }

    @Test
    public void testWithoutGcRecordsOrderIsUnchanged() {
        final FieldNotes fieldNotes = new FieldNotes();
        fieldNotes.add(cache("OC1234"), log(LogType.FOUND_IT, "a"));
        fieldNotes.add(cache("OP5678"), log(LogType.FOUND_IT, "b"));

        assertThat(fieldNotes.getContent()).isEqualTo(
                "OC1234,2024-05-01T10:20:30Z,Found it,\"a\"\n"
                        + "OP5678,2024-05-01T10:20:30Z,Found it,\"b\"\n");
    }

    @Test
    public void testMultiLineLogTextStaysWithItsRecord() {
        final FieldNotes fieldNotes = new FieldNotes();
        fieldNotes.add(cache("OC1234"), log(LogType.FOUND_IT, "line 1\nline 2"));
        fieldNotes.add(cache("GC12345"), log(LogType.FOUND_IT, "TFTC"));

        assertThat(fieldNotes.getContent()).isEqualTo(
                "GC12345,2024-05-01T10:20:30Z,Found it,\"TFTC\"\n"
                        + "OC1234,2024-05-01T10:20:30Z,Found it,\"line 1\nline 2\"\n");
    }
}
