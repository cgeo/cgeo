package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LogType;

import java.util.ArrayList;
import java.util.List;

/**
 * filters caches by popularity ratio (favorites per find in %).
 */
class PopularityRatioFilter extends AbstractFilter {
    private final int minRatio;
    private final int maxRatio;

    public PopularityRatioFilter(String name, final int minRatio, final int maxRatio) {
        super(name);
        this.minRatio = minRatio;
        this.maxRatio = maxRatio;
    }

    @Override
    public boolean accepts(final Geocache cache) {

        int finds;
        int favorites;
        float ratio;

        finds = getFindsCount(cache);

        // prevent division by zero
        if (finds == 0) {
            return false;
        }

        favorites = cache.getFavoritePoints();
        ratio = ((float) favorites / (float) finds) * 100.0f;

        return (ratio > minRatio) && (ratio <= maxRatio);
    }

    private static int getFindsCount(Geocache cache) {
        if (cache.getLogCounts().isEmpty()) {
            cache.setLogCounts(DataStore.loadLogCounts(cache.getGeocode()));
        }
        Integer logged = cache.getLogCounts().get(LogType.FOUND_IT);
        if (logged != null) {
            return logged;
        }
        return 0;
    }

    public static class Factory implements IFilterFactory {

        private static final int[] RATIOS = { 10, 20, 30, 40, 50, 75 };

        @Override
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<IFilter>(RATIOS.length);
            for (int i = 0; i < RATIOS.length; i++) {
                final int minRange = RATIOS[i];
                final int maxRange = Integer.MAX_VALUE;
                final String name = "> " + minRange + " " + CgeoApplication.getInstance().getResources().getString(R.string.percent_favorite_points);
                filters.add(new PopularityRatioFilter(name, minRange, maxRange));
            }
            return filters;
        }

    }
}
