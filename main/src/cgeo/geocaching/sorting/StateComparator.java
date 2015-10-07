package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sort caches by state (normal, disabled, archived)
 *
 */
class StateComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return getState(cache1) - getState(cache2);
    }

    private static int getState(final Geocache cache) {
        if (cache.isDisabled()) {
            return 1;
        }
        if (cache.isArchived()) {
            return 2;
        }
        return 0;
    }

}
