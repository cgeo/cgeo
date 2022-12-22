package cgeo.geocaching.models;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class PersonalNoteTest {

    @Test
    public void testNewestLocalAlwaysPreserved() {
        PersonalNote note = createNote("local note", false);
        note.gatherMissingDataFrom(createNote("provider note", true));
        assertThat(note.getNote()).contains("local note");


        note = createNote("local note", false);
        note.gatherMissingDataFrom(createNote("local2 note", false));
        assertThat(note.getNote()).contains("local note");
        assertThat(note.getNote()).doesNotContain("local2 note");

        note = createNote("provider note", true);
        note.gatherMissingDataFrom(createNote("local note", false));
        assertThat(note.getNote()).contains("local note");

        note = createNote("provider note", true);
        note.gatherMissingDataFrom(createNote("provider2 note", true));
        assertThat(note.getNote()).contains("provider note");
        assertThat(note.getNote()).doesNotContain("provider2 note");
    }

    @Test
    public void testPreserveProviderNote() {
        final PersonalNote note = createNote("provider note", true);
        note.gatherMissingDataFrom(createNote("local note", false));
        assertThat(note.getNote()).contains("provider note");
    }

    @Test
    public void testCorrectMergeOfProviderNote() {
        final PersonalNote note = createNote("provider note two\n--\nprovider note one\n--\nprovider note three", true);
        note.gatherMissingDataFrom(createNote("local note\n--\nprovider note one\n--\nprovider note two\n--\nprovider note four", false));
        assertThat(note.getNote()).isEqualTo("local note\n--\nprovider note one\n--\nprovider note two\n--\nprovider note four\n--\nprovider note three");
    }

    private static PersonalNote createNote(final String text, final boolean isFromProvider) {
        final PersonalNote note = new PersonalNote();
        note.setNote(text);
        note.setFromProvider(isFromProvider);
        return note;
    }

}
