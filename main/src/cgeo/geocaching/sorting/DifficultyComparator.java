package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by difficulty
 *
 */
public class DifficultyComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.difficulty != null && cache2.difficulty != null;
    }

    @Override
    protected int compareCaches(final cgCache cache1, final cgCache cache2) {
        if (cache1.difficulty > cache2.difficulty) {
            return 1;
        } else if (cache2.difficulty > cache1.difficulty) {
            return -1;
        }
        return 0;
    }
}