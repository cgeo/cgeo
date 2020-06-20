package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.interfaces.PositionAndHistory;
import cgeo.geocaching.maps.routing.Route;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.MapLineUtils;
import cgeo.geocaching.utils.TrackUtils;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

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

public class GooglePositionAndHistory implements PositionAndHistory, Route.RouteUpdater, TrackUtils.TrackUpdaterSingle {

    public static final float ZINDEX_DIRECTION_LINE = 5;
    public static final float ZINDEX_POSITION = 10;
    public static final float ZINDEX_TRACK = 6;
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
    private int mapRotation = MAPROTATION_MANUAL;

    private static final int MAX_HISTORY_POINTS = 230; // TODO add alpha, make changeable in constructor?

    private WeakReference<GoogleMap> mapRef = null;
    private GoogleMapObjects positionObjs;
    private GoogleMapObjects historyObjs;
    private GoogleMapObjects routeObjs;
    private GoogleMapObjects trackObjs;
    private GoogleMapView mapView;
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
        updateMapRotation();
    }

    @Override
    public void setCoordinates(final Location coord) {
        final boolean coordChanged = coord == null ? coordinates != null : !coord.equals(coordinates);
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
    public void updateRoute(final ArrayList<Geopoint> route, final float distance) {
        this.route = new ArrayList<LatLng>();
        for (int i = 0; i < route.size(); i++) {
            this.route.add(new LatLng(route.get(i).getLatitude(), route.get(i).getLongitude()));
        }

        if (postRouteDistance != null) {
            postRouteDistance.postRealDistance(distance);
        }
        repaintRequired();
    }

    @Override
    public void updateTrack(final TrackUtils.Track track) {
        this.track = new ArrayList<>();
        final ArrayList<Geopoint> temp = track.getTrack();
        for (int i = 0; i < track.getSize(); i++) {
            this.track.add(new LatLng(temp.get(i).getLatitude(), temp.get(i).getLongitude()));
        }
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
        if (destCoords != null && Settings.isMapDirection()) {
            // draw direction line
            positionObjs.addPolyline(getDirectionPolyline(new Geopoint(coordinates), destCoords));
        }
    }


    private synchronized void drawHistory() {
        if (null == coordinates) {
            return;
        }
        historyObjs.removeAll();
        if (!Settings.isMapTrail()) {

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
                            .width(MapLineUtils.getHistoryLineInsetWidth())
                            .zIndex(ZINDEX_HISTORY)
                    );

                    // history line shadow
                    historyObjs.addPolyline(new PolylineOptions()
                            .addAll(points)
                            .color(MapLineUtils.getTrailColor())
                            .width(MapLineUtils.getHistoryLineShadowWidth())
                            .zIndex(ZINDEX_HISTORY_SHADOW)
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
