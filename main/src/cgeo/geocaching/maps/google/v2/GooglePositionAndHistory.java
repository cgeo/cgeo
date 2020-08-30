package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.interfaces.PositionAndHistory;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.models.ManualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.MapLineUtils;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class GooglePositionAndHistory implements PositionAndHistory, Route.UpdateRoute, ManualRoute.UpdateManualRoute {

    public static final float ZINDEX_DIRECTION_LINE = 5;
    public static final float ZINDEX_POSITION = 10;
    public static final float ZINDEX_TRACK = 6;
    public static final float ZINDEX_ROUTE = 5;
    public static final float ZINDEX_POSITION_ACCURACY_CIRCLE = 3;
    public static final float ZINDEX_HISTORY = 2;

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static final float LINE_MAXIMUM_DISTANCE_METERS = 10000;

    private Location coordinates;
    private float heading;
    private final PositionHistory history = new PositionHistory();

    // settings for map auto rotation
    private Location lastBearingCoordinates = null;
    private int mapRotation = MAPROTATION_MANUAL;

    private static final int MAX_HISTORY_POINTS = 230; // TODO add alpha, make changeable in constructor?

    private WeakReference<GoogleMap> mapRef = null;
    private final GoogleMapObjects positionObjs;
    private final GoogleMapObjects historyObjs;
    private final GoogleMapObjects routeObjs;
    private final GoogleMapObjects trackObjs;
    private final GoogleMapView mapView;
    private GoogleMapView.PostRealDistance postRealDistance = null;
    private GoogleMapView.PostRealDistance postRouteDistance = null;

    private static Bitmap locationIcon;

    private ArrayList<LatLng> route = null;
    private Viewport lastViewport = null;
    private ArrayList<LatLng> track = null;

    public GooglePositionAndHistory(final GoogleMap googleMap, final GoogleMapView mapView, final GoogleMapView.PostRealDistance postRealDistance, final GoogleMapView.PostRealDistance postRouteDistance) {
        this.mapRef = new WeakReference<>(googleMap);
        positionObjs = new GoogleMapObjects(googleMap);
        historyObjs = new GoogleMapObjects(googleMap);
        routeObjs = new GoogleMapObjects(googleMap);
        trackObjs = new GoogleMapObjects(googleMap);
        this.mapView = mapView;
        this.postRealDistance = postRealDistance;
        this.postRouteDistance = postRouteDistance;
        mapView.setDistanceDrawer(null);
        updateMapRotation();
    }

    @Override
    public void setCoordinates(final Location coord) {
        final boolean coordChanged = !Objects.equals(coord, coordinates);
        coordinates = coord;
        if (coordChanged) {
            history.rememberTrailPosition(coordinates);
            mapView.setCoordinates(coordinates);

            if (mapRotation == MAPROTATION_AUTO) {
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

    public void updateMapRotation() {
        this.mapRotation = Settings.getMapRotation();
        final GoogleMap map = mapRef.get();
        if (null != map) {
            map.getUiSettings().setRotateGesturesEnabled(mapRotation == MAPROTATION_MANUAL);
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
    public void updateManualRoute(final Route route) {
        this.route = route.getAllPointsLatLng();
        if (postRouteDistance != null) {
            postRouteDistance.postRealDistance(route.getDistance());
        }
        repaintRequired();
    }

    @Override
    public void updateRoute(final Route track) {
        this.track = null == track ? null : track.getAllPointsLatLng();
        repaintRequired();
    }

    @Override
    public void repaintRequired() {
        drawPosition();
        drawHistory();
        drawRoute();
        drawViewport(lastViewport);
        drawTrack();
    }


    private static Bitmap getLocationIcon() {
        if (locationIcon == null) {
            locationIcon = BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron);
        }
        return locationIcon;
    }

    private PolylineOptions getDirectionPolyline(final Geopoint from, final Geopoint to) {
        final PolylineOptions options = new PolylineOptions()
                .width(MapLineUtils.getDirectionLineWidth())
                .color(MapLineUtils.getDirectionColor())
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
        if (destCoords != null) {
            final Geopoint currentCoords = new Geopoint(coordinates);
            if (Settings.isMapDirection()) {
                // draw direction line
                positionObjs.addPolyline(getDirectionPolyline(currentCoords, destCoords));
            } else if (null != postRealDistance) {
                postRealDistance.postRealDistance(destCoords.distanceTo(currentCoords));
            }
        }
    }

    private synchronized void drawHistory() {
        if (null == coordinates) {
            return;
        }
        historyObjs.removeAll();
        if (Settings.isMapTrail()) {
            final ArrayList<Location> paintHistory = new ArrayList<>(getHistory());
            final int size = paintHistory.size();
            if (size < 2) {
                return;
            }
            // always add current position to drawn history to have a closed connection, even if it's not yet recorded
            paintHistory.add(coordinates);

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
                            .color(MapLineUtils.getTrailColor())
                            .width(MapLineUtils.getHistoryLineWidth())
                            .zIndex(ZINDEX_HISTORY)
                    );
                }
            }
        }
    }

    private synchronized void drawRoute() {
        routeObjs.removeAll();
        if (route != null && route.size() > 1) {
            routeObjs.addPolyline(new PolylineOptions()
                    .addAll(route)
                    .color(MapLineUtils.getRouteColor())
                    .width(MapLineUtils.getRouteLineWidth())
                    .zIndex(ZINDEX_ROUTE)
            );
        }
    }

    public synchronized void drawViewport(final Viewport viewport) {
        if (null == viewport) {
            return;
        }
        final PolylineOptions options = new PolylineOptions()
                .width(MapLineUtils.getDebugLineWidth())
                .color(0x80EB391E)
                .zIndex(ZINDEX_DIRECTION_LINE)
                .add(new LatLng(viewport.getLatitudeMin(), viewport.getLongitudeMin()))
                .add(new LatLng(viewport.getLatitudeMin(), viewport.getLongitudeMax()))
                .add(new LatLng(viewport.getLatitudeMax(), viewport.getLongitudeMax()))
                .add(new LatLng(viewport.getLatitudeMax(), viewport.getLongitudeMin()))
                .add(new LatLng(viewport.getLatitudeMin(), viewport.getLongitudeMin()));

        positionObjs.addPolyline(options);
        lastViewport = viewport;
    }

    private synchronized void drawTrack() {
        trackObjs.removeAll();
        if (track != null && track.size() > 1 && !Settings.isHideTrack()) {
            trackObjs.addPolyline(new PolylineOptions()
                .addAll(track)
                .color(MapLineUtils.getTrackColor())
                .width(MapLineUtils.getTrackLineWidth())
                .zIndex(ZINDEX_TRACK)
            );
        }
    }

}
