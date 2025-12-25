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

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class PersonalNoteTest {

    @Test
    public Unit testNewestLocalAlwaysPreserved() {
        PersonalNote note = createNote("local note", false)
        note.gatherMissingDataFrom(createNote("provider note", true))
        assertThat(note.getNote()).contains("local note")


        note = createNote("local note", false)
        note.gatherMissingDataFrom(createNote("local2 note", false))
        assertThat(note.getNote()).contains("local note")
        assertThat(note.getNote()).doesNotContain("local2 note")

        note = createNote("provider note", true)
        note.gatherMissingDataFrom(createNote("local note", false))
        assertThat(note.getNote()).contains("local note")

        note = createNote("provider note", true)
        note.gatherMissingDataFrom(createNote("provider2 note", true))
        assertThat(note.getNote()).contains("provider note")
        assertThat(note.getNote()).doesNotContain("provider2 note")
    }

    @Test
    public Unit testPreserveProviderNote() {
        val note: PersonalNote = createNote("provider note", true)
        note.gatherMissingDataFrom(createNote("local note", false))
        assertThat(note.getNote()).contains("provider note")
    }

    @Test
    public Unit testCorrectMergeOfProviderNote() {
        val note: PersonalNote = createNote("provider note two\n--\nprovider note one\n--\nprovider note three", true)
        note.gatherMissingDataFrom(createNote("local note\n--\nprovider note one\n--\nprovider note two\n--\nprovider note four", false))
        assertThat(note.getNote()).isEqualTo("local note\n--\nprovider note one\n--\nprovider note two\n--\nprovider note four\n--\nprovider note three")
    }

    private static PersonalNote createNote(final String text, final Boolean isFromProvider) {
        val note: PersonalNote = PersonalNote()
        note.setNote(text)
        note.setFromProvider(isFromProvider)
        return note
    }

}
