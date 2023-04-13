package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.maps.google.v2.GoogleMapObjects;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;

import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

class GoogleMapsPositionLayer extends AbstractPositionLayer<LatLng> {
    private final GoogleMapObjects trackObjs;

    GoogleMapsPositionLayer(final GoogleMap googleMap, final View root) {
        super(root, LatLng::new);
        trackObjs = new GoogleMapObjects(googleMap);
    }

    protected void destroyLayer(final GoogleMap map) {
        trackObjs.removeAll();
    }

}
