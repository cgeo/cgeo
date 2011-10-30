package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by size
 *
 */
public class SizeComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.getSize() != null && cache2.getSize() != null;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        return cache2.getSize().comparable - cache1.getSize().comparable;
    }
}