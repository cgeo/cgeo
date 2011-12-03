package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by difficulty
 *
 */
public class DifficultyComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.getDifficulty() != 0.0 && cache2.getDifficulty() != 0.0;
    }

    @Override
    protected int compareCaches(final cgCache cache1, final cgCache cache2) {
        if (cache1.getDifficulty() > cache2.getDifficulty()) {
            return 1;
        } else if (cache2.getDifficulty() > cache1.getDifficulty()) {
            return -1;
        }
        return 0;
    }
}