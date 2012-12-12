package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * comparator which inverses the sort order of the given other comparator
 * 
 */
public class InverseComparator implements CacheComparator {

    private final CacheComparator originalComparator;

    public InverseComparator(CacheComparator comparator) {
        this.originalComparator = comparator;
    }

    @Override
    public int compare(cgCache lhs, cgCache rhs) {
        return originalComparator.compare(rhs, lhs);
    }

}
