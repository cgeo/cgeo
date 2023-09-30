package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class RatingGeocacheFilter extends NumberRangeGeocacheFilter<Float> {

    public RatingGeocacheFilter() {
        super(Float::valueOf, f -> f);
    }

    @Override
    public Float getValue(final Geocache cache) {
        return cache.getRating();
    }

    @Override
    protected String getSqlColumnName() {
        return "rating";
    }

}
