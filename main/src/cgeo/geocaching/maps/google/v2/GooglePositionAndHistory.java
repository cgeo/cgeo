package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.interfaces.PositionAndHistory;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.settings.Settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class GooglePositionAndHistory implements PositionAndHistory {

    public static final float ZINDEX_DIRECTION_LINE = 5;
    public static final float ZINDEX_POSITION = 10;
    public static final float ZINDEX_POSITION_ACCURACY_CIRCLE = 3;
    public static final float ZINDEX_HISTORY = 2;
    public static final float ZINDEX_HISTORY_SHADOW = 1;


    private Location coordinates;
    private float heading;
    private PositionHistory history = new PositionHistory();

    private static final int MAX_HISTORY_POINTS = 230; // TODO add alpha, make changeable in constructor?


    private GoogleMapObjects positionObjs;
    private GoogleMapObjects historyObjs;
    private GoogleMapView mapView;

    private static Bitmap locationIcon;


    public GooglePositionAndHistory(final GoogleMap googleMap, final GoogleMapView mapView) {
        positionObjs = new GoogleMapObjects(googleMap);
        historyObjs = new GoogleMapObjects(googleMap);
        this.mapView = mapView;
    }

    @Override
    public void setCoordinates(final Location coord) {
        final boolean coordChanged = coord == null ? coordinates != null : !coord.equals(coordinates);
        coordinates = coord;
        if (coordChanged) {
            history.rememberTrailPosition(coordinates);
            mapView.setCoordinates(coordinates);
        }
    }

    @Override
    public Location getCoordinates() {
        return coordinates;
    }

    @Override
    public void setHeading(final float heading) {
        if (this.heading != heading) {
            this.heading = heading;
        }
    }

    @Override
    public float getHeading() {
        return heading;
    }

    @Override
    public ArrayList<Location> getHistory() {
        return history.getHistory();
    }

    @Override
    public void setHistory(final ArrayList<Location> history) {
        if (history != this.history.getHistory()) {
            this.history.setHistory(history);
        }
    }

    @Override
    public void repaintRequired() {
        drawPosition();
        if (Settings.isMapTrail()) {
            drawHistory();
        }
    }


    private static Bitmap getLocationIcon() {
        if (locationIcon == null) {
            locationIcon = BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron);
        }
        return locationIcon;
    }

    private PolylineOptions getDirectionPolyline(final Geopoint from, final Geopoint to) {
        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        final PolylineOptions options = new PolylineOptions()
                .width(5f * metrics.density)
                .color(0x80EB391E)
                .zIndex(ZINDEX_DIRECTION_LINE)
                .add(new LatLng(from.getLatitude(), from.getLongitude()));

        final Geopoint[] routingPoints = Routing.getTrack(from, to);

        if (routingPoints.length > 1) {

            for (int i = 1; i < routingPoints.length; i++) {
                options.add(new LatLng(routingPoints[i].getLatitude(), routingPoints[i].getLongitude()));
            }

        }

        options.add(new LatLng(to.getLatitude(), to.getLongitude()));
        return options;
    }

    private synchronized void drawPosition() {
        positionObjs.removeAll();
        if (this.coordinates == null) {
            return;
        }

        final LatLng latLng = new LatLng(coordinates.getLatitude(), coordinates.getLongitude());

        // accuracy circle
        positionObjs.addCircle(new CircleOptions()
                .center(latLng)
                .strokeColor(0x66000000)
                .strokeWidth(3)
                .fillColor(0x08000000)
                .radius(coordinates.getAccuracy())
                .zIndex(ZINDEX_POSITION_ACCURACY_CIRCLE)
        );

        positionObjs.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(getLocationIcon()))
                .position(latLng)
                .rotation(heading)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .zIndex(ZINDEX_POSITION)
        );

        final Geopoint destCoords = mapView.getDestinationCoords();
        if (destCoords != null) {
            // draw direction line
            positionObjs.addPolyline(getDirectionPolyline(new Geopoint(coordinates), destCoords));
        }
    }


    private synchronized void drawHistory() {
        historyObjs.removeAll();
        final List<Location> history = getHistory();
        if (history.isEmpty()) {
            return;
        }

        final int size = history.size();

        final List<LatLng> points = new ArrayList<>(MAX_HISTORY_POINTS);

        for (int i = 1; i < size; i++) {
            final Location loc = history.get(i);
            points.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
        }

        if (coordinates != null) {
            points.add(new LatLng(coordinates.getLatitude(), coordinates.getLongitude()));
        }

        final float alpha = 1; // TODO

        // history line
        historyObjs.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(0xFFFFFF | ((int) (alpha * 0xff) << 24))
                .width(3)
                .zIndex(ZINDEX_HISTORY)
        );

        // history line shadow
        historyObjs.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(0x000000 | ((int) (alpha * 0x66) << 24))
                .width(7)
                .zIndex(ZINDEX_HISTORY_SHADOW)
        );

    }
}
