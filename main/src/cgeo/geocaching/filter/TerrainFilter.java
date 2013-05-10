package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import java.util.ArrayList;
import java.util.List;

class TerrainFilter extends AbstractRangeFilter {

    public TerrainFilter(int terrain) {
        super(R.string.cache_terrain, terrain);
    }

    @Override
    public boolean accepts(Geocache cache) {
        return rangeMin <= cache.getTerrain() && cache.getTerrain() < rangeMax;
    }

    public static class Factory implements IFilterFactory {
        @Override
        public List<IFilter> getFilters() {
            final ArrayList<IFilter> filters = new ArrayList<IFilter>(5);
            for (int terrain = 1; terrain <= 5; terrain++) {
                filters.add(new TerrainFilter(terrain));
            }
            return filters;
        }
    }

}
