package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapControllerImpl;
import cgeo.geocaching.utils.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

public class GoogleMapController implements MapControllerImpl {

    private GoogleMap googleMap = null;

    @Override
    public void animateTo(final GeoPointImpl geoPoint) {
        if (googleMap == null) {
            return;
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(toLatLng(geoPoint)), 200, null);
    }

    private static LatLng toLatLng(final GeoPointImpl geoPoint) {
        return new LatLng(geoPoint.getLatitudeE6() / 1e6, geoPoint.getLongitudeE6() / 1e6);
    }

    @Override
    public void setCenter(final GeoPointImpl geoPoint) {
        if (googleMap == null) {
            return;
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(toLatLng(geoPoint)));
    }

    @Override
    public void setZoom(final int mapzoom) {
        if (googleMap == null) {
            return;
        }
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(mapzoom));
    }

    @Override
    public void zoomToSpan(final int latSpanE6, final int lonSpanE6) {
        if (googleMap == null) {
            return;
        }
        // copied from cgeo.geocaching.maps.mapsforge.MapsforgeMapController.zoomToSpan()
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
