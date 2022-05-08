package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class DescriptionGeocacheFilter extends StringGeocacheFilter {

    public String getValue(final Geocache cache) {
        return cache.getDescription();
    }


    protected String getSqlColumnName() {
        return "description";
    }

}
