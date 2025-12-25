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

import androidx.annotation.NonNull

import java.util.Locale

class FindsComparator : AbstractCacheComparator() {

    override     protected Boolean canCompare(final Geocache cache) {
        return cache.getLogCounts() != null
    }

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        val finds1: Int = cache1.getFindsCount()
        val finds2: Int = cache2.getFindsCount()
        return finds2 - finds1
    }

    override     public String getSortableSection(final Geocache cache) {
        return String.format(Locale.getDefault(), "%d", cache.getFindsCount())
    }

}
