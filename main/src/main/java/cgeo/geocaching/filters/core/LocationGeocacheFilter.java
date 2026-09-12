package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class LocationGeocacheFilter extends StringGeocacheFilter {

    public static LocationGeocacheFilter create(final String text) {
        return LocationGeocacheFilter.create(text, false, StringFilter.StringFilterType.CONTAINS);
    }

    public static LocationGeocacheFilter create(final String text, final boolean matchCase, final StringFilter.StringFilterType filterType) {
        return StringGeocacheFilter.create(GeocacheFilterType.LOCATION, text, matchCase, filterType);
    }

    public String getValue(final Geocache cache) {
        return cache.getLocation();
    }

    protected String getSqlColumnName() {
        return "location";
    }

}
