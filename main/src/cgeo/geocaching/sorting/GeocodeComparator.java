package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

import org.apache.commons.lang3.StringUtils;

/**
 * sorts caches by geo code, therefore effectively sorting by cache age
 * 
 */
public class GeocodeComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache) {
        // This will fallback to geocode comparisons.
        return false;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        throw new RuntimeException("should never be called");
    }
}
