package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by popularity (favorite count)
 *
 */
public class PopularityComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final cgCache cache1, final cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(final cgCache cache1, final cgCache cache2) {
        return cache2.getFavoritePoints() - cache1.getFavoritePoints();
    }
}
