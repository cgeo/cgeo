package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class OwnerGeocacheFilter extends StringGeocacheFilter {

    public static OwnerGeocacheFilter create(final String text) {
        return OwnerGeocacheFilter.create(text, false, StringFilter.StringFilterType.CONTAINS);
    }

    public static OwnerGeocacheFilter create(final String text, final boolean matchCase, final StringFilter.StringFilterType filterType) {
        return StringGeocacheFilter.create(GeocacheFilterType.OWNER, text, matchCase, filterType);
    }

    public String getValue(final Geocache cache) {
        return cache.getOwnerDisplayName();
    }

    protected String getSqlColumnName() {
        return "owner";
    }


}
