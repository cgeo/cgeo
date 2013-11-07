/**
 *
 */
package cgeo.geocaching.sorting;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.LogType;

/**
 * sorts caches by popularity ratio (favorites per find in %).
 */
public class PopularityRatioComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache1, final Geocache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {

        float ratio1 = 0.0f;
        float ratio2 = 0.0f;

        int finds1 = getFindsCount(cache1);
        int finds2 = getFindsCount(cache2);

        if (finds1 != 0) {
            ratio1 = (((float) cache1.getFavoritePoints()) / ((float) finds1));
        }
        if (finds2 != 0) {
            ratio2 = (((float) cache2.getFavoritePoints()) / ((float) finds2));
        }

        if ((ratio2 - ratio1) > 0.0f) {
            return 1;
        } else if ((ratio2 - ratio1) < 0.0f) {
            return -1;
        }

        return 0;
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
}
