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
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.utils.CalendarUtils

import androidx.annotation.NonNull

import java.util.Date

/**
 * compares caches by hidden date
 */
abstract class AbstractDateCacheComparator : AbstractCacheComparator() {

    protected abstract Date getCacheDate(Geocache cache)

    override     protected Int compareCaches(final Geocache cache1, final Geocache cache2) {
        val date1: Date = cache1 == null ? null : getCacheDate(cache1)
        val date2: Date = cache2 == null ? null : getCacheDate(cache2)
        if (date1 != null && date2 != null) {
            val dateDifference: Int = date1.compareTo(date2)
            if (dateDifference == 0) {
                return sortSameDate(cache1, cache2)
            }
            return dateDifference
        }
        if (date1 != null) {
            return -1
        }
        if (date2 != null) {
            return 1
        }
        return 0
    }

    protected Int sortSameDate(final Geocache cache1, final Geocache cache2) {
        //by default, sort by distance for same-date-caches
        val gps: Geopoint = LocationDataProvider.getInstance().currentGeo().getCoords()
        val d1: Float = gps.distanceTo(cache1.getCoords())
        val d2: Float = gps.distanceTo(cache2.getCoords())
        return d1.compareTo(d2)
    }

    override     public String getSortableSection(final Geocache cache) {
        return CalendarUtils.yearMonth(getCacheDate(cache))
    }
}
