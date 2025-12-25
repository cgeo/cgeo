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
 * sorts caches by geo code, therefore effectively sorting by cache age
 */
class GeocodeComparator : AbstractCacheComparator() {

    override     protected Boolean canCompare(final Geocache cache) {
        // This will fall back to geocode comparisons.
        return false
    }

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        throw IllegalStateException("should never be called")
    }

    override     public String getSortableSection(final Geocache cache) {
        return cache.getGeocode()
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".geocode", sortDesc)
    }

}
