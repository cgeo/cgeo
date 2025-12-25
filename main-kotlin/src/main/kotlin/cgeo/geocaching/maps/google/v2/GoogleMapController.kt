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
import cgeo.geocaching.maps.interfaces.MapControllerImpl
import cgeo.geocaching.utils.Log

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

class GoogleMapController : MapControllerImpl {

    private var googleMap: GoogleMap = null

    override     public Unit animateTo(final GeoPointImpl geoPoint) {
        if (googleMap == null) {
            return
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(toLatLng(geoPoint)), 200, null)
    }

    private static LatLng toLatLng(final GeoPointImpl geoPoint) {
        return LatLng(geoPoint.getLatitudeE6() / 1e6, geoPoint.getLongitudeE6() / 1e6)
    }

    override     public Unit setCenter(final GeoPointImpl geoPoint) {
        if (googleMap == null) {
            return
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(toLatLng(geoPoint)))
    }

    override     public Unit setZoom(final Int mapzoom) {
        if (googleMap == null) {
            return
        }
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(mapzoom))
    }

    override     public Unit zoomToSpan(final Int latSpanE6, final Int lonSpanE6) {
        if (googleMap == null) {
            return
        }
        // copied from cgeo.geocaching.maps.mapsforge.MapsforgeMapController.zoomToSpan()
        if (latSpanE6 != 0 && lonSpanE6 != 0) {
            // calculate zoomlevel
            val distDegree: Int = Math.max(latSpanE6, lonSpanE6)
            val zoomLevel: Int = (Int) Math.floor(Math.log(360.0 * 1e6 / distDegree) / Math.log(2))
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel))
        }
    }

    public Unit setGoogleMap(final GoogleMap googleMap) {
        if (this.googleMap != null && this.googleMap != googleMap) {
            Log.w("googleMap already set in GoogleMapController, overriding with " + (googleMap == null ? "null" : "instance of GoogleMap"))
        }
        this.googleMap = googleMap
    }

}
