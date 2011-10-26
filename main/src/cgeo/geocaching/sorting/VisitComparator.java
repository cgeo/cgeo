package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by last visited date
 *
 */
public class VisitComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.getVisitedDate() != null && cache1.getVisitedDate() > 0
                && cache2.getVisitedDate() != null && cache2.getVisitedDate() > 0;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        if (cache1.getVisitedDate() > cache2.getVisitedDate()) {
            return -1;
        } else if (cache1.getVisitedDate() < cache2.getVisitedDate()) {
            return 1;
        }
        return 0;
    }
}
