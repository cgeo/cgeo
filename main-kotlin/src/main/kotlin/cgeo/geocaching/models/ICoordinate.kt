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

package cgeo.geocaching.models

import cgeo.geocaching.location.Geopoint

interface ICoordinate {

    Geopoint getCoords()

    /** instances of ICoordinates may optionally provide elevation information */
    default Float getElevation() {
        return 0f
    }

}
