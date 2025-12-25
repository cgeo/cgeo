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

import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.Log

import androidx.annotation.Nullable

import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils


class PersonalNote {

    private static val SEPARATOR: String = "\n--\n"
    private static val PATTERN_SEPARATOR_SPLIT: Pattern = Pattern.compile("\\s*" + SEPARATOR + "\\s*")

    private String note
    private Boolean fromProvider

    public String getNote() {
        // non premium members have no personal notes, premium members have an empty string by default.
        // map both to null, so other code doesn't need to differentiate
        return StringUtils.defaultIfBlank(note, null)
    }

    public Unit setNote(final String note) {
        this.note = StringUtils.trimToNull(note)
    }

    public Unit setFromProvider(final Boolean fromProvider) {
        this.fromProvider = fromProvider
    }

    public final Unit gatherMissingDataFrom(final PersonalNote other) {
        // don't use StringUtils.isBlank here. Otherwise we cannot recognize a note which was deleted on GC
        if (Settings.isPersonalCacheNoteMergeDisabled()) {
            Log.d("running with personal note merging disabled")
            return
        }
        if (note == null) {
            note = other.note
        } else if (other.note != null && fromProvider && !other.fromProvider) {

            //Special logic to prevent overriding of local note with remote provider note
            // on cache refresh:
            //if this is an original provider note and the other one is the last local note,
            //then the LOCAL NOTE is preserved and the provider note's content is appended as needed
            val newNote: StringBuilder = StringBuilder(other.note)

            //Scan provider note entry-by-entry and preserve what is not already in local note
            //(entry-by-entry-scan prevents provider notes to get duplicated on multiple cache refreshes with minimal changes on provider side)
            if (StringUtils.isNotEmpty(this.note)) {
                for (String token : PATTERN_SEPARATOR_SPLIT.split(this.note)) {
                    val realToken: String = token.trim()
                    if (StringUtils.isNotEmpty(realToken) && !other.note.contains(realToken)) {
                        newNote.append(SEPARATOR).append(realToken)
                    }
                }
            }
            this.note = newNote.toString()
            //after above logic is applied, the resulting note is no longer the original one from provider
            this.fromProvider = false

        }
    }

    override     public Boolean equals(final Object o) {
        if (this == o) {
            return true
        }
        if (o == null || getClass() != o.getClass()) {
            return false
        }
        val that: PersonalNote = (PersonalNote) o
        return fromProvider == that.fromProvider &&
                StringUtils.equalsIgnoreCase(note, that.note)
    }

    override     public Int hashCode() {
        return note == null ? 13 : StringUtils.toRootLowerCase(note).hashCode()
    }

    override     public final String toString() {
        return note
    }

}
