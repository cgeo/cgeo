package cgeo.geocaching.models;

import junit.framework.TestCase;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class PersonalNoteTest extends TestCase {

    public static void testParse() {
        final String testString = "Simple cgeo note\n--\nSimple provider note";
        final Geocache cache = new Geocache();
        cache.setPersonalNote(testString);
        final PersonalNote parsedNote = new PersonalNote(cache);
        assertThat(parsedNote.toString()).isEqualTo(testString);
        assertPersonalNote(parsedNote, "Simple cgeo note", "Simple provider note");

    }

    public static void testParseProviderOnly() {
        final String testString = "Simple provider note";
        final Geocache cache = new Geocache();
        cache.setPersonalNote(testString);
        final PersonalNote parsedNote = new PersonalNote(cache);
        assertThat(parsedNote.toString()).isEqualTo(testString);
        assertPersonalNote(parsedNote, null, "Simple provider note");
    }

    public static void testParseCgeoOnly() {
        final String testString = "Simple cgeo note";
        final Geocache cache = new Geocache();
        cache.setPersonalNote(testString);
        final PersonalNote parsedNote = new PersonalNote(cache);
        assertThat(parsedNote.toString()).isEqualTo("Simple cgeo note");
        assertPersonalNote(parsedNote, null, "Simple cgeo note");
    }

    private static void assertPersonalNote(final PersonalNote note, final String cgeoNote, final String providerNote) {
        assertThat(note.getCgeoNote()).isEqualTo(cgeoNote);
        assertThat(note.getProviderNote()).isEqualTo(providerNote);
    }
}
