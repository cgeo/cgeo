package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.utils.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

public class GoogleMapController {

    private GoogleMap googleMap = null;

    public void animateTo(final LatLng geoPoint) {
        if (googleMap == null) {
            return;
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(geoPoint), 200, null);
    }

    public void setZoom(final int mapzoom) {
        if (googleMap == null) {
            return;
        }
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(mapzoom));
    }

    public void zoomToSpan(final int latSpanE6, final int lonSpanE6) {
        if (googleMap == null) {
            return;
        }
        if (latSpanE6 != 0 && lonSpanE6 != 0) {
            // calculate zoomlevel
            final int distDegree = Math.max(latSpanE6, lonSpanE6);
            final int zoomLevel = (int) Math.floor(Math.log(360.0 * 1e6 / distDegree) / Math.log(2));
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel));
        }
    }

    public void setGoogleMap(final GoogleMap googleMap) {
        if (this.googleMap != null && this.googleMap != googleMap) {
            Log.w("googleMap already set in GoogleMapController, overriding with " + (googleMap == null ? "null" : "new instance of GoogleMap"));
        }
        this.googleMap = googleMap;
    }

}
