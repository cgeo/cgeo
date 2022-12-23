package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.maps.google.v2.GoogleMapObjects;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.utils.MapLineUtils;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_GOOGLE;

import android.location.Location;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

class GoogleMapsPositionLayer extends AbstractPositionLayer<LatLng> {

    private final GoogleMapObjects directionObjs;
    private final GoogleMapObjects positionObjs;
    private final GoogleMapObjects trackObjs;
    private final GoogleMapObjects historyObjs;

    GoogleMapsPositionLayer(final GoogleMap googleMap, final View root) {
        super(root, LatLng::new);
        directionObjs = new GoogleMapObjects(googleMap);
        positionObjs = new GoogleMapObjects(googleMap);
        trackObjs = new GoogleMapObjects(googleMap);
        historyObjs = new GoogleMapObjects(googleMap);
    }

    protected void destroyLayer(final GoogleMap map) {
        directionObjs.removeAll();
        positionObjs.removeAll();
        trackObjs.removeAll();
        historyObjs.removeAll();
    }

    public void setCurrentPositionAndHeading(final Location location, final float heading) {
        setCurrentPositionAndHeadingHelper(location, heading, (directionLine) -> {
            directionObjs.removeAll();
            directionObjs.addPolyline(new PolylineOptions()
                    .addAll(directionLine)
                    .color(MapLineUtils.getDirectionColor())
                    .width(MapLineUtils.getDirectionLineWidth(true))
                    .zIndex(LayerHelper.ZINDEX_DIRECTION_LINE)
            );
        }, MAP_GOOGLE);
    }

    // ========================================================================
    // route / track handling

    @Override
    public void updateIndividualRoute(final Route route) {
        super.updateIndividualRoute(route, Route::getAllPointsLatLng);
    }

    @Override
    public void updateTrack(final String key, final Route track) {
        super.updateTrack(key, track, Route::getAllPointsLatLng);
    }

    // ========================================================================
    // repaint methods

    @Override
    protected void repaintPosition() {
        super.repaintPosition();
        positionObjs.removeAll();
        if (currentLocation == null) {
            return;
        }

        final LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        // accuracy circle
        final float accuracy = currentLocation.getAccuracy();
        if (accuracy > 0.001f) {
            positionObjs.addCircle(new CircleOptions()
                    .center(latLng)
                    .strokeColor(MapLineUtils.getAccuracyCircleColor())
                    .strokeWidth(3)
                    .fillColor(MapLineUtils.getAccuracyCircleFillColor())
                    .radius(accuracy)
                    .zIndex(LayerHelper.ZINDEX_POSITION_ACCURACY_CIRCLE)
            );
        }

        // position and heading arrow
        positionObjs.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(positionAndHeadingArrow))
                .position(latLng)
                .rotation(currentHeading)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .zIndex(LayerHelper.ZINDEX_POSITION)
        );

    }

    @Override
    protected void repaintHistory() {
        historyObjs.removeAll();
        repaintHistoryHelper((points) -> historyObjs.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(MapLineUtils.getTrailColor())
                .width(MapLineUtils.getHistoryLineWidth(true))
                .zIndex(LayerHelper.ZINDEX_HISTORY)
        ));
    }

    @Override
    protected void repaintRouteAndTracks() {
        trackObjs.removeAll();
        repaintRouteAndTracksHelper((segment, isTrack) -> trackObjs.addPolyline(new PolylineOptions()
                .addAll(segment)
                .color(isTrack ? MapLineUtils.getTrackColor() : MapLineUtils.getRouteColor())
                .width(isTrack ? MapLineUtils.getTrackLineWidth(true) : MapLineUtils.getRouteLineWidth(true))
                .zIndex(LayerHelper.ZINDEX_TRACK_ROUTE)
        ));
    }

}
