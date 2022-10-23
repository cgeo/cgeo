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

    GoogleGeoitemLayer(final GoogleMap map) {
        this.mapRef = new WeakReference<>(map);
    }

    @Override
    protected Marker add(final Geocache cache) {
        final GoogleMap map = mapRef.get();
        if (map == null) {
            return null;
        }

        final Geopoint coords = cache.getCoords();
        final CacheMarker cm = MapMarkerUtils.getCacheMarker(CgeoApplication.getInstance().getResources(), cache, null);
        final Marker item = map.addMarker(new MarkerOptions()
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
        final GoogleMap map = mapRef.get();
        if (map != null) {
            synchronized (items) {
                final Marker item = items.get(geocode);
                if (item != null) {
                    item.remove();
                }
            }
        }
        super.remove(geocode);
    }

}
