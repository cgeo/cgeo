package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import java.util.Date;


public class HiddenGeocacheFilter extends DateRangeGeocacheFilter {

    @Override
    protected Date getDate(final Geocache cache) {
        return cache.getHiddenDate();
    }

    @Override
    protected String getSqlColumnName() {
        return "hidden";
    }
}
