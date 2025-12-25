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

import java.util.concurrent.atomic.AtomicLong

/**
 * sorts caches by distance to given GPS position
 */
class GlobalGPSDistanceComparator : AbstractDistanceComparator() {

    private static val gpsPosVersion: AtomicLong = AtomicLong(0)
    public static val INSTANCE: GlobalGPSDistanceComparator = GlobalGPSDistanceComparator()

    public static Unit updateGlobalGps(final Geopoint gpsPosition) {
        if (gpsPosition != null) {
            INSTANCE.coords = gpsPosition
            gpsPosVersion.incrementAndGet()
        }
    }
}
