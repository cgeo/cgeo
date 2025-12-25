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

import android.graphics.Canvas
import android.graphics.Point

/**
 * Defines the base functions of the provider-independent
 * Overlay implementations
 */
interface GeneralOverlay {

    Unit draw(Canvas canvas, MapViewImpl mapView, Boolean shadow)

    Unit drawOverlayBitmap(Canvas canvas, Point drawPosition,
                           MapProjectionImpl projection, Byte drawZoomLevel)

    OverlayImpl getOverlayImpl()
}
