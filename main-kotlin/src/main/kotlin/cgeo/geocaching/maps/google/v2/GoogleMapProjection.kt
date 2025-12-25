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

package cgeo.geocaching.maps.google.v2

import cgeo.geocaching.maps.interfaces.GeoPointImpl
import cgeo.geocaching.maps.interfaces.MapProjectionImpl

import android.graphics.Point

import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng

class GoogleMapProjection : MapProjectionImpl {

    private final Projection projection

    public GoogleMapProjection(final Projection projectionIn) {
        projection = projectionIn
    }

    override     public Unit toPixels(final GeoPointImpl leftGeo, final Point left) {
        val p: Point = projection.toScreenLocation(LatLng(leftGeo.getLatitudeE6() / 1e6, leftGeo.getLongitudeE6() / 1e6))
        left.x = p.x
        left.y = p.y
    }

    override     public Object getImpl() {
        return projection
    }

}
