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
import android.graphics.drawable.Drawable

/**
 * Defines the common functions to access the provider-specific
 * ItemizedOverlay implementation
 */
interface ItemizedOverlayImpl : OverlayImpl() {

    Unit superPopulate()

    Unit superSetLastFocusedItemIndex(Int i)

    Drawable superBoundCenterBottom(Drawable marker)

    Boolean superOnTap(Int index)

    Unit superDraw(Canvas canvas, MapViewImpl mapView, Boolean shadow)

    Unit superDrawOverlayBitmap(Canvas canvas, Point drawPosition, MapProjectionImpl projection,
                                Byte drawZoomLevel)

}
