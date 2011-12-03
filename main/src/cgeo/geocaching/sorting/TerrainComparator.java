package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by terrain rating
 *
 */
public class TerrainComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.getTerrain() != 0.0 && cache2.getTerrain() != 0.0;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        if (cache1.getTerrain() > cache2.getTerrain()) {
            return 1;
        } else if (cache2.getTerrain() > cache1.getTerrain()) {
            return -1;
        }
        return 0;
    }
}
