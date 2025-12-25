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

import cgeo.geocaching.models.Geocache

import java.util.Date

/**compares caches by hidden date */
class LastFoundComparator : AbstractDateCacheComparator() {

    public static val INSTANCE_INVERSE: CacheComparator = InverseComparator(LastFoundComparator())

    protected Date getCacheDate(final Geocache cache) {
        return cache.getLastFound()
    }

}
