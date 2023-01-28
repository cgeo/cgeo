package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.IGeoDataProvider;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Action2;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;
import static cgeo.geocaching.settings.Settings.MAPROTATION_AUTO;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;

import android.graphics.Bitmap;
import android.location.Location;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.oscim.core.GeoPoint;

/**
 * layer for positioning the following elements on the map
 * - position and heading arrow
 * - position history
 * - direction line
 * - target and distance infos (target cache, distance to target, length of individual route)
 * - individual route
 * - route(s)/track(s)
 *
 * T is the type the map expects its coordinates in (LatLng for Google Maps, GeoPoint for Mapsforge)
 */
public abstract class AbstractPositionLayer<T> implements IndividualRoute.UpdateIndividualRoute {

    protected Location currentLocation = null;
    protected float currentHeading = 0.0f;
    protected Func2<Double, Double, T> createNewPoint;

    // distance view
    protected GeoPoint destination;
    protected float routedDistance = 0.0f;
    protected float directDistance = 0.0f;
    protected float individualRouteDistance = 0.0f;
    private final boolean showBothDistances = Settings.isBrouterShowBothDistances();
    public final UnifiedTargetAndDistancesHandler mapDistanceDrawer;

    // position and heading arrow
    protected Bitmap positionAndHeadingArrow = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron, null));
    protected int arrowWidth = positionAndHeadingArrow.getWidth();
    protected int arrowHeight = positionAndHeadingArrow.getHeight();

    // position history
    protected final PositionHistory history = new PositionHistory();
    private static final int MAX_HISTORY_POINTS = 230;

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static final float LINE_MAXIMUM_DISTANCE_METERS = 10000;

    // settings for map auto rotation
    private Location lastBearingCoordinates = null;
    private int mapRotation = MAPROTATION_MANUAL;

    // routes & tracks
    private static final String KEY_INDIVIDUAL_ROUTE = "INDIVIDUALROUTE";
    private final HashMap<String, CachedRoute> cache = new HashMap<>();

    private class CachedRoute {
        private boolean isHidden = false;
        private List<List<T>> track = null;
    }

    protected AbstractPositionLayer(final View root, final Func2<Double, Double, T> createNewPoint) {
        mapDistanceDrawer = new UnifiedTargetAndDistancesHandler(root);
        this.createNewPoint = createNewPoint;
    }

    public abstract void setCurrentPositionAndHeading(Location location, float heading);

    public float getCurrentHeading() {
        return currentHeading;
    }

    // ========================================================================
    // history handling

    public void clearHistory() {
        history.reset();
        repaintHistory();
    }

    public ArrayList<TrailHistoryElement> getHistory() {
        return history.getHistory();
    }

    // ========================================================================
    // distance view handling

    public final void setDestination(final GeoPoint destination) {
        this.destination = destination;
        repaintDestinationHelper(null);
    }

    // ========================================================================
    // route / track handling

    private <K extends IGeoDataProvider> void updateRoute(final String key, final K track, final Func1<K, List<List<T>>> getAllPoints) {
        synchronized (cache) {
            CachedRoute c = cache.get(key);
            if (track == null) {
                if (c != null) {
                    cache.remove(key);
                }
            } else {
                if (c == null) {
                    c = new CachedRoute();
                    cache.put(key, c);
                }
                c.track = getAllPoints.call(track);
                c.isHidden = track.isHidden();
            }
        }
        repaintRouteAndTracks();
    }

    public abstract void updateIndividualRoute(Route route);

    public abstract void updateTrack(String key, IGeoDataProvider track);

    public void updateIndividualRoute(final Route route, final Func1<Route, List<List<T>>> getAllPoints) {
        updateRoute(KEY_INDIVIDUAL_ROUTE, route, getAllPoints);
        repaintRouteAndTracks();
        individualRouteDistance = route.getDistance();
        mapDistanceDrawer.drawRouteDistance(individualRouteDistance);
    }

    public void updateTrack(final String key, final IGeoDataProvider track, final Func1<IGeoDataProvider, List<List<T>>> getAllPoints) {
        updateRoute(key, track, getAllPoints);
        repaintRouteAndTracks();
    }

    // ========================================================================
    // repaint methods

    public void repaintRequired() {
        repaintPosition();
        repaintHistory();
        repaintRouteAndTracks();
    }

    protected void setCurrentPositionAndHeadingHelper(final Location location, final float heading, final Action1<List<T>> drawDirection, final AbstractUnifiedMapView<T> map) {
        final boolean coordChanged = !Objects.equals(location, currentLocation);
        final boolean headingChanged = heading != currentHeading;
        currentLocation = location;
        currentHeading = heading;

        if (coordChanged || headingChanged) {
            repaintPosition();
        }

        if (coordChanged) {
            history.rememberTrailPosition(location);
            repaintHistory();

            if (destination != null) {
                final Location l = new Location("UnifiedMap");
                l.setLatitude(destination.getLatitude());
                l.setLongitude(destination.getLongitude());
                directDistance = currentLocation.distanceTo(l) / 1000f;
                repaintDestinationHelper(drawDirection);
            }

            if (mapRotation == MAPROTATION_AUTO) {
                if (null != lastBearingCoordinates) {
                    if (null != map) {
                        final float currentBearing = map.getCurrentBearing();
                        final float bearing = AngleUtils.normalize(lastBearingCoordinates.bearingTo(currentLocation));
                        final float bearingDiff = Math.abs(AngleUtils.difference(bearing, currentBearing));
                        if (bearingDiff > 15.0f) {
                            lastBearingCoordinates = currentLocation;
                            map.setBearing(bearing);
                        }
                    } else {
                        lastBearingCoordinates = null;
                    }
                } else {
                    lastBearingCoordinates = currentLocation;
                }
            }
        }
    }

    protected void repaintPosition() {
        mapDistanceDrawer.drawDistance(showBothDistances, directDistance, routedDistance);
        mapDistanceDrawer.drawRouteDistance(individualRouteDistance);
    }

    protected abstract void repaintHistory();

    protected abstract void repaintRouteAndTracks();

    protected void repaintHistoryHelper(final Action1<List<T>> addSegment) {
        if (null == currentLocation) {
            return;
        }
        if (Settings.isMapTrail()) {
            try {
                final ArrayList<TrailHistoryElement> paintHistory = new ArrayList<>(getHistory());
                final int size = paintHistory.size();
                if (size < 2) {
                    return;
                }
                // always add current position to drawn history to have a closed connection, even if it's not yet recorded
                paintHistory.add(new TrailHistoryElement(currentLocation));

                Location prev = paintHistory.get(0).getLocation();
                int current = 1;
                while (current < size) {
                    final List<T> points = new ArrayList<>(MAX_HISTORY_POINTS);
                    points.add(createNewPoint.call(prev.getLatitude(), prev.getLongitude()));

                    boolean paint = false;
                    while (!paint && current < size) {
                        final Location now = paintHistory.get(current).getLocation();
                        current++;
                        // split into segments if distance between adjacent points is too far
                        if (now.distanceTo(prev) < LINE_MAXIMUM_DISTANCE_METERS) {
                            points.add(createNewPoint.call(now.getLatitude(), now.getLongitude()));
                        } else {
                            paint = true;
                        }
                        prev = now;
                    }
                    if (points.size() > 1) {
                        addSegment.call(points);
                    }
                }
            } catch (OutOfMemoryError ignore) {
                Log.e("drawHistory: out of memory, please reduce max track history size");
                // better do not draw history than crash the map
            }
        }

    }

    private void repaintDestinationHelper(final @Nullable Action1<List<T>> drawSegment) {
        if (currentLocation != null && destination != null) {
            final Geopoint[] routingPoints = Routing.getTrack(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude()), new Geopoint(destination.getLatitude(), destination.getLongitude()));
            final ArrayList<T> points = new ArrayList<>();
            points.add(createNewPoint.call(routingPoints[0].getLatitude(), routingPoints[0].getLongitude()));

            routedDistance = 0.0f;
            if (routingPoints.length > 2 || Settings.isMapDirection()) {
                for (int i = 1; i < routingPoints.length; i++) {
                    points.add(createNewPoint.call(routingPoints[i].getLatitude(), routingPoints[i].getLongitude()));
                    routedDistance += routingPoints[i - 1].distanceTo(routingPoints[i]);
                }
                if (drawSegment != null) {
                    drawSegment.call(points);
                }
            }
            mapDistanceDrawer.drawDistance(showBothDistances, directDistance, routedDistance);
        }
    }

    protected void repaintRouteAndTracksHelper(final Action2<List<T>, Boolean> addSegment) {
        final CachedRoute individualRoute = cache.get(KEY_INDIVIDUAL_ROUTE);
        synchronized (cache) {
            for (CachedRoute c : cache.values()) {
                final boolean isTrack = c != individualRoute;
                // route hidden, no route or route too short?
                if (!c.isHidden && c.track != null && c.track.size() > 0) {
                    for (List<T> segment : c.track) {
                        addSegment.call(segment, isTrack);
                    }
                }
            }
        }
    }
}
