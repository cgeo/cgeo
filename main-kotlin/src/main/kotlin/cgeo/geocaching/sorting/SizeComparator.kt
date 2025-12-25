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

/**
 * sorts caches by size
 */
class SizeComparator : AbstractCacheComparator() {

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        return cache2.getSize().comparable - cache1.getSize().comparable
    }

    override     public String getSortableSection(final Geocache cache) {
        return cache.getSize().toString()
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".size", sortDesc)
    }
}
