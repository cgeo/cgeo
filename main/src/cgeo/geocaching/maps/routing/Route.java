package cgeo.geocaching.maps.routing;

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

public class Route implements Parcelable {

    public interface RouteUpdater {
        void updateRoute(ArrayList<Geopoint> route, float distance);
    }

    private static class RouteSegment implements Parcelable {
        RouteItem item = null;
        Geopoint point = null;
        ArrayList<Geopoint> points = null;
        float distance = 0.0f;

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

        // Parcelable methods

        public static final Creator<RouteSegment> CREATOR = new Creator<RouteSegment>() {

            @Override
            public RouteSegment createFromParcel(final Parcel source) {
                return new RouteSegment(source);
            }

            @Override
            public RouteSegment[] newArray(final int size) {
                return new RouteSegment[size];
            }

        };

        RouteSegment (final Parcel parcel) {
            item = parcel.readParcelable(null);
            point = parcel.readParcelable(null);
            points = parcel.readArrayList(null);
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

    private ArrayList<RouteSegment> segments = null;

    public Route() {
    }

    public void toggleItem(final Context context, final RouteItem item, final RouteUpdater routeUpdater) {
        if (segments == null) {
            segments = new ArrayList<RouteSegment>();
        }
        final int pos = pos(item);
        if (pos == -1) {
            segments.add(new RouteSegment(item));
            calculate(segments.size() - 1);
            Toast.makeText(context, "added to route", Toast.LENGTH_SHORT).show();
        } else {
            segments.remove(pos);
            calculate(pos);
            if (pos < segments.size()) {
                calculate(pos + 1);
            }
            Toast.makeText(context, "removed from route", Toast.LENGTH_SHORT).show();
        }
        updateRoute(routeUpdater);
    }

    public void updateRoute(final RouteUpdater routeUpdater) {
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
                        if (tempPos > 0) {
                            segment.distance += temp[tempPos - 1].distanceTo(temp[tempPos]);
                        }
                    }
                }
            }
        }
    }

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

    Route (final Parcel parcel) {
        segments = parcel.readArrayList(null);
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

