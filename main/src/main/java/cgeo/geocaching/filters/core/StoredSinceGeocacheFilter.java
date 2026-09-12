package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import java.util.Date;

public class StoredSinceGeocacheFilter extends DateRangeGeocacheFilter {

    public static StoredSinceGeocacheFilter create(final Date min, final Date max) {
        return DateRangeGeocacheFilter.create(GeocacheFilterType.STORED_SINCE, min, max);
    }

    @Override
    protected Date getDate(final Geocache cache) {
        return new Date(cache.getDetailedUpdate());
    }

    @Override
    protected String getSqlColumnName() {
        return "detailedupdate";
    }

}
