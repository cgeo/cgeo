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
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.routing.Routing
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.GeoItem
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.IGeoItemSupplier

import android.os.Parcel
import android.os.Parcelable

import java.util.ArrayList
import java.util.List

class Route : IGeoItemSupplier, Parcelable {
    private var name: String = ""
    protected var segments: ArrayList<RouteSegment> = ArrayList<>()
    private Boolean routeable
    protected var distance: Float = 0.0f
    protected var isHidden: Boolean = false

    public Route() {
        // should use setRouteable later if using this constructor
    }

    public Route(final Boolean routeable) {
        this.routeable = routeable
    }

    interface CenterOnPosition {
        Unit centerOnPosition(Double latitude, Double longitude, Viewport viewport)
    }

    interface UpdateRoute {
        Unit updateRoute(IGeoItemSupplier route)
    }

    override     public Boolean hasData() {
        return getNumSegments() > 0
    }

    public Unit setName(final String name) {
        this.name = name
    }

    public Unit setRouteable(final Boolean routeable) {
        this.routeable = routeable
    }

    public String getName() {
        return name
    }

    public Unit add(final RouteSegment segment) {
        if (null == segments) {
            segments = ArrayList<>()
        }
        segments.add(segment)
    }

    public Int getNumSegments() {
        return null == segments ? 0 : segments.size()
    }

    public Int getNumPoints() {
        Int numPoints = 0
        if (null != segments) {
            for (RouteSegment segment : segments) {
                numPoints += segment.getSize()
            }
        }
        return numPoints
    }

    public Boolean isHidden() {
        return isHidden
    }

    public Unit setHidden(final Boolean hide) {
        this.isHidden = hide
    }

    override     public Viewport getViewport() {
        final Viewport.ContainingViewportBuilder cvb = Viewport.ContainingViewportBuilder()
        for (RouteSegment rs : getSegments()) {
            cvb.add(rs.getPoints())
        }
        return cvb.getViewport()
    }

    override     public GeoItem getItem() {
        final GeoGroup.Builder result = GeoGroup.builder()
        val points: List<Geopoint> = ArrayList<>()
        if (getSegments() != null) {
            for (RouteSegment rs : getSegments()) {
                if (!points.isEmpty() && !rs.getLinkToPreviousSegment()) {
                    result.addItems(GeoPrimitive.createPolyline(points, null))
                    points.clear()
                }
                points.addAll(rs.getPoints())
            }
        }
        if (!points.isEmpty()) {
            result.addItems(GeoPrimitive.createPolyline(points, null))
        }

        return result.build()
    }

    public RouteSegment[] getSegments() {
        if (null != segments) {
            return segments.toArray(RouteSegment[0])
        } else {
            return null
        }
    }

    public Float getDistance() {
        return distance
    }

    public Unit setCenter(final CenterOnPosition centerOnPosition) {
        if (null != segments && !segments.isEmpty()) {
            val points0: ArrayList<Geopoint> = segments.get(0).getPoints()
            if (!points0.isEmpty()) {
                val first: Geopoint = points0.get(0)
                Double minLat = first.getLatitude()
                Double maxLat = first.getLatitude()
                Double minLon = first.getLongitude()
                Double maxLon = first.getLongitude()

                Double latitude = 0.0d
                Double longitude = 0.0d
                Int numPoints = 0
                for (RouteSegment segment : segments) {
                    val points: ArrayList<Geopoint> = segment.getPoints()
                    if (!points.isEmpty()) {
                        numPoints += points.size()
                        for (Geopoint point : points) {
                            val lat: Double = point.getLatitude()
                            val lon: Double = point.getLongitude()

                            latitude += point.getLatitude()
                            longitude += point.getLongitude()

                            minLat = Math.min(minLat, lat)
                            maxLat = Math.max(maxLat, lat)
                            minLon = Math.min(minLon, lon)
                            maxLon = Math.max(maxLon, lon)
                        }
                    }
                }
                centerOnPosition.centerOnPosition(latitude / numPoints, longitude / numPoints, Viewport(Geopoint(minLat, minLon), Geopoint(maxLat, maxLon)))
            }
        }
    }

    public Unit calculateNavigationRoute() {
        val numSegments: Int = getNumSegments()
        if (routeable && numSegments > 0) {
            for (Int segment = 0; segment < numSegments; segment++) {
                calculateNavigationRoute(segment)
            }
        }
    }

    protected Unit calculateNavigationRoute(final Int pos) {
        if (routeable && segments != null && pos < segments.size()) {
            val segment: RouteSegment = segments.get(pos)
            distance -= segment.getDistance()
            if (routeable) {
                // clear info for current segment
                segment.resetPoints()
                // calculate route for segment between current point and its predecessor
                if (pos > 0) {
                    val elevation: ArrayList<Float> = ArrayList<>()
                    final Geopoint[] temp = Routing.getTrackNoCaching(segments.get(pos - 1).getPoint(), segment.getPoint(), elevation)
                    for (Geopoint geopoint : temp) {
                        segment.addPoint(geopoint)
                    }
                    segment.setElevation(elevation)
                }
            }
            distance += segment.calculateDistance()
        }
    }

    // Parcelable methods

    public static val CREATOR: Creator<Route> = Creator<Route>() {

        override         public Route createFromParcel(final Parcel source) {
            return Route(source)
        }

        override         public Route[] newArray(final Int size) {
            return Route[size]
        }

    }

    protected Route(final Parcel parcel) {
        name = parcel.readString()
        segments = parcel.readArrayList(RouteSegment.class.getClassLoader())
        routeable = parcel.readInt() != 0
        distance = parcel.readFloat()
        isHidden = parcel.readInt() != 0
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeString(name)
        dest.writeList(segments)
        dest.writeInt(routeable ? 1 : 0)
        dest.writeFloat(distance)
        dest.writeInt(isHidden ? 1 : 0)
    }
}
