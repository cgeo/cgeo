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

import java.util.Collections
import java.util.List

/**
 * comparator which inverses the sort order of the given other comparator
 */
class InverseComparator : CacheComparator {

    private final CacheComparator originalComparator

    public InverseComparator(final CacheComparator comparator) {
        this.originalComparator = comparator
    }

    override     public Int compare(final Geocache lhs, final Geocache rhs) {
        return originalComparator.compare(rhs, lhs)
    }

    override     public String getSortableSection(final Geocache cache) {
        return originalComparator.getSortableSection(cache)
    }

    override     public Unit sort(final List<Geocache> list) {
        originalComparator.beforeSort(list)
        Collections.sort(list, this)
        originalComparator.afterSort(list)
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        originalComparator.addSortToSql(sql, !sortDesc)
    }
}
