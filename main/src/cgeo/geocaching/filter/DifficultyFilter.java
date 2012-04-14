package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;

import java.util.ArrayList;

class DifficultyFilter extends AbstractRangeFilter {

    public DifficultyFilter(int difficulty) {
        super(R.string.cache_difficulty, difficulty);
    }

    @Override
    public boolean accepts(cgCache cache) {
        return rangeMin <= cache.getDifficulty() && cache.getDifficulty() < rangeMax;
    }

    public static class Factory implements IFilterFactory {

        @Override
        public IFilter[] getFilters() {
            final ArrayList<IFilter> filters = new ArrayList<IFilter>(5);
            for (int difficulty = 1; difficulty <= 5; difficulty++) {
                filters.add(new DifficultyFilter(difficulty));
            }
            return filters.toArray(new IFilter[filters.size()]);
        }

    }
}
