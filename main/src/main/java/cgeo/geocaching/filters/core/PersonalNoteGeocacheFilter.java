package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class PersonalNoteGeocacheFilter extends StringGeocacheFilter {

    public String getValue(final Geocache cache) {
        return cache.getPersonalNote() == null ? "" : cache.getPersonalNote();
    }

    protected String getSqlColumnName() {
        return "personal_note";
    }

}
