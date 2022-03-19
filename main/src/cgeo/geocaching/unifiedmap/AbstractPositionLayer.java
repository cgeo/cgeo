package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.TrailHistoryElement;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;
import static cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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
public abstract class AbstractPositionLayer<T> {

    protected Location currentLocation = null;
    protected float currentHeading = 0.0f;

    // position and heading arrow
    protected Bitmap positionAndHeadingArrow = BitmapFactory.decodeResource(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron);
    protected int arrowWidth = positionAndHeadingArrow.getWidth();
    protected int arrowHeight = positionAndHeadingArrow.getHeight();

    // position history
    protected final PositionHistory history = new PositionHistory();
    private static final int MAX_HISTORY_POINTS = 230;

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    protected static final float LINE_MAXIMUM_DISTANCE_METERS = 10000;

    // settings for map auto rotation
    private Location lastBearingCoordinates = null;
    private int mapRotation = MAPROTATION_MANUAL;

    // routes & tracks
    protected static final String KEY_INDIVIDUAL_ROUTE = "INDIVIDUALROUTE";
    protected final HashMap<String, CachedRoute> cache = new HashMap<>();
    protected class CachedRoute {
        public boolean isHidden = false;
        public ArrayList<ArrayList<T>> track = null;
    }

    public void setCurrentPositionAndHeading(final Location location, final float heading) {
        final boolean coordChanged = !Objects.equals(location, currentLocation);
        final boolean headingChanged = heading != currentHeading;
        currentLocation = location;
        currentHeading = heading;

        if (coordChanged || headingChanged) {
            repaintArrow();
        }

        if (coordChanged) {
            history.rememberTrailPosition(location);
            repaintHistory();

            /*
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

             */

        }
    }

    public float getCurrentHeading() {
        return currentHeading;
    }

    public ArrayList<TrailHistoryElement> getHistory() {
        return history.getHistory();
    }

    // ========================================================================
    // route / track handling

    private void updateRoute(final String key, final Route track, final Func1<Route, ArrayList<ArrayList<T>>> getAllPoints) {
        synchronized (cache) {
            CachedRoute c = cache.get(key);
            if (c == null) {
                c = new CachedRoute();
                cache.put(key, c);
            }
            c.track = null;
            if (track != null) {
                c.track = getAllPoints.call(track);
                c.isHidden = track.isHidden();
            }
        }
        repaintRouteAndTracks();
    }

    private void removeRoute(final String key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }

    public abstract void updateIndividualRoute(Route route);

    public void updateIndividualRoute(final Route route, final Func1<Route, ArrayList<ArrayList<T>>> getAllPoints) {
        updateRoute(KEY_INDIVIDUAL_ROUTE, route, getAllPoints);
        repaintRouteAndTracks();
        // @todo update distance info
    }


    // ========================================================================
    // repaint methods

    public void repaintRequired() {
        repaintArrow();
        repaintPosition();
        repaintHistory();
        repaintRouteAndTracks();
    }

    protected abstract void repaintArrow();
    protected abstract void repaintPosition();
    protected abstract void repaintHistory();
    protected abstract void repaintRouteAndTracks();

    public void repaintHistoryHelper(final Func2<Double, Double, T> createNewPoint, final Action1<List<T>> addSegment) {
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
}
