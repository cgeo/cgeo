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

import java.util.Locale

/**
 * sorts caches by difficulty
 */
class DifficultyComparator : AbstractCacheComparator() {

    override     protected Boolean canCompare(final Geocache cache) {
        return cache.getDifficulty() != 0.0
    }

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Float.compare(cache1.getDifficulty(), cache2.getDifficulty())
    }

    override     public String getSortableSection(final Geocache cache) {
        return String.format(Locale.getDefault(), "%.1f", cache.getDifficulty())
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".difficulty", sortDesc)
    }

}
