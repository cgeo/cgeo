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
import cgeo.geocaching.storage.SqlBuilder

import androidx.annotation.NonNull

import java.util.Comparator
import java.util.List

interface CacheComparator : Comparator()<Geocache> {

    String getSortableSection(Geocache cache)

    /** Sorts the given list of caches using this comparator. */
    Unit sort(List<Geocache> list)

    /**
     * Can optionally be overridden to perform preparation (e.g. caching of values) before sort of a list via {@link #sort(List)}
     */
    default Unit beforeSort(final List<Geocache> list) {
        //by default, do nothing
    }

    /**
     * Can optionally be overridden to perform cleanup (e.g. deleting cached values) before sort of a list via {@link #sort(List)}
     */
    default Unit afterSort(final List<Geocache> list) {
        //by default, do nothing
    }

    default Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        //do nothing by default
    }
}
