package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import java.util.ArrayList;
import java.util.List;

class PopularityFilter extends AbstractFilter {
    private final int minFavorites;
    private final int maxFavorites;

    public PopularityFilter(String name, final int minFavorites, final int maxFavorites) {
        super(name);
        this.minFavorites = minFavorites;
        this.maxFavorites = maxFavorites;
    }

    @Override
    public boolean accepts(final Geocache cache) {
        return (cache.getFavoritePoints() > minFavorites) && (cache.getFavoritePoints() <= maxFavorites);
    }

    public static class Factory implements IFilterFactory {

        private static final int[] FAVORITES = { 10, 20, 50, 100, 200, 500 };

        @Override
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<IFilter>(FAVORITES.length);
            for (int i = 0; i < FAVORITES.length; i++) {
                final int minRange = FAVORITES[i];
                final int maxRange = Integer.MAX_VALUE;
                final String range = "> " + minRange;
                final String name = CgeoApplication.getInstance().getResources().getQuantityString(R.plurals.tts_favorite_points, minRange, range);
                filters.add(new PopularityFilter(name, minRange, maxRange));
            }
            return filters;
        }

    }
}
