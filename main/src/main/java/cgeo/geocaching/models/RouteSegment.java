package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class RouteSegment implements Parcelable {
    private final RouteItem item;
    private float distance;
    private ArrayList<Geopoint> points;
    private boolean linkToPreviousSegment = true;

    public RouteSegment(final RouteItem item, final ArrayList<Geopoint> points, final boolean linkToPreviousSegment) {
        this.item = item;
        distance = 0.0f;
        this.points = points;
        this.linkToPreviousSegment = linkToPreviousSegment;
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

    public RouteItem getItem() {
        return item;
    }

    public float getDistance() {
        return distance;
    }

    public ArrayList<Geopoint> getPoints() {
        if (null == points || points.size() == 0) {
            this.points = new ArrayList<>();
            final Geopoint point = item.getPoint();
            if (null != point) {
                this.points.add(point);
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

    public boolean getLinkToPreviousSegment() {
        return linkToPreviousSegment;
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

    private RouteSegment(final Parcel parcel) {
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
