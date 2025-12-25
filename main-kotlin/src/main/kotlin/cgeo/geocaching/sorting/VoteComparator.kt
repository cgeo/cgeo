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
 * sorts caches by the users own voting (if available at all)
 */
class VoteComparator : AbstractCacheComparator() {

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        // if there is no vote available, put that cache at the end of the list
        return Float.compare(cache2.getMyVote(), cache1.getMyVote())
    }

    override     public String getSortableSection(final Geocache cache) {
        return String.format(Locale.getDefault(), "%.2f", cache.getMyVote())
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".myvote", !sortDesc)
    }
}
