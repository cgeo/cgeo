package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointConverter;
import cgeo.geocaching.maps.google.v2.GoogleMapObjects;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.utils.MapLineUtils;

import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

class GoogleMapsPositionLayer extends AbstractPositionLayer<LatLng> {
    private final GoogleMapObjects trackObjs;

    private static final GeopointConverter<LatLng> GP_CONVERTER = new GeopointConverter<>(
            gc -> new LatLng(gc.getLatitude(), gc.getLongitude()),
            ll -> new Geopoint(ll.latitude, ll.longitude)
    );

    GoogleMapsPositionLayer(final GoogleMap googleMap, final View root) {
        super(root, LatLng::new);
        trackObjs = new GoogleMapObjects(googleMap);
    }

    protected void destroyLayer(final GoogleMap map) {
        trackObjs.removeAll();
    }

    // ========================================================================
    // route / track handling

    @Override
    public void updateIndividualRoute(final IndividualRoute route) {
        super.updateIndividualRoute(route, GP_CONVERTER::toListList);
    }

    // ========================================================================
    // repaint methods

    @Override
    protected void repaintRouteAndTracks() {
        trackObjs.removeAll();
        repaintRouteAndTracksHelper((segment, color, width) -> trackObjs.addPolyline(new PolylineOptions()
                .addAll(segment)
                .color(color)
                .width(MapLineUtils.getWidthFromRaw(width, true))
                .zIndex(LayerHelper.ZINDEX_TRACK_ROUTE)
        ));
    }

}
