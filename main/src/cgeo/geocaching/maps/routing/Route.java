package cgeo.geocaching.maps.routing;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;

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

    private static class RouteSegment implements Parcelable {
        private RouteItem item = null;
        private Geopoint point = null;
        private ArrayList<Geopoint> points = null;
        private float distance = 0.0f;

        RouteSegment(final RouteItem item) {
            this.item = item;

            point = null;
            if (item.getType() == CoordinatesType.CACHE) {
                final Geocache cache = DataStore.loadCache(item.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    point = cache.getCoords();
                }
            } else if (item.getType() == CoordinatesType.WAYPOINT && item.getId() >= 0) {
                final Waypoint waypoint = DataStore.loadWaypoint(item.getId());
                if (waypoint != null) {
                    point = waypoint.getCoords();
                }
            }

            points = null;
            distance = 0.0f;
        }

        public boolean hasPoint() {
            return point != null;
        }

        // Parcelable methods

        public static final Parcelable.Creator<RouteSegment> CREATOR = new Parcelable.Creator<RouteSegment>() {

            @Override
            public RouteSegment createFromParcel(final Parcel source) {
                return new RouteSegment(source);
            }

            @Override
            public RouteSegment[] newArray(final int size) {
                return new RouteSegment[size];
            }

        };

        private RouteSegment (final Parcel parcel) {
            item = parcel.readParcelable(RouteItem.class.getClassLoader());
            point = parcel.readParcelable(Geopoint.class.getClassLoader());
            points = parcel.readArrayList(Geopoint.class.getClassLoader());
            distance = parcel.readFloat();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeParcelable(item, flags);
            dest.writeParcelable(point, flags);
            dest.writeList(points);
            dest.writeFloat(distance);
        }

    }

    public Route() {
    }

    public void toggleItem(final Context context, final RouteItem item, final RouteUpdater routeUpdater) {
        if (loadingRoute) {
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
                if (segment.points != null) {
                    for (int j = 0; j < segment.points.size(); j++) {
                        route.add(segment.points.get(j));
                    }
                }
                distance += segment.distance;
            }
        }
        routeUpdater.updateRoute(route, distance);
    }

    public void clearRoute(final RouteUpdater routeUpdater) {
        Schedulers.io().scheduleDirect(() -> DataStore.clearRoute());
        segments = null;
        routeUpdater.updateRoute(new ArrayList<>(), 0);
    }

    public void loadRoute() {
        if (loadingRoute) {
            return;
        }
        Schedulers.io().scheduleDirect(() -> loadRouteInternal());
    }

    public boolean isEmpty() {
        return segments == null || segments.size() < 1;
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
                routeItems.add(segments.get(i).item);
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
            segments = new ArrayList<RouteSegment>();
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
            if (segments.get(i).item.getGeocode().equals(geocode)) {
                return i;
            }
        }
        return -1;
    }

    private void calculate (final int pos) {
        if (segments != null && pos < segments.size()) {
            // clear info for current segment
            final RouteSegment segment = segments.get(pos);
            segment.points = new ArrayList<>();
            segment.distance = 0.0f;
            // calculate route for segment between current point and its predecessor
            if (pos > 0) {
                // save points and calculate distance
                final Geopoint[] temp = Routing.getTrackNoCaching(segments.get(pos - 1).point, segment.point);
                if (temp != null && temp.length > 0) {
                    for (int tempPos = 0; tempPos < temp.length; tempPos++) {
                        segment.points.add(temp[tempPos]);
                        if (tempPos > 0 && temp[tempPos - 1] != null && temp[tempPos] != null) {
                            segment.distance += temp[tempPos - 1].distanceTo(temp[tempPos]);
                        }
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
