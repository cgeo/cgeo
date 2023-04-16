package cgeo.geocaching.location;

import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.ICoordinates;
import cgeo.geocaching.models.Waypoint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public final class Viewport {

    @NonNull public final Geopoint center;
    @NonNull public final Geopoint bottomLeft;
    @NonNull public final Geopoint topRight;

    public Viewport(@NonNull final ICoordinates point1, @NonNull final ICoordinates point2) {
        final Geopoint gp1 = point1.getCoords();
        final Geopoint gp2 = point2.getCoords();
        this.bottomLeft = new Geopoint(Math.min(gp1.getLatitude(), gp2.getLatitude()),
                Math.min(gp1.getLongitude(), gp2.getLongitude()));
        this.topRight = new Geopoint(Math.max(gp1.getLatitude(), gp2.getLatitude()),
                Math.max(gp1.getLongitude(), gp2.getLongitude()));
        this.center = new Geopoint((gp1.getLatitude() + gp2.getLatitude()) / 2,
                (gp1.getLongitude() + gp2.getLongitude()) / 2);
    }

    public Viewport(@NonNull final ICoordinates center, final double latSpan, final double lonSpan) {
        this.center = center.getCoords();
        final double centerLat = this.center.getLatitude();
        final double centerLon = this.center.getLongitude();
        final double latHalfSpan = Math.abs(latSpan) / 2;
        final double lonHalfSpan = Math.abs(lonSpan) / 2;
        bottomLeft = new Geopoint(centerLat - latHalfSpan, centerLon - lonHalfSpan);
        topRight = new Geopoint(centerLat + latHalfSpan, centerLon + lonHalfSpan);
    }

    public Viewport(@NonNull final ICoordinates point) {
        center = point.getCoords();
        bottomLeft = point.getCoords();
        topRight = point.getCoords();
    }

    /**
     * Creates a Viewport with given center which covers the area around it with given radius
     */
    public Viewport(@NonNull final ICoordinates center, final float radiusInKilometers) {
        this.center = center.getCoords();
        this.topRight = this.center.project(0, radiusInKilometers).project(90, radiusInKilometers);
        this.bottomLeft = this.center.project(180, radiusInKilometers).project(270, radiusInKilometers);
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

    /**
     * Check whether a point is contained in this viewport.
     *
     * @param point the coordinates to check
     * @return true if the point is contained in this viewport, false otherwise or if the point contains no coordinates
     */
    public boolean contains(@NonNull final ICoordinates point) {
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
    public int count(@NonNull final Collection<? extends ICoordinates> points) {
        int total = 0;
        for (final ICoordinates point : points) {
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
    public <T extends ICoordinates> Collection<T> filter(@NonNull final Collection<T> points) {
        final Collection<T> result = new ArrayList<>();
        for (final T point : points) {
            if (point != null && contains(point)) {
                result.add(point);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "(" + bottomLeft + "," + topRight + ")";
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

    public boolean intersects(@NonNull final Viewport vp) {
        return contains(vp.bottomLeft) || contains(vp.topRight) ||
                contains(vp.bottomLeft.getLatitudeE6(), vp.topRight.getLongitudeE6()) ||
                contains(vp.topRight.getLatitudeE6(), vp.bottomLeft.getLongitudeE6());
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
        return new StringBuilder(prefix).append("latitude >= ").append(doubleToSql(getLatitudeMin())).append(" and ")
                .append(prefix).append("latitude <= ").append(doubleToSql(getLatitudeMax())).append(" and ")
                .append(prefix).append("longitude >= ").append(doubleToSql(getLongitudeMin())).append(" and ")
                .append(prefix).append("longitude <= ").append(doubleToSql(getLongitudeMax()));
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
    public static Viewport containing(final Collection<? extends ICoordinates> points) {
        return containing(points, false, false);
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
        return containing(geocaches, false, true);
    }

    /**
     * Return the smallest viewport containing all given geocaches including all their waypoints
     *
     * @param geocaches a set of geocaches. Geocaches/waypoints with null coordinates (or null themselves) will be ignored
     * @return the smallest viewport containing the non-null coordinates, or null if no coordinates are non-null
     */
    @Nullable
    public static Viewport containingCachesAndWaypoints(final Collection<Geocache> geocaches) {
        return containing(geocaches, true, false);
    }

    /**
     * internal worker function for
     * - containing(Points)
     * - containingGCLiveCaches(Geocaches)
     * - containingCachesAndWaypoints(Geocaches)
     */
    @Nullable
    private static Viewport containing(final Collection<? extends ICoordinates> points, final boolean withWaypoints, final boolean gcLiveOnly) {
        final ContainingViewportBuilder cb = new ContainingViewportBuilder();
        final GCConnector conn = GCConnector.getInstance();
        for (final ICoordinates point : points) {
            if (point != null && (!gcLiveOnly || (conn.canHandle(((Geocache) point).getGeocode()) && !((Geocache) point).inDatabase()))) {
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

    /** Helper class to build Viewports without instanciating too many helper objects */
    public static class ContainingViewportBuilder {
        private boolean valid = false;
        private double latMin = Double.MAX_VALUE;
        private double latMax = -Double.MAX_VALUE;
        private double lonMin = Double.MAX_VALUE;
        private double lonMax = -Double.MAX_VALUE;
        private Viewport viewport = null;

        public ContainingViewportBuilder add(final ICoordinates ... points) {
            if (points != null) {
                for (ICoordinates p : points) {
                    add(p);
                }
            }
            return this;
        }

        public ContainingViewportBuilder add(final Collection<? extends ICoordinates> coll) {
            if (coll != null) {
                for (ICoordinates p : coll) {
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

        private ContainingViewportBuilder add(final ICoordinates point) {
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

    @Override
    public int hashCode() {
        return bottomLeft.hashCode() ^ topRight.hashCode();
    }

}
