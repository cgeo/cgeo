package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

class TerrainFilter extends AbstractRangeFilter {

    public TerrainFilter(final int terrain) {
        super(R.string.cache_terrain, terrain);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        final float terrain = cache.getTerrain();
        return rangeMin <= terrain && terrain < rangeMax;
    }

    public static class Factory implements IFilterFactory {
        private static final int TERRAIN_MIN = 1;
        private static final int TERRAIN_MAX = 7;

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<>(TERRAIN_MAX);
            for (int terrain = TERRAIN_MIN; terrain <= TERRAIN_MAX; terrain++) {
                filters.add(new TerrainFilter(terrain));
            }
            return filters;
        }
    }

}
