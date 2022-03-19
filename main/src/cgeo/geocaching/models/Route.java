package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.routing.Routing;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import com.google.android.gms.maps.model.LatLng;
import org.oscim.core.GeoPoint;

public class Route implements Parcelable {
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
        void updateRoute(Route route);
    }

    public void setName(final String name) {
        this.name = name;
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

    public ArrayList<ArrayList<Geopoint>> getAllPoints() {
        final ArrayList<ArrayList<Geopoint>> allPoints = new ArrayList<>();
        if (segments != null) {
            for (RouteSegment segment : segments) {
                // extend existing list of points, if linking of segments is requested - otherwise add new segment
                if (allPoints.size() > 0 && segment.getLinkToPreviousSegment()) {
                    allPoints.get(allPoints.size() - 1).addAll(segment.getPoints());
                } else {
                    allPoints.add(new ArrayList<>(segment.getPoints()));
                }
            }
        }
        return allPoints;
    }

    public ArrayList<ArrayList<LatLng>> getAllPointsLatLng() {
        final ArrayList<ArrayList<LatLng>> allPoints = new ArrayList<>();
        if (segments != null) {
            for (RouteSegment segment : segments) {
                // convert to list of LatLng
                final ArrayList<LatLng> points = new ArrayList<>();
                for (Geopoint point : segment.getPoints()) {
                    points.add(new LatLng(point.getLatitude(), point.getLongitude()));
                }
                // extend existing list of points, if linking of segments is requested - otherwise add new segment
                if (allPoints.size() > 0 && segment.getLinkToPreviousSegment()) {
                    allPoints.get(allPoints.size() - 1).addAll(points);
                } else {
                    allPoints.add(points);
                }
            }
        }
        return allPoints;
    }

    public ArrayList<ArrayList<GeoPoint>> getAllPointsGeoPoint() {
        final ArrayList<ArrayList<GeoPoint>> allPoints = new ArrayList<>();
        if (segments != null) {
            for (RouteSegment segment : segments) {
                // convert to list of GeoPoint
                final ArrayList<GeoPoint> points = new ArrayList<>();
                for (Geopoint point : segment.getPoints()) {
                    points.add(new GeoPoint(point.getLatitude(), point.getLongitude()));
                }
                // extend existing list of points, if linking of segments is requested - otherwise add new segment
                if (allPoints.size() > 0 && segment.getLinkToPreviousSegment()) {
                    allPoints.get(allPoints.size() - 1).addAll(points);
                } else {
                    allPoints.add(points);
                }
            }
        }
        return allPoints;
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
