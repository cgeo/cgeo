package cgeo.geocaching;

import org.apache.commons.lang3.StringUtils;


public class PersonalNote {
    private static final String SEPARATOR = "\n--\n";
    private String cgeoNote;
    private String providerNote;
    private boolean isOffline;

    private PersonalNote() {
        // Empty default constructor
    }

    public PersonalNote(final Geocache cache) {
        this.isOffline = cache.isOffline();
        final String personalNote = cache.getPersonalNote();
        if (StringUtils.isEmpty(personalNote)) {
            return;
        }
        final String[] notes = StringUtils.splitByWholeSeparator(personalNote, SEPARATOR);
        if (notes.length > 1) {
            this.cgeoNote = notes[0];
            this.providerNote = notes[1];
        } else {
            this.providerNote = notes[0];
        }
    }

    public final PersonalNote mergeWith(final PersonalNote other) {
        if (StringUtils.isEmpty(cgeoNote) && StringUtils.isEmpty(other.cgeoNote)) {
            return mergeOnlyProviderNotes(other);
        }
        final PersonalNote result = new PersonalNote();
        if (cgeoNote != null && other.cgeoNote != null) {
            if (other.isOffline) {
                result.cgeoNote = other.cgeoNote;
            } else {
                result.cgeoNote = cgeoNote;
            }
        }
        if (other.cgeoNote != null) {
            result.cgeoNote = other.cgeoNote;
        } else {
            result.cgeoNote = cgeoNote;
        }
        if (providerNote != null && other.providerNote != null) {
            if (isOffline) {
                result.providerNote = providerNote;
            } else {
                result.providerNote = other.providerNote;
            }
        }
        if (providerNote != null) {
            result.providerNote = providerNote;
        } else {
            result.providerNote = other.providerNote;
        }
        return result;
    }

    /**
     * Merge different provider notes from c:geo and provider.
     *
     * @param other
     *            The note to merge
     * @return PersonalNote The merged note
     */
    private PersonalNote mergeOnlyProviderNotes(final PersonalNote other) {
        final PersonalNote result = new PersonalNote();
        if (StringUtils.isNotEmpty(other.providerNote) && StringUtils.isNotEmpty(providerNote)) {
            if (providerNote.equals(other.providerNote)) {
                result.providerNote = providerNote;
                return result;
            }
            if (other.isOffline) {
                result.cgeoNote = other.providerNote;
                result.providerNote = providerNote;
            } else {
                result.cgeoNote = providerNote;
                result.providerNote = other.providerNote;
            }
        }
        return result;
    }

    @Override
    public final String toString() {
        final StringBuilder builder = new StringBuilder();
        if (cgeoNote != null) {
            builder.append(cgeoNote).append(SEPARATOR);
        }
        builder.append(providerNote);
        return builder.toString();
    }

    public final String getCgeoNote() {
        return cgeoNote;
    }

    public final String getProviderNote() {
        return providerNote;
    }

}
