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

package cgeo.geocaching.maps.interfaces

import android.graphics.Point

/**
 * Defines common functions of the provider-specific
 * MapProjection implementations
 */
interface MapProjectionImpl {

    Object getImpl()

    Unit toPixels(GeoPointImpl leftGeo, Point left)

}
