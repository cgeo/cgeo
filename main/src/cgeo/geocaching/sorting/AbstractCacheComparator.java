package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.utils.Log;


/**
 * abstract super implementation for all cache comparators
 *
 */
public abstract class AbstractCacheComparator implements CacheComparator {

    @Override
    public final int compare(cgCache cache1, cgCache cache2) {
        try {
            // first check that we have all necessary data for the comparison
            if (!canCompare(cache1, cache2)) {
                return 0;
            }
            return compareCaches(cache1, cache2);
        } catch (Exception e) {
            Log.e("AbstractCacheComparator.compare: " + e.toString());
        }
        return 0;
    }

    /**
     * check necessary preconditions (like missing fields) before running the comparison itself
     *
     * @param cache1
     * @param cache2
     * @return
     */
    protected abstract boolean canCompare(final cgCache cache1, final cgCache cache2);

    /**
     * compares two caches. Logging and exception handling is implemented outside this method already.
     *
     * @param cache1
     * @param cache2
     * @return an integer < 0 if cache1 is less than cache2, 0 if they are equal, and > 0 if cache1 is greater than
     *         cache2.
     */
    protected abstract int compareCaches(final cgCache cache1, final cgCache cache2);
}
