package cgeo.geocaching.models;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;


public class PersonalNote {

    private static final String SEPARATOR = "\n--\n";
    private static final Pattern PATTERN_SEPARATOR_SPLIT = Pattern.compile("\\s*" + SEPARATOR + "\\s*");

    private String note;
    private boolean fromProvider;

    @Nullable
    public String getNote() {
        // non premium members have no personal notes, premium members have an empty string by default.
        // map both to null, so other code doesn't need to differentiate
        return StringUtils.defaultIfBlank(note, null);
    }

    public void setNote(final String note) {
        this.note = StringUtils.trimToNull(note);
    }

    public void setFromProvider(final boolean fromProvider) {
        this.fromProvider = fromProvider;
    }

    public final void gatherMissingDataFrom(final PersonalNote other) {
        // don't use StringUtils.isBlank here. Otherwise we cannot recognize a note which was deleted on GC
        if (Settings.isPersonalCacheNoteMergeDisabled()) {
            Log.d("running with personal note merging disabled");
            return;
        }
        if (note == null) {
            note = other.note;
        } else if (other.note != null && fromProvider && !other.fromProvider) {

            //Special logic to prevent overriding of local note with remote provider note
            // on cache refresh:
            //if this is an original provider note and the other one is the last local note,
            //then the LOCAL NOTE is preserved and the provider note's content is appended as needed
            final StringBuilder newNote = new StringBuilder(other.note);

            //Scan provider note entry-by-entry and preserve what is not already in local note
            //(entry-by-entry-scan prevents provider notes to get duplicated on multiple cache refreshes with minimal changes on provider side)
            if (StringUtils.isNotEmpty(this.note)) {
                for (String token : PATTERN_SEPARATOR_SPLIT.split(this.note)) {
                    final String realToken = token.trim();
                    if (StringUtils.isNotEmpty(realToken) && !other.note.contains(realToken)) {
                        newNote.append(SEPARATOR).append(realToken);
                    }
                }
            }
            this.note = newNote.toString();
            //after above logic is applied, the resulting note is no longer the original one from provider
            this.fromProvider = false;

        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PersonalNote that = (PersonalNote) o;
        return fromProvider == that.fromProvider &&
                StringUtils.equalsIgnoreCase(note, that.note);
    }

    @Override
    public int hashCode() {
        return note == null ? 13 : StringUtils.toRootLowerCase(note).hashCode();
    }

    @Override
    public final String toString() {
        return note;
    }

}
