package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.maps.google.v2.GoogleMapObjects;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.utils.MapLineUtils;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

class GoogleMapsPositionLayer extends AbstractPositionLayer {

    public static final float ZINDEX_POSITION = 10;
    public static final float ZINDEX_POSITION_ACCURACY_CIRCLE = 3;
    public static final float ZINDEX_HISTORY = 2;

    private final GoogleMapObjects positionObjs;
    private final GoogleMapObjects historyObjs;

    GoogleMapsPositionLayer(final GoogleMap googleMap) {
        positionObjs = new GoogleMapObjects(googleMap);
        historyObjs = new GoogleMapObjects(googleMap);
    }

    @Override
    protected void repaintArrow() {
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
                .zIndex(ZINDEX_POSITION_ACCURACY_CIRCLE)
            );
        }

        // position and heading arrow
        positionObjs.addMarker(new MarkerOptions()
            .icon(BitmapDescriptorFactory.fromBitmap(positionAndHeadingArrow))
            .position(latLng)
            .rotation(currentHeading)
            .anchor(0.5f, 0.5f)
            .flat(true)
            .zIndex(ZINDEX_POSITION)
        );

    };

    @Override
    protected void repaintPosition() {
        // @todo
    };

    @Override
    protected void repaintHistory() {
        historyObjs.removeAll();
        repaintHistoryHelper(LatLng::new, (points) -> historyObjs.addPolyline(new PolylineOptions()
            .addAll(points)
            .color(MapLineUtils.getTrailColor())
            .width(MapLineUtils.getHistoryLineWidth())
            .zIndex(ZINDEX_HISTORY)
        ));
    };

    @Override
    protected void repaintRouteAndTracks() {
        // @todo
    };

}
