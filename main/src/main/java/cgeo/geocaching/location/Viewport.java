package cgeo.geocaching.location;

import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinate;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.JsonUtils;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class Viewport implements Parcelable {

    public static final Viewport EMPTY = new Viewport(0, 0, 0, 0);

    @NonNull public final Geopoint center;
    @NonNull public final Geopoint bottomLeft; //contains the MINIMUM lat and lon
    @NonNull public final Geopoint topRight; // contains the MAXIMUM lat and lon

    public Viewport(final double lat1, final double lon1, final double lat2, final double lon2) {
        this.bottomLeft = new Geopoint(Math.min(lat1, lat2), Math.min(lon1, lon2));
        this.topRight = new Geopoint(Math.max(lat1, lat2), Math.max(lon1, lon2));
        this.center = new Geopoint((lat1 + lat2) / 2, (lon1 + lon2) / 2);
    }

    private Viewport(final int lat1E6, final int lon1E6, final int lat2E6, final int lon2E6, final Object dummy) {
        this.bottomLeft = Geopoint.forE6(Math.min(lat1E6, lat2E6), Math.min(lon1E6, lon2E6));
        this.topRight = Geopoint.forE6(Math.max(lat1E6, lat2E6), Math.max(lon1E6, lon2E6));
        this.center = Geopoint.forE6(((lat1E6 + lat2E6) / 2), ((lon1E6 + lon2E6) / 2));
    }

    public static Viewport forE6(final int lat1E6, final int lon1E6, final int lat2E6, final int lon2E6) {
        return new Viewport(lat1E6, lon1E6, lat2E6, lon2E6, null);
    }

    public Viewport(@NonNull final ICoordinate point1, @NonNull final ICoordinate point2) {
        final Geopoint gp1 = point1.getCoords();
        final Geopoint gp2 = point2.getCoords();
        this.bottomLeft = new Geopoint(Math.min(gp1.getLatitude(), gp2.getLatitude()),
                Math.min(gp1.getLongitude(), gp2.getLongitude()));
        this.topRight = new Geopoint(Math.max(gp1.getLatitude(), gp2.getLatitude()),
                Math.max(gp1.getLongitude(), gp2.getLongitude()));
        this.center = new Geopoint((gp1.getLatitude() + gp2.getLatitude()) / 2,
                (gp1.getLongitude() + gp2.getLongitude()) / 2);
    }

    public Viewport(@NonNull final ICoordinate center, final double latSpan, final double lonSpan) {
        this.center = center.getCoords();
        final double centerLat = this.center.getLatitude();
        final double centerLon = this.center.getLongitude();
        final double latHalfSpan = Math.abs(latSpan) / 2;
        final double lonHalfSpan = Math.abs(lonSpan) / 2;
        bottomLeft = new Geopoint(centerLat - latHalfSpan, centerLon - lonHalfSpan);
        topRight = new Geopoint(centerLat + latHalfSpan, centerLon + lonHalfSpan);
    }

    public Viewport(@NonNull final ICoordinate point) {
        center = point.getCoords();
        bottomLeft = point.getCoords();
        topRight = point.getCoords();
    }

    /**
     * Creates a Viewport with given center which covers the area around it with given radius
     */
    public Viewport(@NonNull final ICoordinate center, final float radiusInKilometers) {
        this.center = center.getCoords();
        this.topRight = this.center.project(0, radiusInKilometers).project(90, radiusInKilometers);
        this.bottomLeft = this.center.project(180, radiusInKilometers).project(270, radiusInKilometers);
    }

    @Nullable
    public static Viewport forJson(final JsonNode node) {
        final Geopoint bottomLeft = Geopoint.forJson(JsonUtils.get(node, "bottomLeft"));
        final Geopoint topRight = Geopoint.forJson(JsonUtils.get(node, "topRight"));
        if (bottomLeft == null || topRight == null) {
            return null;
        }
        return new Viewport(bottomLeft, topRight);
    }

    public ObjectNode toJson() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.set(node, "bottomLeft", bottomLeft.toJson());
        JsonUtils.set(node, "topRight", topRight.toJson());
        return node;
    }

    @Nullable
    public GeoItem toGeoItem(final GeoStyle style, final int zLevel) {
        if (!isValid(this)) {
            return null;
        }
        final List<Geopoint> points = Arrays.asList(getBottomRight(), bottomLeft, getTopLeft(), topRight);
        return GeoPrimitive.createPolygon(points, style).buildUpon().setZLevel(zLevel).build();
    }

    public double getLatitudeMin() {
        return bottomLeft.getLatitude();
    }

    public double getLatitudeMax() {
        return topRight.getLatitude();
    }

    public double getLongitudeMin() {
        return bottomLeft.getLongitude();
    }

    public double getLongitudeMax() {
        return topRight.getLongitude();
    }

    @NonNull
    public Geopoint getCenter() {
        return center;
    }

    public double getLatitudeSpan() {
        return getLatitudeMax() - getLatitudeMin();
    }

    public double getLongitudeSpan() {
        return getLongitudeMax() - getLongitudeMin();
    }

    public Geopoint getBottomRight() {
        return Geopoint.forE6(bottomLeft.getLatitudeE6(), topRight.getLongitudeE6());
    }

    public Geopoint getTopLeft() {
        return Geopoint.forE6(topRight.getLatitudeE6(), bottomLeft.getLongitudeE6());
    }


    /**
     * Check whether a point is contained in this viewport.
     *
     * @param point the coordinates to check
     * @return true if the point is contained in this viewport, false otherwise or if the point contains no coordinates
     */
    public boolean contains(@NonNull final ICoordinate point) {
        final Geopoint coords = point.getCoords();
        return coords != null && contains(coords.getLatitudeE6(), coords.getLongitudeE6());
    }

    public boolean contains(final int latE6, final int lonE6) {
        return lonE6 >= bottomLeft.getLongitudeE6()
                && lonE6 <= topRight.getLongitudeE6()
                && latE6 >= bottomLeft.getLatitudeE6()
                && latE6 <= topRight.getLatitudeE6();
    }

    /**
     * Count the number of points present in the viewport.
     *
     * @param points a collection of (possibly null) points
     * @return the number of non-null points in the viewport
     */
    public int count(@NonNull final Collection<? extends ICoordinate> points) {
        int total = 0;
        for (final ICoordinate point : points) {
            if (point != null && contains(point)) {
                total += 1;
            }
        }
        return total;
    }

    /**
     * Filter return the points present in the viewport.
     *
     * @param points a collection of (possibly null) points
     * @return a new collection containing the points in the viewport
     */
    public <T extends ICoordinate> Collection<T> filter(@NonNull final Collection<T> points) {
        final Collection<T> result = new ArrayList<>();
        for (final T point : points) {
            if (point != null && contains(point)) {
                result.add(point);
            }
        }
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return "Viewport[" + bottomLeft + "," + topRight + "]";
    }

    /**
     * Check whether another viewport is fully included into the current one.
     *
     * @param vp the other viewport
     * @return true if the viewport is fully included into this one, false otherwise
     */
    public boolean includes(@NonNull final Viewport vp) {
        return contains(vp.bottomLeft) && contains(vp.topRight);
    }

    /**
     * Return the "where" part of the string appropriate for a SQL query.
     *
     * @param dbTable the database table to use as prefix, or null if no prefix is required
     * @return the string without the "where" keyword
     */
    @NonNull
    public StringBuilder sqlWhere(@Nullable final String dbTable) {
        final String prefix = dbTable == null ? "" : (dbTable + ".");
        return new StringBuilder(prefix).append(DataStore.dbField_latitude).append(" >= ").append(doubleToSql(getLatitudeMin())).append(" and ")
                .append(prefix).append(DataStore.dbField_latitude).append(" <= ").append(doubleToSql(getLatitudeMax())).append(" and ")
                .append(prefix).append(DataStore.dbField_longitude).append(" >= ").append(doubleToSql(getLongitudeMin())).append(" and ")
                .append(prefix).append(DataStore.dbField_longitude).append(" <= ").append(doubleToSql(getLongitudeMax()));
    }

    private static String doubleToSql(final double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return String.valueOf(value).replace(',', '.');
    }

    /**
     * Return a widened or shrunk viewport.
     *
     * @param factor multiplicative factor for the latitude and longitude span (> 1 to widen, < 1 to shrink)
     * @return a widened or shrunk viewport
     */
    @NonNull
    public Viewport resize(final double factor) {
        return new Viewport(getCenter(), getLatitudeSpan() * factor, getLongitudeSpan() * factor);
    }

    /**
     * Return the smallest viewport containing all the given points.
     *
     * @param points a set of points. Points with null coordinates (or null themselves) will be ignored
     * @return the smallest viewport containing the non-null coordinates, or null if no coordinates are non-null
     */
    @Nullable
    public static Viewport containing(final Collection<? extends ICoordinate> points) {
        return containing(points, false, null);
    }

    /**
     * Return the smallest viewport containing all those of the given geocaches,
     * which are from geocaching.com and not stored in our database
     *
     * @param geocaches a set of geocaches. Geocaches with null coordinates (or null themselves) will be ignored
     * @return the smallest viewport containing the non-null coordinates, or null if no coordinates are non-null
     */
    @Nullable
    public static Viewport containingGCliveCaches(final Collection<Geocache> geocaches) {
        final GCConnector conn = GCConnector.getInstance();
        return containing(geocaches, false, cache -> conn.canHandle(cache.getGeocode()) && !cache.inDatabase());
    }

    /**
     * Return the smallest viewport containing all given geocaches including all their waypoints
     *
     * @param geocaches a set of geocaches. Geocaches/waypoints with null coordinates (or null themselves) will be ignored
     * @return the smallest viewport containing the non-null coordinates, or null if no coordinates are non-null
     */
    @Nullable
    public static Viewport containingCachesAndWaypoints(final Collection<Geocache> geocaches) {
        return containing(geocaches, true, null);
    }

    /**
     * internal worker function for
     * - containing(Points)
     * - containingGCLiveCaches(Geocaches)
     * - containingCachesAndWaypoints(Geocaches)
     */
    @Nullable
    private static <T extends ICoordinate> Viewport containing(final Iterable<T> points, final boolean withWaypoints, final Predicate<T> test) {
        final ContainingViewportBuilder cb = new ContainingViewportBuilder();
        for (final T point : points) {
            if (point != null && (test == null || test.test(point))) {
                cb.add(point);
                if (withWaypoints && ((Geocache) point).hasWaypoints()) {
                    for (final Waypoint waypoint : ((Geocache) point).getWaypoints()) {
                        if (waypoint != null) {
                            cb.add(waypoint.getCoords());
                        }
                    }
                }
            }
        }
        return cb.getViewport();
    }

    public static Viewport intersect(final Viewport vp1, final Viewport vp2) {
        return intersect(Arrays.asList(vp1, vp2));
    }

    public static Viewport intersect(final Iterable<Viewport> vps) {
        return intersect(vps, vp -> vp);
    }

    public static <T> Viewport intersect(final Iterable<T> source, final Function<T, Viewport> mapper) {
        if (source == null) {
            return null;
        }
        int maxLowerLon = Integer.MIN_VALUE;
        int maxLowerLat = Integer.MIN_VALUE;
        int minHigherLon = Integer.MAX_VALUE;
        int minHigherLat = Integer.MAX_VALUE;
        for (T src : source) {
            if (src == null) {
                return null;
            }
            final Viewport vp = mapper.apply(src);
            if (vp == null) {
                return null;
            }
            maxLowerLon = Math.max(maxLowerLon, vp.bottomLeft.getLongitudeE6());
            maxLowerLat = Math.max(maxLowerLat, vp.bottomLeft.getLatitudeE6());
            minHigherLon = Math.min(minHigherLon, vp.topRight.getLongitudeE6());
            minHigherLat = Math.min(minHigherLat, vp.topRight.getLatitudeE6());
        }
        if (maxLowerLon == Integer.MIN_VALUE) {
            return null;
        }
        // There might not be any overlap at all
        if (minHigherLon <= maxLowerLon || minHigherLat <= maxLowerLat) {
            return null;
        }

        return new Viewport(maxLowerLat, maxLowerLon, minHigherLat, minHigherLon, null);
    }

    /** Helper class to build Viewports without instanciating too many helper objects */
    public static class ContainingViewportBuilder {
        private boolean valid = false;
        private double latMin = Double.MAX_VALUE;
        private double latMax = -Double.MAX_VALUE;
        private double lonMin = Double.MAX_VALUE;
        private double lonMax = -Double.MAX_VALUE;
        private Viewport viewport = null;

        public ContainingViewportBuilder add(final ICoordinate... points) {
            if (points != null) {
                for (ICoordinate p : points) {
                    add(p);
                }
            }
            return this;
        }

        public ContainingViewportBuilder add(final Collection<? extends ICoordinate> coll) {
            if (coll != null) {
                for (ICoordinate p : coll) {
                    add(p);
                }
            }
            return this;
        }

        public ContainingViewportBuilder add(final Viewport vp) {
            if (vp != null) {
                add(vp.bottomLeft);
                add(vp.topRight);
            }
            return this;
        }

        private ContainingViewportBuilder add(final ICoordinate point) {
            if (point == null) {
                return this;
            }
            viewport = null;
            final Geopoint coords = point.getCoords();
            if (coords != null) {
                valid = true;
                final double latitude = coords.getLatitude();
                final double longitude = coords.getLongitude();
                latMin = Math.min(latMin, latitude);
                latMax = Math.max(latMax, latitude);
                lonMin = Math.min(lonMin, longitude);
                lonMax = Math.max(lonMax, longitude);
            }
            return this;
        }

        public Viewport getViewport() {
            if (viewport == null && valid) {
                viewport = new Viewport(new Geopoint(latMin, lonMin), new Geopoint(latMax, lonMax));
            }
            return viewport;
        }


    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Viewport)) {
            return false;
        }
        final Viewport vp = (Viewport) other;
        return bottomLeft.equals(vp.bottomLeft) && topRight.equals(vp.topRight);
    }

    public boolean isJustADot() {
        return bottomLeft.equals(topRight);
    }

    public static boolean isValid(final Viewport viewport) {
        return viewport != null && !viewport.isJustADot();
    }

    @Override
    public int hashCode() {
        return bottomLeft.hashCode() ^ topRight.hashCode();
    }

    // Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull final Parcel dest, final int flags) {
        dest.writeParcelable(center, flags);
        dest.writeParcelable(bottomLeft, flags);
        dest.writeParcelable(topRight, flags);
    }

    Viewport(final Parcel in) {
        center = Objects.requireNonNull(in.readParcelable(Geopoint.class.getClassLoader()));
        bottomLeft = Objects.requireNonNull(in.readParcelable(Geopoint.class.getClassLoader()));
        topRight = Objects.requireNonNull(in.readParcelable(Geopoint.class.getClassLoader()));
    }

    public static final Creator<Viewport> CREATOR = new Creator<Viewport>() {
        @Override
        public Viewport createFromParcel(final Parcel in) {
            return new Viewport(in);
        }

        @Override
        public Viewport[] newArray(final int size) {
            return new Viewport[size];
        }
    };

}
