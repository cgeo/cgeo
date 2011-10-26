package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by popularity (favorite count)
 *
 */
public class PopularityComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.getFavouriteCnt() != null && cache2.getFavouriteCnt() != null;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        if (cache1.getFavouriteCnt() < cache2.getFavouriteCnt()) {
            return 1;
        } else if (cache2.getFavouriteCnt() < cache1.getFavouriteCnt()) {
            return -1;
        }
        return 0;
    }
}
