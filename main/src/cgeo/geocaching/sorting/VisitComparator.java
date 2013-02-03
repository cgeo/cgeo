package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by last visited date
 *
 */
public class VisitComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache1, final Geocache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Long.valueOf(cache2.getVisitedDate()).compareTo(cache1.getVisitedDate());
    }
}
