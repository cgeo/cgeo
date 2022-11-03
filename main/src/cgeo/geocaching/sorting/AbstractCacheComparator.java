package cgeo.geocaching.sorting;

import cgeo.geocaching.connector.gc.GCUtils;
import cgeo.geocaching.models.Geocache;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * abstract super implementation for all cache comparators
 */
abstract class AbstractCacheComparator implements CacheComparator {

    @Override
    public final int compare(final Geocache cache1, final Geocache cache2) {
        final boolean canCompare1 = canCompare(cache1);
        final boolean canCompare2 = canCompare(cache2);
        if (!canCompare1) {
            return canCompare2 ? 1 : fallbackToGeocode(cache1, cache2);
        }
        return canCompare2 ? compareCaches(cache1, cache2) : -1;
    }

    private static int fallbackToGeocode(final Geocache cache1, final Geocache cache2) {
        final int comparePrefix = StringUtils.compareIgnoreCase(StringUtils.substring(cache1.getGeocode(), 0, 2), StringUtils.substring(cache2.getGeocode(), 0, 2));
        if (comparePrefix == 0) {
            final long l1 = GCUtils.gcLikeCodeToGcLikeId(cache1.getGeocode());
            final long l2 = GCUtils.gcLikeCodeToGcLikeId(cache2.getGeocode());
            if (l1 != l2) {
                return l1 > l2 ? 1 : -1;
            }
        }
        return comparePrefix;
    }

    /**
     * Check necessary preconditions (like missing fields) before running the comparison itself.
     * Caches not filling the conditions will be placed last, sorted by Geocode.
     *
     * The default implementation returns {@code true} and can be overridden if needed in sub classes.
     *
     * @param cache the cache to be sorted
     * @return {@code true} if the cache holds the necessary data to be compared meaningfully
     */
    protected boolean canCompare(final Geocache cache) {
        return true;
    }

    /**
     * Compares two caches. Logging and exception handling is implemented outside this method already.
     * <p/>
     * A cache is smaller than another cache if it is desirable to show it first when presented to the user.
     * For example, a highly rated cache must be considered smaller than a poorly rated one.
     *
     * @return an integer < 0 if cache1 is less than cache2, 0 if they are equal, and > 0 if cache1 is greater than
     * cache2.
     */
    protected abstract int compareCaches(Geocache cache1, Geocache cache2);

    /**
     * Can optinally be overridden to perform preparation (e.g. caching of values) before sort of a list via {@link #sort(List)}
     */
    protected void beforeSort(final List<Geocache> list) {
        //by default, do nothing
    }

    /**
     * Can optinally be overridden to perform cleanup (e.g. deleting cached values) before sort of a list via {@link #sort(List)}
     */
    protected void afterSort(final List<Geocache> list) {
        //by default, do nothing
    }

    /**
     * Sorts the given list of caches using this comparator. Respects implementations of {@link #beforeSort(List)} and{@link #afterSort(List)}
     */
    public void sort(final List<Geocache> list) {
        beforeSort(list);
        Collections.sort(list, this);
        afterSort(list);
    }

}
