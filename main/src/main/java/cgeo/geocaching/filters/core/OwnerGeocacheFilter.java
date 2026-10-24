package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import org.apache.commons.lang3.StringUtils;

public class OwnerGeocacheFilter extends StringGeocacheFilter {

    public String getValue(final Geocache cache) {
        final String ownerName = cache.getOwnerDisplayName();
        return StringUtils.isNotBlank(ownerName) ? ownerName : null;
    }

    protected String getSqlColumnName() {
        return "owner";
    }


}
