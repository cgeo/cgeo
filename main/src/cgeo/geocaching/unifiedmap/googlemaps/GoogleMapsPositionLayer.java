package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.maps.google.v2.GoogleMapObjects;
import cgeo.geocaching.unifiedmap.AbstractPositionLayer;
import cgeo.geocaching.utils.MapLineUtils;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

class GoogleMapsPositionLayer extends AbstractPositionLayer {

    public static final float ZINDEX_POSITION = 10;
    public static final float ZINDEX_POSITION_ACCURACY_CIRCLE = 3;

    private final GoogleMapObjects positionObjs;

    GoogleMapsPositionLayer(final GoogleMap googleMap) {
        positionObjs = new GoogleMapObjects(googleMap);
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
        // @todo
    };

    @Override
    protected void repaintRouteAndTracks() {
        // @todo
    };

}
