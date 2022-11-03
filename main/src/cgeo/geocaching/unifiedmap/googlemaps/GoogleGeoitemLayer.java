package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.unifiedmap.AbstractGeoitemLayer;
import cgeo.geocaching.unifiedmap.LayerHelper;

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
    protected void add(final IWaypoint item) {
        final GoogleMap map = mapRef.get();
        if (map == null) {
            return;
        }
        super.add(item);
    }

    @Override
    protected GeoItemCache<Marker> addInternal(final IWaypoint item, final boolean isCache, final Geopoint coords, final RouteItem routeItem, final CacheMarker cm) {
        final GoogleMap map = mapRef.get();
        final Marker marker = map.addMarker(new MarkerOptions()
            .position(new LatLng(coords.getLatitude(), coords.getLongitude()))
            .anchor(0.5f, 1f)
            .icon(BitmapDescriptorFactory.fromBitmap(cm.getBitmap()))
            .zIndex(isCache ? LayerHelper.ZINDEX_GEOCACHE : LayerHelper.ZINDEX_WAYPOINT)
        );
        return new GeoItemCache<>(routeItem, marker);
    }

    @Override
    protected void remove(final String geocode) {
        final GoogleMap map = mapRef.get();
        if (map != null) {
            synchronized (items) {
                final GeoItemCache<Marker> item = items.get(geocode);
                if (item != null) {
                    item.mapItem.remove();
                }
            }
        }
        super.remove(geocode);
    }

}
