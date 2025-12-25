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

package cgeo.geocaching.models.geoitem

import cgeo.geocaching.location.Geopoint

/** Projects lat/lon-coordinates to screen coordinates for a concrete map */
interface ToScreenProjector {

    /**
     * for a given geopoint, returns an Int array of size 2 (x, y) containing the
     * screen coordinate for the given lat/lon for a currently displayed map. Any visualization
     * details of the map (e.g. rotating, tilting, zooming, ...) should be accomodated for by the
     * implementor.
     */
    Int[] project(Geopoint t)
}
