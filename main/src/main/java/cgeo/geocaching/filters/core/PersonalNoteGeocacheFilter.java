package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class PersonalNoteGeocacheFilter extends StringGeocacheFilter {

    public static PersonalNoteGeocacheFilter create(final String text) {
        return PersonalNoteGeocacheFilter.create(text, false, StringFilter.StringFilterType.CONTAINS);
    }

    public static PersonalNoteGeocacheFilter create(final String text, final boolean matchCase, final StringFilter.StringFilterType filterType) {
        return StringGeocacheFilter.create(GeocacheFilterType.PERSONAL_NOTE, text, matchCase, filterType);
    }

    public String getValue(final Geocache cache) {
        return cache.getPersonalNote() == null ? "" : cache.getPersonalNote();
    }

    protected String getSqlColumnName() {
        return "personal_note";
    }

}
