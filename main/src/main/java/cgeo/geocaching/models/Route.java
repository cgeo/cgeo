package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.IGeoItemSupplier;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Route implements IGeoItemSupplier, Parcelable {
    private String name = "";
    protected ArrayList<RouteSegment> segments = new ArrayList<>();
    private final boolean routeable;
    protected float distance = 0.0f;
    protected boolean isHidden = false;

    public Route(final boolean routeable) {
        this.routeable = routeable;
    }

    public interface CenterOnPosition {
        void centerOnPosition(double latitude, double longitude, Viewport viewport);
    }

    public interface UpdateRoute {
        void updateRoute(IGeoItemSupplier route);
    }

    @Override
    public boolean hasData() {
        return getNumSegments() > 0;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return name;
    }

    public void add(final RouteSegment segment) {
        if (null == segments) {
            segments = new ArrayList<>();
        }
        segments.add(segment);
    }

    public int getNumSegments() {
        return null == segments ? 0 : segments.size();
    }

    public int getNumPoints() {
        int numPoints = 0;
        if (null != segments) {
            for (RouteSegment segment : segments) {
                numPoints += segment.getSize();
            }
        }
        return numPoints;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(final boolean hide) {
        this.isHidden = hide;
    }

    @Override
    public List<GeoPrimitive> getGeoData() {
        final List<GeoPrimitive> result = new ArrayList<>();
        final List<Geopoint> points = new ArrayList<>();
        if (getSegments() != null) {
            for (RouteSegment rs : getSegments()) {
                if (!points.isEmpty() && !rs.getLinkToPreviousSegment()) {
                    result.add(GeoPrimitive.createPolyline(points, null));
                    points.clear();
                }
                points.addAll(rs.getPoints());
            }
        }
        if (!points.isEmpty()) {
            result.add(GeoPrimitive.createPolyline(points, null));
        }
        return result;
    }

    @Override
    public Viewport getViewport() {
        final Viewport.ContainingViewportBuilder cvb = new Viewport.ContainingViewportBuilder();
        for (RouteSegment rs : getSegments()) {
            cvb.add(rs.getPoints());
        }
        return cvb.getViewport();
    }

    @Override
    public GeoItem getItem() {
        return GeoGroup.create(getGeoData());
    }

    public RouteSegment[] getSegments() {
        if (null != segments) {
            return segments.toArray(new RouteSegment[0]);
        } else {
            return null;
        }
    }

    public float getDistance() {
        return distance;
    }

    public void setCenter(final CenterOnPosition centerOnPosition) {
        if (null != segments && segments.size() > 0) {
            final ArrayList<Geopoint> points0 = segments.get(0).getPoints();
            if (points0.size() > 0) {
                final Geopoint first = points0.get(0);
                double minLat = first.getLatitude();
                double maxLat = first.getLatitude();
                double minLon = first.getLongitude();
                double maxLon = first.getLongitude();

                double latitude = 0.0d;
                double longitude = 0.0d;
                int numPoints = 0;
                for (RouteSegment segment : segments) {
                    final ArrayList<Geopoint> points = segment.getPoints();
                    if (points.size() > 0) {
                        numPoints += points.size();
                        for (Geopoint point : points) {
                            final double lat = point.getLatitude();
                            final double lon = point.getLongitude();

                            latitude += point.getLatitude();
                            longitude += point.getLongitude();

                            minLat = Math.min(minLat, lat);
                            maxLat = Math.max(maxLat, lat);
                            minLon = Math.min(minLon, lon);
                            maxLon = Math.max(maxLon, lon);
                        }
                    }
                }
                centerOnPosition.centerOnPosition(latitude / numPoints, longitude / numPoints, new Viewport(new Geopoint(minLat, minLon), new Geopoint(maxLat, maxLon)));
            }
        }
    }

    public void calculateNavigationRoute() {
        final int numSegments = getNumSegments();
        if (routeable && numSegments > 0) {
            for (int segment = 0; segment < numSegments; segment++) {
                calculateNavigationRoute(segment);
            }
        }
    }

    protected void calculateNavigationRoute(final int pos) {
        if (routeable && segments != null && pos < segments.size()) {
            final RouteSegment segment = segments.get(pos);
            distance -= segment.getDistance();
            if (routeable) {
                // clear info for current segment
                segment.resetPoints();
                // calculate route for segment between current point and its predecessor
                if (pos > 0) {
                    final Geopoint[] temp = Routing.getTrackNoCaching(segments.get(pos - 1).getPoint(), segment.getPoint());
                    for (Geopoint geopoint : temp) {
                        segment.addPoint(geopoint);
                    }
                }
            }
            distance += segment.calculateDistance();
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

    protected Route(final Parcel parcel) {
        name = parcel.readString();
        segments = parcel.readArrayList(RouteSegment.class.getClassLoader());
        routeable = parcel.readInt() != 0;
        distance = parcel.readFloat();
        isHidden = parcel.readInt() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(name);
        dest.writeList(segments);
        dest.writeInt(routeable ? 1 : 0);
        dest.writeFloat(distance);
        dest.writeInt(isHidden ? 1 : 0);
    }
}
