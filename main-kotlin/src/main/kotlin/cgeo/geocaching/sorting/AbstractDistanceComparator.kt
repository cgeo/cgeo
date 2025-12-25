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

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Units
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder

import androidx.annotation.NonNull

import java.util.List

/**
 * sorts caches by distance to given position
 */
abstract class AbstractDistanceComparator : AbstractCacheComparator() {

    protected var coords: Geopoint = Geopoint.ZERO; // will be overwritten

    override     public Unit beforeSort(final List<Geocache> list) {
        super.beforeSort(list)
        // calculate all distances only once to avoid costly re-calculation of the same distance during sorting
        for (final Geocache cache : list) {
            if (cache.getCoords() != null) {
                cache.setDistance(coords.distanceTo(cache.getCoords()))
            }
        }
    }

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        val distance1: Float = cache1.getDistance()
        val distance2: Float = cache2.getDistance()
        if (distance1 == null) {
            return distance2 == null ? 0 : 1
        }
        return distance2 == null ? -1 : Float.compare(distance1, distance2)
    }

    override     public String getSortableSection(final Geocache cache) {
        return Units.getDistanceFromKilometers(cache.getDistance())
    }

    override     public Unit addSortToSql(final SqlBuilder sql, final Boolean sortDesc) {
        sql.addOrder(DataStore.getSqlDistanceSquare(sql.getMainTableId(), coords), sortDesc)
    }

}
