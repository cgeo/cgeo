package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by geo code, therefore effectively sorting by cache age
 *
 */
class GeocodeComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache) {
        // This will fall back to geocode comparisons.
        return false;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        throw new RuntimeException("should never be called");
    }
}
