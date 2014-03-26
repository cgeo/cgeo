package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by last visited date
 *
 */
public class VisitComparator extends AbstractCacheComparator {

    final static public VisitComparator singleton = new VisitComparator();

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Long.valueOf(cache2.getVisitedDate()).compareTo(cache1.getVisitedDate());
    }
}
