package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import java.util.Collection;

public class DifficultyGeocacheFilter extends NumberRangeGeocacheFilter<Float> {

    public static DifficultyGeocacheFilter create(final Float min, final Float max) {
        return NumberRangeGeocacheFilter.create(GeocacheFilterType.DIFFICULTY, min, max);
    }

    public static DifficultyGeocacheFilter create(final Collection<Float> values, final Float minUnlimitedValue, final Float maxUnlimitedValue) {
        return NumberRangeGeocacheFilter.create(GeocacheFilterType.DIFFICULTY, values, minUnlimitedValue, maxUnlimitedValue);
    }

    public DifficultyGeocacheFilter() {
        super(Float::valueOf, f -> f);
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
