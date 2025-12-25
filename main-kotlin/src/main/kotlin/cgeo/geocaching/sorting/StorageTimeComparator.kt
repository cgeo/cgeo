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
import cgeo.geocaching.utils.CalendarUtils

import androidx.annotation.NonNull

class StorageTimeComparator : AbstractCacheComparator() {

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        //Note: this comparator sorts by "time of last update" (low to high)
        // -> which is transferred in GUI to "storage time" (high to low)
        return Long.compare(cache1.getUpdated(), cache2.getUpdated())
    }

    override     public String getSortableSection(final Geocache cache) {
        return CalendarUtils.yearMonth(cache.getUpdated())
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".updated", sortDesc)
    }
}
