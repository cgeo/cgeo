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

import androidx.annotation.NonNull

/**
 * sorts caches by distance to given target position
 */
class TargetDistanceComparator : AbstractDistanceComparator() {

    public TargetDistanceComparator(final Geopoint coords) {
        this.coords = coords
    }
}
