package cgeo.geocaching.maps.routing;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import java.util.ArrayList;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class Route implements Parcelable {

    private enum ToggleItemState {
        ADDED,
        REMOVED,
        ERROR_NO_POINT
    }

    private ArrayList<RouteSegment> segments = null;
    private boolean loadingRoute = false;

    public interface RouteUpdater {
        void updateRoute(ArrayList<Geopoint> route, float distance);
    }

    public Route() {
    }

    public void toggleItem(final Context context, final RouteItem item, final RouteUpdater routeUpdater) {
        if (loadingRoute) {
            return;
        }

        if (item.getType() == CoordinatesType.WAYPOINT && item.getId() == -1) {
            Toast.makeText(context, R.string.individual_route_error_single_waypoint_mode, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (toggleItemInternal(item)) {
            case ADDED:
                Toast.makeText(context, R.string.individual_route_added, Toast.LENGTH_SHORT).show();
                break;
            case REMOVED:
                Toast.makeText(context, R.string.individual_route_removed, Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(context, R.string.individual_route_error_toggling_waypoint, Toast.LENGTH_SHORT).show();
                break;
        }
        updateRoute(routeUpdater);
        saveRoute();
    }

    public void updateRoute(final RouteUpdater routeUpdater) {
        if (loadingRoute) {
            return;
        }

        final ArrayList<Geopoint> route = new ArrayList<>();
        float distance = 0.0f;
        if (segments != null) {
            for (int i = 0; i < segments.size(); i++) {
                final RouteSegment segment = segments.get(i);
                final ArrayList<Geopoint> points = segment.getPoints();
                if (points != null) {
                    route.addAll(points);
                }
                distance += segment.getDistance();
            }
        }
        if (null != routeUpdater) {
            routeUpdater.updateRoute(route, distance);
        }
    }

    public void clearRoute(final RouteUpdater routeUpdater) {
        clearRouteInternal(routeUpdater, true);
    }

    public void reloadRoute(final RouteUpdater routeUpdater) {
        clearRouteInternal(routeUpdater, false);
        AndroidRxUtils.andThenOnUi(Schedulers.io(), this::loadRouteInternal, () -> updateRoute(routeUpdater));
    }

    public void loadRoute() {
        if (loadingRoute) {
            return;
        }
        Schedulers.io().scheduleDirect(this::loadRouteInternal);
    }

    public boolean isEmpty() {
        return segments == null || segments.size() < 1;
    }

    public RouteSegment[] getSegments() {
        return segments.toArray(new RouteSegment[0]);
    }

    private void clearRouteInternal(final RouteUpdater routeUpdater, final boolean deleteInDatabase) {
        if (deleteInDatabase) {
            Schedulers.io().scheduleDirect(DataStore::clearRoute);
        }
        segments = null;
        if (null != routeUpdater) {
            routeUpdater.updateRoute(new ArrayList<>(), 0);
        }
    }

    private synchronized void loadRouteInternal() {
        loadingRoute = true;
        final ArrayList<RouteItem> routeItems = DataStore.loadRoute();
        for (int i = 0; i < routeItems.size(); i++) {
            toggleItemInternal(routeItems.get(i));
        }
        loadingRoute = false;
    }

    private synchronized void saveRoute() {
        if (segments != null) {
            final ArrayList<RouteItem> routeItems = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                routeItems.add(segments.get(i).getItem());
            }
            Schedulers.io().scheduleDirect(() -> DataStore.saveRoute(routeItems));
        }
    }

    /**
     * adds or removes the item given
     * @param item
     * @return ToggleItemState
     */
    private ToggleItemState toggleItemInternal(final RouteItem item) {
        if (segments == null) {
            segments = new ArrayList<>();
        }
        final int pos = pos(item);
        if (pos == -1) {
            final RouteSegment segment = new RouteSegment(item);
            if (segment.hasPoint()) {
                segments.add(new RouteSegment(item));
                calculate(segments.size() - 1);
                return ToggleItemState.ADDED;
            } else {
                return ToggleItemState.ERROR_NO_POINT;
            }
        } else {
            segments.remove(pos);
            calculate(pos);
            if (pos < segments.size()) {
                calculate(pos + 1);
            }
            return ToggleItemState.REMOVED;
        }
    }

    private int pos(final RouteItem item) {
        if (segments == null || segments.size() == 0) {
            return -1;
        }

        final String geocode = item.getGeocode();
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).getItem().getGeocode().equals(geocode)) {
                return i;
            }
        }
        return -1;
    }

    private void calculate (final int pos) {
        if (segments != null && pos < segments.size()) {
            // clear info for current segment
            final RouteSegment segment = segments.get(pos);
            segment.resetPoints();
            // calculate route for segment between current point and its predecessor
            if (pos > 0) {
                // save points and calculate distance
                final Geopoint[] temp = Routing.getTrackNoCaching(segments.get(pos - 1).getPoint(), segment.getPoint());
                if (temp != null && temp.length > 0) {
                    for (int tempPos = 0; tempPos < temp.length; tempPos++) {
                        segment.addPoint(temp[tempPos], (tempPos > 0 && temp[tempPos - 1] != null && temp[tempPos] != null) ? temp[tempPos - 1].distanceTo(temp[tempPos]) : 0.0f);
                    }
                }
            }
        }
    }

    // Parcelable methods

    public static final Creator<Route> CREATOR = new Creator<Route>() {

        @Override
        public Route createFromParcel(final Parcel source) {
            return new Route(source);
        }

        @Override
        public Route[] newArray(final int size) {
            return new Route[size];
        }

    };

    private Route (final Parcel parcel) {
        segments = parcel.readArrayList(RouteSegment.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeList(segments);
    }
}
