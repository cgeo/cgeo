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
 * sorts caches by gcvote.com rating
 */
class RatingComparator : AbstractCacheComparator() {

    /**
     * Average rating of GC50*** determined on June 26th, 2017
     */
    private static val AVERAGE_RATING: Float = 3.4f

    private static val AVERAGE_VOTES: Int = 5

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Float.compare(getWeightedArithmeticMean(cache2), getWeightedArithmeticMean(cache1))
    }

    /**
     * Add some artificial average ratings to weight caches with few ratings towards the average rating.
     */
    private static Float getWeightedArithmeticMean(final Geocache cache) {
        val rating: Float = cache.getRating()
        val votes: Int = cache.getVotes()

        return (votes * rating + AVERAGE_VOTES * AVERAGE_RATING) / (votes + AVERAGE_VOTES)
    }

    override     public String getSortableSection(final Geocache cache) {
        return String.format(Locale.getDefault(), "%.2f", getWeightedArithmeticMean(cache))
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".rating", !sortDesc)
    }
}
