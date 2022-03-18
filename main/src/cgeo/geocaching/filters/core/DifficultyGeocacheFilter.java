package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class DifficultyGeocacheFilter extends NumberRangeGeocacheFilter<Float> {

    public DifficultyGeocacheFilter() {
        super(Float::valueOf);
    }

    @Override
    public Float getValue(final Geocache cache) {
        return cache.getDifficulty();
    }

    @Override
    protected String getSqlColumnName() {
        return "difficulty";
    }

}
