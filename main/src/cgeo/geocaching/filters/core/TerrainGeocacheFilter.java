package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class TerrainGeocacheFilter extends NumberRangeGeocacheFilter<Float> {

    public TerrainGeocacheFilter() {
        super(Float::valueOf);
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
