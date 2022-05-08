package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class LocationGeocacheFilter extends StringGeocacheFilter {

    public String getValue(final Geocache cache) {
        return cache.getLocation();
    }

    protected String getSqlColumnName() {
        return "location";
    }

}
