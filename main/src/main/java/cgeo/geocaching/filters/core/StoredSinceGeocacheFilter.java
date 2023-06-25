package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import java.util.Date;

public class StoredSinceGeocacheFilter extends DateRangeGeocacheFilter {

    @Override
    protected Date getDate(final Geocache cache) {
        return new Date(cache.getDetailedUpdate());
    }

    @Override
    protected String getSqlColumnName() {
        return "detailedupdate";
    }

}
