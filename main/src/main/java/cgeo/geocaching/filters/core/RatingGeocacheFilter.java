package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import java.util.Collection;

public class RatingGeocacheFilter extends NumberRangeGeocacheFilter<Float> {

    public static RatingGeocacheFilter create(final Float min, final Float max) {
        return NumberRangeGeocacheFilter.create(GeocacheFilterType.RATING, min, max);
    }

    public static RatingGeocacheFilter create(final Collection<Float> values, final Float minUnlimitedValue, final Float maxUnlimitedValue) {
        return NumberRangeGeocacheFilter.create(GeocacheFilterType.RATING, values, minUnlimitedValue, maxUnlimitedValue);
    }

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
