package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class OwnerGeocacheFilter extends StringGeocacheFilter {

    public String getValue(final Geocache cache) {
        return cache.getOwnerDisplayName();
    }

    protected String getSqlColumnName() {
        return "owner";
    }


}
