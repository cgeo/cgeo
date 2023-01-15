package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.GeoObject;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.IGeoDataProvider;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.maps.interfaces.PositionAndHistory;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.sensors.GeoDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapLineUtils;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;

import android.location.Location;

import androidx.core.content.res.ResourcesCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class GooglePositionAndHistory implements PositionAndHistory, Tracks.UpdateTrack, IndividualRoute.UpdateIndividualRoute {

    public static final float ZINDEX_DIRECTION_LINE = 5;
    public static final float ZINDEX_POSITION = 10;
    public static final float ZINDEX_TRACK = 6;
    public static final float ZINDEX_ROUTE = 5;
    public static final float ZINDEX_POSITION_ACCURACY_CIRCLE = 3;
    public static final float ZINDEX_HISTORY = 2;

    private static final String KEY_INDIVIDUAL_ROUTE = "INDIVIDUALROUTE";

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static final float LINE_MAXIMUM_DISTANCE_METERS = 10000;

    private Location coordinates;
    private float heading;
    private final PositionHistory history = new PositionHistory();

    private LatLng longTapLatLng;

    // settings for map auto rotation
    private Location lastBearingCoordinates = null;
    private int mapRotation = MAPROTATION_MANUAL;

    private static final int MAX_HISTORY_POINTS = 230; // TODO add alpha, make changeable in constructor?

    private WeakReference<GoogleMap> mapRef = null;
    private final GoogleMapObjects positionObjs;
    private final GoogleMapObjects longTapObjs;
    private final GoogleMapObjects historyObjs;
    private final GoogleMapObjects routeObjs;
    private final GoogleMapObjects trackObjs;
    private final GoogleMapView mapView;
    private GoogleMapView.PostRealDistance postRealDistance = null;
    private GoogleMapView.PostRealDistance postRouteDistance = null;

    private Viewport lastViewport = null;

    private final HashMap<String, GeoObjectCache> cache = new HashMap<>();

    private static class GeoObjectCache {
        //private boolean isHidden = false;
        private final List<MapObjectOptions> googleObjects = new ArrayList<>();
    }

    public GooglePositionAndHistory(final GoogleMap googleMap, final GoogleMapView mapView, final GoogleMapView.PostRealDistance postRealDistance, final GoogleMapView.PostRealDistance postRouteDistance) {
        this.mapRef = new WeakReference<>(googleMap);
        positionObjs = new GoogleMapObjects(googleMap);
        longTapObjs = new GoogleMapObjects(googleMap);
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
    public void setLongTapLatLng(final LatLng latLng) {
        longTapLatLng = latLng;
        repaintRequired();
    }

    @Override
    public LatLng getLongTapLatLng() {
        return longTapLatLng;
    }

    @Override
    public void resetLongTapLatLng() {
        longTapLatLng = null;
        repaintRequired();
    }

    @Override
    public ArrayList<TrailHistoryElement> getHistory() {
        return history.getHistory();
    }

    @Override
    public void setHistory(final ArrayList<TrailHistoryElement> history) {
        if (history != this.history.getHistory()) {
            this.history.setHistory(history);
        }
    }

    @Override
    public void updateIndividualRoute(final Route route) {
        updateRoute(KEY_INDIVIDUAL_ROUTE, route);
        if (postRouteDistance != null) {
            postRouteDistance.postRealDistance(route.getDistance());
        }
    }

    @Override
    public void updateRoute(final String key, final IGeoDataProvider track) {
        final boolean isIndividualRoute = KEY_INDIVIDUAL_ROUTE.equals(key);
        synchronized (cache) {
            GeoObjectCache c = cache.get(key);
            if (c == null) {
                c = new GeoObjectCache();
                cache.put(key, c);
            }
            final List<MapObjectOptions> newObjects = createGoogleObjects(track, isIndividualRoute ? ZINDEX_ROUTE : ZINDEX_TRACK);
            final List<MapObjectOptions> oldObjects = c.googleObjects;
            final GoogleMapObjects layer = isIndividualRoute ? routeObjs : trackObjs;
            layer.removeAll();
            layer.addAll(newObjects);
            c.geoobjects.clear();
            if (track != null && track.getGeoData() != null) {
                c.geoobjects.addAll(track.getGeoData());
                c.isHidden = track.isHidden();
            }
        }
        repaintRequired();
    }

    private static ArrayList<LatLng> toLatLng(final Collection<Geopoint> gcs) {
        final ArrayList<LatLng> list = new ArrayList<>();
        for (Geopoint gc : gcs) {
            list.add(toLatLng(gc));
        }
        return list;
    }

    private static LatLng toLatLng(final Geopoint gp) {
        return new LatLng(gp.getLatitude(), gp.getLongitude());
    }

    public void removeRoute(final String key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }

//    public void setHidden(final String key, final boolean isHidden) {
//        synchronized (cache) {
//            final GeoObjectGroup c = cache.get(key);
//            if (c != null) {
//                c.isHidden = isHidden;
//            }
//        }
//    }

    @Override
    public void repaintRequired() {
        drawPosition();
        drawHistory();
        drawViewport(lastViewport);
        drawRouteAndTracks();
        drawLongTapMarker();
    }

    private PolylineOptions getDirectionPolyline(final Geopoint from, final Geopoint to) {
        final PolylineOptions options = new PolylineOptions()
                .width(MapLineUtils.getDirectionLineWidth(false))
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
                .strokeColor(MapLineUtils.getAccuracyCircleColor())
                .strokeWidth(3)
                .fillColor(MapLineUtils.getAccuracyCircleFillColor())
                .radius(coordinates.getAccuracy())
                .zIndex(ZINDEX_POSITION_ACCURACY_CIRCLE)
        );

        positionObjs.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorCache.toBitmapDescriptor(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron, null)))
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
            try {
                final ArrayList<TrailHistoryElement> paintHistory = new ArrayList<>(getHistory());
                final int size = paintHistory.size();
                if (size < 2) {
                    return;
                }
                // always add current position to drawn history to have a closed connection, even if it's not yet recorded
                paintHistory.add(new TrailHistoryElement(coordinates));

                Location prev = paintHistory.get(0).getLocation();
                int current = 1;
                while (current < size) {
                    final List<LatLng> points = new ArrayList<>(MAX_HISTORY_POINTS);
                    points.add(new LatLng(prev.getLatitude(), prev.getLongitude()));

                    boolean paint = false;
                    while (!paint && current < size) {
                        final Location now = paintHistory.get(current).getLocation();
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
                                .width(MapLineUtils.getHistoryLineWidth(false))
                                .zIndex(ZINDEX_HISTORY)
                        );
                    }
                }
            } catch (OutOfMemoryError ignore) {
                Log.e("drawHistory: out of memory, please reduce max track history size");
                // better do not draw history than crash the map
            }
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

    private synchronized void drawRouteAndTracks() {
        // draw individual route
        routeObjs.removeAll();
        final GeoObjectCache individualRoute = cache.get(KEY_INDIVIDUAL_ROUTE);
        drawGeoObjects(individualRoute, routeObjs, ZINDEX_ROUTE);
        // draw tracks
        trackObjs.removeAll();
        synchronized (cache) {
            for (GeoObjectCache c : cache.values()) {
                drawGeoObjects(c, trackObjs, ZINDEX_TRACK);
           }
        }
    }

    private static List<MapObjectOptions> createGoogleObjects(final IGeoDataProvider provider, final float zLevel) {
        final List<MapObjectOptions> target = new ArrayList<>();
        // objectgroup hidden or no objects?
        if (provider == null || provider.isHidden() || provider.getGeoData() == null || provider.getGeoData().isEmpty()) {
            return target;
        }
        for (GeoObject go : provider.getGeoData()) {
            switch (go.getType()) {
                case POLYLINE:
                    target.add(MapObjectOptions.from(new PolylineOptions()
                            .addAll(toLatLng(go.getPoints()))
                            .color(go.getStyle().getStrokeColor())
                            .width(MapLineUtils.getAdjustedWidth(go.getStyle().getStrokeWidth(), false))
                            .zIndex(zLevel)
                    ));
                    break;
                case POINT:
                    target.add(MapObjectOptions.from(new CircleOptions()
                            .center(toLatLng(go.getPoints().get(0)))
                            .radius(MapLineUtils.getAdjustedWidth(go.getStyle().getStrokeWidth(), false))
                            .fillColor(go.getStyle().getStrokeColor())
                            .zIndex(zLevel)
                    ));
                    break;
                case POLYGON:
                default:
                    target.add(MapObjectOptions.from(new PolygonOptions()
                            .addAll(toLatLng(go.getPoints()))
                            .strokeColor(go.getStyle().getStrokeColor())
                            .strokeWidth(MapLineUtils.getAdjustedWidth(go.getStyle().getStrokeWidth(), false))
                            .fillColor(go.getStyle().getFillColor())
                            .zIndex(zLevel)
                    ));
            }
        }
        return target;
    }

    private synchronized void drawLongTapMarker() {
        longTapObjs.removeAll();
        if (longTapLatLng != null) {
            positionObjs.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorCache.toBitmapDescriptor(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.map_pin, null)))
                    .position(longTapLatLng)
                    .anchor(0.5f, 1f)
                    .zIndex(ZINDEX_POSITION)
            );
        }
    }
}
