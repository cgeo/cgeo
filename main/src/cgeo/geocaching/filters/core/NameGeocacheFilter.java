package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class NameGeocacheFilter extends StringGeocacheFilter {

    public String getValue(final Geocache cache) {
        return cache.getName();
    }

    protected String getSqlColumnName() {
        return "name";
    }

}
