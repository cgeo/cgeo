package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class DescriptionGeocacheFilter extends StringGeocacheFilter {

    public static DescriptionGeocacheFilter create(final String text) {
        return DescriptionGeocacheFilter.create(text, false, StringFilter.StringFilterType.CONTAINS);
    }

    public static DescriptionGeocacheFilter create(final String text, final boolean matchCase, final StringFilter.StringFilterType filterType) {
        return StringGeocacheFilter.create(GeocacheFilterType.DESCRIPTION, text, matchCase, filterType);
    }

    public String getValue(final Geocache cache) {
        return cache.getDescription();
    }


    protected String getSqlColumnName() {
        return "description";
    }

}
