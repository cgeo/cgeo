package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by terrain rating
 *
 */
public class TerrainComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final cgCache cache1, final cgCache cache2) {
        return cache1.getTerrain() != 0.0 && cache2.getTerrain() != 0.0;
    }

    @Override
    protected int compareCaches(final cgCache cache1, final cgCache cache2) {
        return Float.compare(cache1.getTerrain(), cache2.getTerrain());
    }
}
