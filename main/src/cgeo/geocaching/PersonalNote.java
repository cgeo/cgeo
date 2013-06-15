package cgeo.geocaching;


public class PersonalNote {
    private static final String CGEO_PREFIX = "cgeo:\n";
    private static final String SEPARATOR = "--\n";
    private String cgeoNote;
    private String providerNote;

    public static final PersonalNote parseFrom(final String note) {
        final PersonalNote result = new PersonalNote();
        final String personalNote = note;
        final int separatorIndex = personalNote.indexOf(SEPARATOR);
        final int cgeoIndex = personalNote.indexOf(CGEO_PREFIX) + CGEO_PREFIX.length();
        if (cgeoIndex > -1) {
            if (separatorIndex < cgeoIndex) {
                result.providerNote = personalNote;
            } else {
                result.cgeoNote = personalNote.substring(cgeoIndex, separatorIndex);
                result.providerNote = personalNote.substring(separatorIndex + SEPARATOR.length());
            }
        } else {
            result.providerNote = personalNote;
        }
        return result;
    }

    private PersonalNote() {
    }

    public PersonalNote mergeWith(final PersonalNote other) {
        final PersonalNote result = new PersonalNote();
        if (other.cgeoNote != null) {
            result.cgeoNote = other.cgeoNote;
        } else {
            result.cgeoNote = cgeoNote;
        }
        if (providerNote != null) {
            result.providerNote = providerNote;
        } else {
            result.providerNote = other.providerNote;
        }
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        if (cgeoNote != null) {
            buffer.append(CGEO_PREFIX).append(cgeoNote).append(SEPARATOR);
        }
        buffer.append(providerNote);
        return buffer.toString();
    }
}
