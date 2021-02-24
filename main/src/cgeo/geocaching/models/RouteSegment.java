package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class RouteSegment implements Parcelable {
    @NonNull
    private final RouteItem item;
    private float distance;
    @NonNull
    private ArrayList<Geopoint> points;

    public RouteSegment(@NonNull final RouteItem item, @Nullable final ArrayList<Geopoint> points) {
        this.item = item;
        distance = 0.0f;
        this.points = points != null ? points : new ArrayList<>();
    }

    public float calculateDistance() {
        distance = 0.0f;
        if (points.size() > 0) {
            Geopoint last = points.get(0);
            for (Geopoint point : points) {
                distance += last.distanceTo(point);
                last = point;
            }
        }
        return distance;
    }

    @NonNull
    public RouteItem getItem() {
        return item;
    }

    public float getDistance() {
        return distance;
    }

    public ArrayList<Geopoint> getPoints() {
        if (points.size() == 0) {
            final Geopoint point = item.getPoint();
            if (null != point) {
                points.add(point);
            }
        }
        return points;
    }

    public int getSize() {
        return points.size();
    }

    public Geopoint getPoint() {
        return item.getPoint();
    }

    public boolean hasPoint() {
        return null != item.getPoint();
    }

    public void addPoint(final Geopoint geopoint) {
        points.add(geopoint);
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

    private RouteSegment(@NonNull final Parcel parcel) {
        item = parcel.readParcelable(RouteItem.class.getClassLoader());
        distance = parcel.readFloat();
        points = parcel.readArrayList(Geopoint.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(item, flags);
        dest.writeFloat(distance);
        dest.writeList(points);
    }
}
