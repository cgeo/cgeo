package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.utils.Log;


/**
 * abstract super implementation for all cache comparators
 *
 */
public abstract class AbstractCacheComparator implements CacheComparator {

    @Override
    public final int compare(final cgCache cache1, final cgCache cache2) {
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
     * Check necessary preconditions (like missing fields) before running the comparison itself
     *
     * @param cache1
     * @param cache2
     * @return
     */
    protected abstract boolean canCompare(final cgCache cache1, final cgCache cache2);

    /**
     * Compares two caches. Logging and exception handling is implemented outside this method already.
     * <p/>
     * A cache is smaller than another cache if it is desirable to show it first when presented to the user.
     * For example, a highly rated cache must be considered smaller than a poorly rated one.
     *
     * @param cache1
     * @param cache2
     * @return an integer < 0 if cache1 is less than cache2, 0 if they are equal, and > 0 if cache1 is greater than
     *         cache2.
     */
    protected abstract int compareCaches(final cgCache cache1, final cgCache cache2);
}
