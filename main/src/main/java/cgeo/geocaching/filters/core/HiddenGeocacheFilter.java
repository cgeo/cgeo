package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import java.util.Date;


public class HiddenGeocacheFilter extends DateRangeGeocacheFilter {

    public static HiddenGeocacheFilter create(final Date min, final Date max) {
        return DateRangeGeocacheFilter.create(GeocacheFilterType.HIDDEN, min, max);
    }

    @Override
    protected Date getDate(final Geocache cache) {
        return cache.getHiddenDate();
    }

    @Override
    protected String getSqlColumnName() {
        return "hidden";
    }
}
