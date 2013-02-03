package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by difficulty
 *
 */
public class DifficultyComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(Geocache cache1, Geocache cache2) {
        return cache1.getDifficulty() != 0.0 && cache2.getDifficulty() != 0.0;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Float.compare(cache1.getDifficulty(), cache2.getDifficulty());
    }
}