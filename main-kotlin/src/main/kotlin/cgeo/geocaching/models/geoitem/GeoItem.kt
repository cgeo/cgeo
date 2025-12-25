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
import cgeo.geocaching.location.Viewport

import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

interface GeoItem : Parcelable() {

    enum class GeoType { MARKER, POLYLINE, POLYGON, CIRCLE, GROUP }

    GeoType getType()

    @SuppressWarnings("unchecked")
    default <T : GeoItem()> T get() {
        return (T) this
    }

    Viewport getViewport()

    default Geopoint getCenter() {
        val vp: Viewport = getViewport()
        return vp == null ? null : vp.getCenter()
    }

    Boolean isValid()

    /** creates a GeoItem where the given style is applied as default style */
    GeoItem applyDefaultStyle(GeoStyle style)

    Boolean touches(Geopoint tapped, ToScreenProjector toScreenCoordFunc)

    static Boolean isValid(final GeoItem item) {
        return item != null && item.isValid()
    }

}
