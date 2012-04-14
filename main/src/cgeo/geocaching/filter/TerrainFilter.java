package cgeo.geocaching.filter;


import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;

import java.util.ArrayList;

public class TerrainFilter extends AbstractRangeFilter {

    public TerrainFilter(int terrain) {
        super(R.string.cache_terrain, terrain);
    }

    @Override
    public boolean accepts(cgCache cache) {
        return rangeMin <= cache.getTerrain() && cache.getTerrain() < rangeMax;
    }

    public static class Factory implements FilterFactory {
        @Override
        public IFilter[] getFilters() {
            final ArrayList<IFilter> filters = new ArrayList<IFilter>(5);
            for (int terrain = 1; terrain <= 5; terrain++) {
                filters.add(new TerrainFilter(terrain));
            }
            return filters.toArray(new IFilter[filters.size()]);
        }
    }

}
