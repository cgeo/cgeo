/**
 *
 */
package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by popularity ratio (favorites per find in %).
 */
public class PopularityRatioComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {

        float ratio1 = 0.0f;
        float ratio2 = 0.0f;

        int finds1 = cache1.getFindsCount();
        int finds2 = cache2.getFindsCount();

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
}
