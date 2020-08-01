package cgeo.geocaching.maps.routing;

import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class RouteSegment implements Parcelable {
    private RouteItem item;
    private Geopoint point;
    private ArrayList<Geopoint> points;
    private float distance;

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

    public RouteItem getItem() {
        return item;
    }

    public Geopoint getPoint() {
        return point;
    }

    public ArrayList<Geopoint> getPoints() {
        return points;
    }

    public void addPoint(final Geopoint geopoint, final float distance) {
        points.add(geopoint);
        this.distance += distance;
    }

    public float getDistance() {
        return distance;
    }

    public void resetPoints() {
        points = new ArrayList<>();
        distance = 0.0f;
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

