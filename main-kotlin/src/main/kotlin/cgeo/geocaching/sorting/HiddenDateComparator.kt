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

import java.util.Date

/**compares caches by hidden date */
class HiddenDateComparator : AbstractDateCacheComparator() {

    public static val INSTANCE: HiddenDateComparator = HiddenDateComparator()

    protected Date getCacheDate(final Geocache cache) {
        return cache.getHiddenDate()
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".hidden", sortDesc)
    }

}
