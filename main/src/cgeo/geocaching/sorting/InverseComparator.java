package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * comparator which inverses the sort order of the given other comparator
 * 
 */
public class InverseComparator implements CacheComparator {

    private final CacheComparator originalComparator;

    public InverseComparator(final CacheComparator comparator) {
        this.originalComparator = comparator;
    }

    @Override
    public int compare(final Geocache lhs, final Geocache rhs) {
        return originalComparator.compare(rhs, lhs);
    }

}
