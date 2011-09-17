package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by popularity (favorite count)
 *
 */
public class PopularityComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.favouriteCnt != null && cache2.favouriteCnt != null;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        if (cache1.favouriteCnt < cache2.favouriteCnt) {
            return 1;
        } else if (cache2.favouriteCnt < cache1.favouriteCnt) {
            return -1;
        }
        return 0;
    }
}
