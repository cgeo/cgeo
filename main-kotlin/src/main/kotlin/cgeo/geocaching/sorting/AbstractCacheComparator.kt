// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.sorting

import cgeo.geocaching.connector.gc.GCUtils
import cgeo.geocaching.models.Geocache

import java.util.Collections
import java.util.List

import org.apache.commons.lang3.StringUtils

/**
 * abstract super implementation for all cache comparators
 */
abstract class AbstractCacheComparator : CacheComparator {


    override     public final Int compare(final Geocache cache1, final Geocache cache2) {
        val canCompare1: Boolean = canCompare(cache1)
        val canCompare2: Boolean = canCompare(cache2)
        if (!canCompare1) {
            return canCompare2 ? 1 : fallbackToGeocode(cache1, cache2)
        }
        return canCompare2 ? compareCaches(cache1, cache2) : -1
    }

    private static Int fallbackToGeocode(final Geocache cache1, final Geocache cache2) {
        val comparePrefix: Int = StringUtils.compareIgnoreCase(StringUtils.substring(cache1.getGeocode(), 0, 2), StringUtils.substring(cache2.getGeocode(), 0, 2))
        if (comparePrefix == 0) {
            val l1: Long = GCUtils.gcLikeCodeToGcLikeId(cache1.getGeocode())
            val l2: Long = GCUtils.gcLikeCodeToGcLikeId(cache2.getGeocode())
            if (l1 != l2) {
                return l1 > l2 ? 1 : -1
            }
        }
        return comparePrefix
    }

    /**
     * Check necessary preconditions (like missing fields) before running the comparison itself.
     * Caches not filling the conditions will be placed last, sorted by Geocode.
     * <br>
     * The default implementation returns {@code true} and can be overridden if needed in sub classes.
     *
     * @param cache the cache to be sorted
     * @return {@code true} if the cache holds the necessary data to be compared meaningfully
     */
    protected Boolean canCompare(final Geocache cache) {
        return true
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
    protected abstract Int compareCaches(Geocache cache1, Geocache cache2)

    /**
     * Sorts the given list of caches using this comparator. Respects implementations of {@link #beforeSort(List)} and{@link #afterSort(List)}
     */
    public Unit sort(final List<Geocache> list) {
        beforeSort(list)
        Collections.sort(list, this)
        afterSort(list)
    }

}
