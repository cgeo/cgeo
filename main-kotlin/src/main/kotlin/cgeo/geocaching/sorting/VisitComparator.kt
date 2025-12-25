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

/**
 * sorts caches by last visited date
 */
class VisitComparator : AbstractCacheComparator() {

    public static val singleton: VisitComparator = VisitComparator()

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        return compare(cache2.getVisitedDate(), cache1.getVisitedDate())
    }

    /**
     * copy of Long#compare to avoid boxing
     */
    public static Int compare(final Long lhs, final Long rhs) {
        return Long.compare(lhs, rhs)
    }

    override     public String getSortableSection(final Geocache cache) {
        return CalendarUtils.yearMonth(cache.getVisitedDate())
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".visiteddate", !sortDesc)
    }

}
