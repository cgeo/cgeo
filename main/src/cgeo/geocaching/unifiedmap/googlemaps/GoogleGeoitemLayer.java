package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.unifiedmap.AbstractGeoitemLayer;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;

import java.lang.ref.WeakReference;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

class GoogleGeoitemLayer extends AbstractGeoitemLayer<Marker> {

    WeakReference<GoogleMap> mapRef;

    GoogleGeoitemLayer(GoogleMap map) {
        this.mapRef = new WeakReference<>(map);
    }

    @Override
    protected Marker add(final Geocache cache) {
        GoogleMap map = mapRef.get();
        if (map == null) {
            return null;
        }

        Geopoint coords = cache.getCoords();
        CacheMarker cm = MapMarkerUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), cache, CacheListType.MAP);
        Marker item = map.addMarker(new MarkerOptions()
            .position(new LatLng(coords.getLatitude(), coords.getLongitude()))
            .title(cache.getGeocode())
            .anchor(0.5f, 1f)
            .icon(BitmapDescriptorFactory.fromBitmap(cm.getBitmap()))
        );

        Log.e("addGeoitem");
        synchronized (items) {
            items.put(cache.getGeocode(), item);
        }
        return item;
    }

    @Override
    protected void remove(final String geocode) {
        GoogleMap map = mapRef.get();
        if (map != null) {
            synchronized (items) {
                Marker item = items.get(geocode);
                if (item != null) {
                    item.remove();
                }
            }
        }
        super.remove(geocode);
    }

}
