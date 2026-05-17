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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Route implements IGeoItemSupplier, Parcelable {
    private String name = "";
    protected final List<RouteSegment> segments = Collections.synchronizedList(new ArrayList<>());
    private boolean routeable;
    protected float distance = 0.0f;
    protected boolean isHidden = false;

    public Route() {
        // should use setRouteable later if using this constructor
    }

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

    public void setRouteable(final boolean routeable) {
        this.routeable = routeable;
    }

    public boolean isRouteable() {
        return routeable;
    }

    public String getName() {
        return name;
    }

    public void add(final RouteSegment segment) {
        segments.add(segment);
    }

    public int getNumSegments() {
        return segments.size();
    }

    public int getNumPoints() {
        int numPoints = 0;
        synchronized (segments) {
            for (final RouteSegment segment : segments) {
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
    public Viewport getViewport() {
        final Viewport.ContainingViewportBuilder cvb = new Viewport.ContainingViewportBuilder();
        synchronized (segments) {
            for (RouteSegment rs : segments) {
                cvb.add(rs.getPoints());
            }
        }
        return cvb.getViewport();
    }

    @Override
    public GeoItem getItem() {
        final GeoGroup.Builder result = GeoGroup.builder();
        final List<Geopoint> points = new ArrayList<>();
        synchronized (segments) {
            for (RouteSegment rs : segments) {
                if (!points.isEmpty() && !rs.getLinkToPreviousSegment()) {
                    result.addItems(GeoPrimitive.createPolyline(points, null));
                    points.clear();
                }
                points.addAll(rs.getPoints());
            }
        }
        if (!points.isEmpty()) {
            result.addItems(GeoPrimitive.createPolyline(points, null));
        }

        return result.build();
    }

    @NonNull
    public RouteSegment[] getSegments() {
        synchronized (segments) {
            return segments.toArray(new RouteSegment[0]);
        }
    }

    public float getDistance() {
        return distance;
    }

    public void setCenter(final CenterOnPosition centerOnPosition) {
        synchronized (segments) {
            if (!segments.isEmpty()) {
                final ArrayList<Geopoint> points0 = segments.get(0).getPoints();
                if (!points0.isEmpty()) {
                    final Geopoint first = points0.get(0);
                    double minLat = first.getLatitude();
                    double maxLat = first.getLatitude();
                    double minLon = first.getLongitude();
                    double maxLon = first.getLongitude();

                    double latitude = 0.0d;
                    double longitude = 0.0d;
                    int numPoints = 0;
                    for (final RouteSegment segment : segments) {
                        final ArrayList<Geopoint> points = segment.getPoints();
                        if (!points.isEmpty()) {
                            numPoints += points.size();
                            for (final Geopoint point : points) {
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
        synchronized (segments) {
            if (routeable && pos < segments.size()) {
                final RouteSegment segment = segments.get(pos);
                distance -= segment.getDistance();
                if (routeable) {
                    // clear info for current segment
                    segment.resetPoints();
                    // calculate route for segment between current point and its predecessor
                    if (pos > 0) {
                        final ArrayList<Float> elevation = new ArrayList<>();
                        final Geopoint[] temp = Routing.getTrackNoCaching(segments.get(pos - 1).getPoint(), segment.getPoint(), elevation);
                        for (final Geopoint geopoint : temp) {
                            segment.addPoint(geopoint);
                        }
                        segment.setElevation(elevation);
                    }
                }
                distance += segment.calculateDistance();
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

    protected Route(final Parcel parcel) {
        name = parcel.readString();
        synchronized (segments) {
            segments.clear();
            final List<RouteSegment> readSegments = parcel.readArrayList(RouteSegment.class.getClassLoader());
            if (readSegments != null) {
                segments.addAll(readSegments);
            }
        }
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
        synchronized (segments) {
            dest.writeList(segments);
        }
        dest.writeInt(routeable ? 1 : 0);
        dest.writeFloat(distance);
        dest.writeInt(isHidden ? 1 : 0);
    }
}
