package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by terrain rating
 * 
 */
public class TerrainComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.terrain != null && cache2.terrain != null;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        if (cache1.terrain > cache2.terrain) {
            return 1;
        } else if (cache2.terrain > cache1.terrain) {
            return -1;
        }
        return 0;
    }
}
