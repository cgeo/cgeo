package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.interfaces.PositionAndHistory;
import cgeo.geocaching.maps.routing.Route;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AngleUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class GooglePositionAndHistory implements PositionAndHistory, Route.RouteUpdater {

    public static final float ZINDEX_DIRECTION_LINE = 5;
    public static final float ZINDEX_POSITION = 10;
    public static final float ZINDEX_ROUTE = 5;
    public static final float ZINDEX_POSITION_ACCURACY_CIRCLE = 3;
    public static final float ZINDEX_HISTORY = 2;
    public static final float ZINDEX_HISTORY_SHADOW = 1;

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static final float LINE_MAXIMUM_DISTANCE_METERS = 10000;

    private Location coordinates;
    private float heading;
    private PositionHistory history = new PositionHistory();

    // settings for map auto rotation
    private Location lastBearingCoordinates = null;
    private boolean isMapAutoRotationDisabled = false;

    private static final int MAX_HISTORY_POINTS = 230; // TODO add alpha, make changeable in constructor?

    private WeakReference<GoogleMap> mapRef = null;
    private GoogleMapObjects positionObjs;
    private GoogleMapObjects historyObjs;
    private GoogleMapObjects routeObjs;
    private GoogleMapView mapView;
    private final int trailColor;
    private GoogleMapView.PostRealDistance postRealDistance = null;
    private GoogleMapView.PostRealDistance postRouteDistance = null;

    private static Bitmap locationIcon;

    private ArrayList<LatLng> route = null;
    private float distance = 0.0f;


    public GooglePositionAndHistory(final GoogleMap googleMap, final GoogleMapView mapView, final GoogleMapView.PostRealDistance postRealDistance, final GoogleMapView.PostRealDistance postRouteDistance) {
        this.mapRef = new WeakReference<>(googleMap);
        positionObjs = new GoogleMapObjects(googleMap);
        historyObjs = new GoogleMapObjects(googleMap);
        routeObjs = new GoogleMapObjects(googleMap);
        this.mapView = mapView;
        trailColor = Settings.getTrailColor();
        this.postRealDistance = postRealDistance;
        this.postRouteDistance = postRouteDistance;
        updateMapAutoRotation();
    }

    @Override
    public void setCoordinates(final Location coord) {
        final boolean coordChanged = coord == null ? coordinates != null : !coord.equals(coordinates);
        coordinates = coord;
        if (coordChanged) {
            history.rememberTrailPosition(coordinates);
            mapView.setCoordinates(coordinates);

            if (!isMapAutoRotationDisabled) {
                if (null != lastBearingCoordinates) {
                    final GoogleMap map = mapRef.get();
                    if (null != map) {
                        final CameraPosition currentCameraPosition = map.getCameraPosition();
                        final float bearing = AngleUtils.normalize(lastBearingCoordinates.bearingTo(coordinates));
                        final float bearingDiff = Math.abs(AngleUtils.difference(bearing, currentCameraPosition.bearing));
                        if (bearingDiff > 15.0f) {
                            lastBearingCoordinates = coordinates;
                            // adjust bearing of map, keep position and zoom level
                            final CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(currentCameraPosition.target)
                                    .zoom(currentCameraPosition.zoom)
                                    .bearing(bearing)
                                    .tilt(0)
                                    .build();
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        }
                    } else {
                        lastBearingCoordinates = null;
                    }
                } else {
                    lastBearingCoordinates = coordinates;
                }
            }
        }
    }

    public void updateMapAutoRotation() {
        this.isMapAutoRotationDisabled = Settings.isMapAutoRotationDisabled();
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
    public void updateRoute(final ArrayList<Geopoint> route, final float distance) {
        this.route = new ArrayList<LatLng>();
        for (int i = 0; i < route.size(); i++) {
            this.route.add(new LatLng(route.get(i).getLatitude(), route.get(i).getLongitude()));
        }
        this.distance = distance;

        if (postRouteDistance != null) {
            postRouteDistance.postRealDistance(distance);
        }
    }


    @Override
    public void repaintRequired() {
        drawPosition();
        if (Settings.isMapTrail()) {
            drawHistory();
        }
        drawRoute();
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
            // calculate polyline to draw
            for (int i = 1; i < routingPoints.length; i++) {
                options.add(new LatLng(routingPoints[i].getLatitude(), routingPoints[i].getLongitude()));
            }

            // calculate distance
            if (null != postRealDistance) {
                float distance = 0.0f;
                for (int i = 1; i < routingPoints.length; i++) {
                    distance += routingPoints[i - 1].distanceTo(routingPoints[i]);
                }
                postRealDistance.postRealDistance(distance);
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
        if (destCoords != null && Settings.isMapDirection()) {
            // draw direction line
            positionObjs.addPolyline(getDirectionPolyline(new Geopoint(coordinates), destCoords));
        }
    }


    private synchronized void drawHistory() {
        historyObjs.removeAll();

        // always add current position to drawn history to have a closed connection
        final ArrayList<Location> paintHistory = getHistory();
        paintHistory.add(coordinates);

        final int size = paintHistory.size();
        if (size == 1) {
            return;
        }

        Location prev = paintHistory.get(0);
        int current = 1;
        while (current < size) {
            final List<LatLng> points = new ArrayList<>(MAX_HISTORY_POINTS);
            points.add(new LatLng(prev.getLatitude(), prev.getLongitude()));

            boolean paint = false;
            while (!paint && current < size) {
                final Location now = paintHistory.get(current);
                current++;
                if (now.distanceTo(prev) < LINE_MAXIMUM_DISTANCE_METERS) {
                    points.add(new LatLng(now.getLatitude(), now.getLongitude()));
                } else {
                    paint = true;
                }
                prev = now;
            }
            if (points.size() > 1) {
                // history line
                historyObjs.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(0xFFFFFFFF)
                    .width(3)
                    .zIndex(ZINDEX_HISTORY)
                );

                // history line shadow
                historyObjs.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .color(trailColor)
                    .width(7)
                    .zIndex(ZINDEX_HISTORY_SHADOW)
                );
            }
        }
    }

    private synchronized void drawRoute() {
        routeObjs.removeAll();
        if (route != null && route.size() > 1) {
            routeObjs.addPolyline(new PolylineOptions()
                    .addAll(route)
                    .color(0xFF0000FF)
                    .width(7)
                    .zIndex(ZINDEX_ROUTE)
            );
        }
    }

}
