package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class NameGeocacheFilter extends StringGeocacheFilter {

    public static NameGeocacheFilter create(final String text) {
        return NameGeocacheFilter.create(text, false, StringFilter.StringFilterType.CONTAINS);
    }

    public static NameGeocacheFilter create(final String text, final boolean matchCase, final StringFilter.StringFilterType filterType) {
        return StringGeocacheFilter.create(GeocacheFilterType.NAME, text, matchCase, filterType);
    }

    public String getValue(final Geocache cache) {
        return cache.getName();
    }

    protected String getSqlColumnName() {
        return "name";
    }

}
