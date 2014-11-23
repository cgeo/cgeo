package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by size
 *
 */
class SizeComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache) {
        return cache.getSize() != null;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return cache2.getSize().comparable - cache1.getSize().comparable;
    }
}