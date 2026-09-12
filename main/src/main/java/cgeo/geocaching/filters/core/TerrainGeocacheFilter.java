package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import java.util.Collection;

public class TerrainGeocacheFilter extends NumberRangeGeocacheFilter<Float> {

    public static TerrainGeocacheFilter create(final Float min, final Float max) {
        return NumberRangeGeocacheFilter.create(GeocacheFilterType.TERRAIN, min, max);
    }

    public static TerrainGeocacheFilter create(final Collection<Float> values, final Float minUnlimitedValue, final Float maxUnlimitedValue) {
        return NumberRangeGeocacheFilter.create(GeocacheFilterType.TERRAIN, values, minUnlimitedValue, maxUnlimitedValue);
    }

    public TerrainGeocacheFilter() {
        super(Float::valueOf, f -> f);
    }

    @Override
    public Float getValue(final Geocache cache) {
        return cache.getTerrain();
    }

    @Override
    protected String getSqlColumnName() {
        return "terrain";
    }

}
