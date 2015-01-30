package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.LogType;

import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * filters caches by popularity ratio (favorites per find in %).
 */
class PopularityRatioFilter extends AbstractFilter {
    private final int minRatio;
    private final int maxRatio;

    public PopularityRatioFilter(@NonNull final String name, final int minRatio, final int maxRatio) {
        super(name);
        this.minRatio = minRatio;
        this.maxRatio = maxRatio;
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        final int finds = getFindsCount(cache);

        if (finds == 0) {   // Prevent division by zero
            return false;
        }

        final int favorites = cache.getFavoritePoints();
        final float ratio = 100.0f * favorites / finds;
        return ratio > minRatio && ratio <= maxRatio;
    }

    private static int getFindsCount(final Geocache cache) {
        if (cache.getLogCounts().isEmpty()) {
            cache.setLogCounts(DataStore.loadLogCounts(cache.getGeocode()));
        }
        final Integer logged = cache.getLogCounts().get(LogType.FOUND_IT);
        if (logged != null) {
            return logged;
        }
        return 0;
    }

    public static class Factory implements IFilterFactory {

        private static final int[] RATIOS = { 10, 20, 30, 40, 50, 75 };

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<>(RATIOS.length);
            for (final int minRange : RATIOS) {
                final int maxRange = Integer.MAX_VALUE;
                final String name = CgeoApplication.getInstance().getResources().getString(R.string.more_than_percent_favorite_points, minRange);
                filters.add(new PopularityRatioFilter(name, minRange, maxRange));
            }
            return filters;
        }

    }
}
