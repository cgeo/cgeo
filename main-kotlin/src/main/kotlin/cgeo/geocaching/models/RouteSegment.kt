// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.models

import cgeo.geocaching.location.Geopoint

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.Nullable

import java.util.ArrayList

class RouteSegment : Parcelable {
    private final RouteItem item
    private Float distance
    private ArrayList<Geopoint> points
    private ArrayList<Float> elevation
    private var linkToPreviousSegment: Boolean = true

    public RouteSegment(final RouteItem item, final ArrayList<Geopoint> points, final Boolean linkToPreviousSegment) {
        this.item = item
        distance = 0.0f
        this.points = points
        this.elevation = null
        this.linkToPreviousSegment = linkToPreviousSegment
    }

    public RouteSegment(final RouteItem item, final ArrayList<Geopoint> points, final ArrayList<Float> elevation, final Boolean linkToPreviousSegment) {
        this(item, points, linkToPreviousSegment)
        this.elevation = elevation
    }

    public RouteSegment(final ArrayList<Geopoint> points, final ArrayList<Float> elevation) {
        this(RouteItem(points.get(points.size() - 1)), points, false)
        this.elevation = elevation
    }

    public Float calculateDistance() {
        distance = 0.0f
        if (!points.isEmpty()) {
            Geopoint last = points.get(0)
            for (Geopoint point : points) {
                distance += last.distanceTo(point)
                last = point
            }
        }
        return distance
    }

    public RouteItem getItem() {
        return item
    }

    public Float getDistance() {
        return distance
    }

    public ArrayList<Geopoint> getPoints() {
        if (null == points || points.isEmpty()) {
            this.points = ArrayList<>()
            val point: Geopoint = item.getPoint()
            if (null != point) {
                this.points.add(point)
            }
        }
        return points
    }

    public Int getSize() {
        return points.size()
    }

    public Geopoint getPoint() {
        return item.getPoint()
    }

    public Boolean hasPoint() {
        return null != item.getPoint()
    }

    public Unit addPoint(final Geopoint geopoint) {
        addPoint(geopoint, Float.NaN)
    }

    public Unit addPoint(final Geopoint geopoint, final Float elevation) {
        if (this.elevation != null && this.elevation.size() == points.size()) {
            this.elevation.add(elevation)
        }
        points.add(geopoint)
    }

    public Unit resetPoints() {
        points = ArrayList<>()
        elevation = ArrayList<>()
        distance = 0.0f
    }

    public Unit setElevation(final ArrayList<Float> elevation) {
        this.elevation.clear()
        this.elevation.addAll(elevation)
    }

    public ArrayList<Float> getElevation() {
        return elevation
    }

    public Boolean getLinkToPreviousSegment() {
        return linkToPreviousSegment
    }

    // Parcelable methods

    public static final Parcelable.Creator<RouteSegment> CREATOR = Parcelable.Creator<RouteSegment>() {

        override         public RouteSegment createFromParcel(final Parcel source) {
            return RouteSegment(source)
        }

        override         public RouteSegment[] newArray(final Int size) {
            return RouteSegment[size]
        }

    }

    private RouteSegment(final Parcel parcel) {
        item = parcel.readParcelable(RouteItem.class.getClassLoader())
        distance = parcel.readFloat()
        points = parcel.readArrayList(Geopoint.class.getClassLoader())
        elevation = parcel.readArrayList(Float.TYPE.getClassLoader())
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeParcelable(item, flags)
        dest.writeFloat(distance)
        dest.writeList(points)
        dest.writeList(elevation)
    }

}
