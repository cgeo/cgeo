package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by popularity (favorite count)
 *
 */
public class PopularityComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache1, final Geocache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return cache2.getFavoritePoints() - cache1.getFavoritePoints();
    }
}
