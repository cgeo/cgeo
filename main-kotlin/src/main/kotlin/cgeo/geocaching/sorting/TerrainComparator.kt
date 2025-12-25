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
 * sorts caches by terrain rating
 */
class TerrainComparator : AbstractCacheComparator() {

    override     protected Boolean canCompare(final Geocache cache) {
        return cache.getTerrain() != 0.0
    }

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Float.compare(cache1.getTerrain(), cache2.getTerrain())
    }

    override     public String getSortableSection(final Geocache cache) {
        return String.format(Locale.getDefault(), "%.1f", cache.getTerrain())
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".terrain", sortDesc)
    }

}
