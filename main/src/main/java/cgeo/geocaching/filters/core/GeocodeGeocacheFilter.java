package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class GeocodeGeocacheFilter extends StringGeocacheFilter {

    public String getValue(final Geocache cache) {
        return cache.getGeocode();
    }

    protected String getSqlColumnName() {
        return "geocode";
    }

}
