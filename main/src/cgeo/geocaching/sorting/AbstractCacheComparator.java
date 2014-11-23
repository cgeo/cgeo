package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

/**
 * abstract super implementation for all cache comparators
 *
 */
abstract class AbstractCacheComparator implements CacheComparator {

    @Override
    public final int compare(final Geocache cache1, final Geocache cache2) {
        try {
            final boolean canCompare1 = canCompare(cache1);
            final boolean canCompare2 = canCompare(cache2);
            if (!canCompare1) {
                return canCompare2 ? 1 : fallbackToGeocode(cache1, cache2);
            }
            return canCompare2 ? compareCaches(cache1, cache2) : -1;
        } catch (final Exception e) {
            Log.e("AbstractCacheComparator.compare", e);
            // This may violate the Comparator interface if the exception is not systematic.
            return fallbackToGeocode(cache1, cache2);
        }
    }

    private static int fallbackToGeocode(final Geocache cache1, final Geocache cache2) {
        return StringUtils.defaultString(cache1.getGeocode()).compareToIgnoreCase(StringUtils.defaultString(cache2.getGeocode()));
    }

    /**
     * Check necessary preconditions (like missing fields) before running the comparison itself.
     * Caches not filling the conditions will be placed last, sorted by Geocode.
     *
     * The default returns <code>true</code> and can be overridden if needed in child classes.
     *
     * @param cache
     * @return <code>true</code> if the cache holds the necessary data to be compared meaningfully
     */
    @SuppressWarnings("static-method")
    protected boolean canCompare(final Geocache cache) {
        return true;
    }

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
    protected abstract int compareCaches(final Geocache cache1, final Geocache cache2);

}
