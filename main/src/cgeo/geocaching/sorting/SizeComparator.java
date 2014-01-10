package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by size
 *
 */
public class SizeComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(Geocache cache) {
        return cache.getSize() != null;
    }

    @Override
    protected int compareCaches(Geocache cache1, Geocache cache2) {
        return cache2.getSize().comparable - cache1.getSize().comparable;
    }
}