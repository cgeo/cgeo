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
 * sorts caches by popularity ratio (favorites per find in %).
 */
class PopularityRatioComparator : AbstractCacheComparator() {

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        val finds1: Int = cache1.getFindsCount()
        val finds2: Int = cache2.getFindsCount()

        Float ratio1 = 0.0f
        if (finds1 != 0) {
            ratio1 = (Float) cache1.getFavoritePoints() / (Float) finds1
        }
        Float ratio2 = 0.0f
        if (finds2 != 0) {
            ratio2 = (Float) cache2.getFavoritePoints() / (Float) finds2
        }

        return Float.compare(ratio2, ratio1)
    }

    override     public String getSortableSection(final Geocache cache) {
        val finds: Int = cache.getFindsCount()
        return 0 == finds ? "--" : String.format(Locale.getDefault(), "%.2f", ((Float) cache.getFavoritePoints()) / finds)
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        //also sort by favourite count, should resemble ratio close enough
        sql.addOrder(sql.getMainTableId() + ".favourite_cnt", !sortDesc)
    }
}
