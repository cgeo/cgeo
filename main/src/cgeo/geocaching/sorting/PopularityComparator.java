package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by popularity (favorite count)
 *
 */
class PopularityComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return cache2.getFavoritePoints() - cache1.getFavoritePoints();
    }
}
