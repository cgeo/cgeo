package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class NameGeocacheFilter extends StringGeocacheFilter {

    public static NameGeocacheFilter create(final String text) {
        final NameGeocacheFilter nf = GeocacheFilterType.NAME.create();
        nf.getStringFilter().setTextValue(text);
        return nf;
    }

    public String getValue(final Geocache cache) {
        return cache.getName();
    }

    protected String getSqlColumnName() {
        return "name";
    }

}
