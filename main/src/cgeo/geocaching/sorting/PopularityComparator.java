package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by popularity (favorite count)
 *
 */
public class PopularityComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        if (cache1.getFavoritePoints() < cache2.getFavoritePoints()) {
            return 1;
        } else if (cache2.getFavoritePoints() < cache1.getFavoritePoints()) {
            return -1;
        }
        return 0;
    }
}
