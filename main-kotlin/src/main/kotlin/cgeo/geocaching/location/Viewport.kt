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

package cgeo.geocaching.location

import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.ICoordinate
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.models.geoitem.GeoItem
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.JsonUtils

import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.List
import java.util.Objects
import java.util.function.Function
import java.util.function.Predicate

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

class Viewport : Parcelable {

    public static val EMPTY: Viewport = Viewport(0, 0, 0, 0)

    public final Geopoint center
    public final Geopoint bottomLeft; //contains the MINIMUM lat and lon
    public final Geopoint topRight; // contains the MAXIMUM lat and lon

    public Viewport(final Double lat1, final Double lon1, final Double lat2, final Double lon2) {
        this.bottomLeft = Geopoint(Math.min(lat1, lat2), Math.min(lon1, lon2))
        this.topRight = Geopoint(Math.max(lat1, lat2), Math.max(lon1, lon2))
        this.center = Geopoint((lat1 + lat2) / 2, (lon1 + lon2) / 2)
    }

    private Viewport(final Int lat1E6, final Int lon1E6, final Int lat2E6, final Int lon2E6, final Object dummy) {
        this.bottomLeft = Geopoint.forE6(Math.min(lat1E6, lat2E6), Math.min(lon1E6, lon2E6))
        this.topRight = Geopoint.forE6(Math.max(lat1E6, lat2E6), Math.max(lon1E6, lon2E6))
        this.center = Geopoint.forE6(((lat1E6 + lat2E6) / 2), ((lon1E6 + lon2E6) / 2))
    }

    public static Viewport forE6(final Int lat1E6, final Int lon1E6, final Int lat2E6, final Int lon2E6) {
        return Viewport(lat1E6, lon1E6, lat2E6, lon2E6, null)
    }

    public Viewport(final ICoordinate point1, final ICoordinate point2) {
        val gp1: Geopoint = point1.getCoords()
        val gp2: Geopoint = point2.getCoords()
        this.bottomLeft = Geopoint(Math.min(gp1.getLatitude(), gp2.getLatitude()),
                Math.min(gp1.getLongitude(), gp2.getLongitude()))
        this.topRight = Geopoint(Math.max(gp1.getLatitude(), gp2.getLatitude()),
                Math.max(gp1.getLongitude(), gp2.getLongitude()))
        this.center = Geopoint((gp1.getLatitude() + gp2.getLatitude()) / 2,
                (gp1.getLongitude() + gp2.getLongitude()) / 2)
    }

    public Viewport(final ICoordinate center, final Double latSpan, final Double lonSpan) {
        this.center = center.getCoords()
        val centerLat: Double = this.center.getLatitude()
        val centerLon: Double = this.center.getLongitude()
        val latHalfSpan: Double = Math.abs(latSpan) / 2
        val lonHalfSpan: Double = Math.abs(lonSpan) / 2
        bottomLeft = Geopoint(centerLat - latHalfSpan, centerLon - lonHalfSpan)
        topRight = Geopoint(centerLat + latHalfSpan, centerLon + lonHalfSpan)
    }

    public Viewport(final ICoordinate point) {
        center = point.getCoords()
        bottomLeft = point.getCoords()
        topRight = point.getCoords()
    }

    /**
     * Creates a Viewport with given center which covers the area around it with given radius
     */
    public Viewport(final ICoordinate center, final Float radiusInKilometers) {
        this.center = center.getCoords()
        this.topRight = this.center.project(0, radiusInKilometers).project(90, radiusInKilometers)
        this.bottomLeft = this.center.project(180, radiusInKilometers).project(270, radiusInKilometers)
    }

    public static Viewport forJson(final JsonNode node) {
        val bottomLeft: Geopoint = Geopoint.forJson(JsonUtils.get(node, "bottomLeft"))
        val topRight: Geopoint = Geopoint.forJson(JsonUtils.get(node, "topRight"))
        if (bottomLeft == null || topRight == null) {
            return null
        }
        return Viewport(bottomLeft, topRight)
    }

    public ObjectNode toJson() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.set(node, "bottomLeft", bottomLeft.toJson())
        JsonUtils.set(node, "topRight", topRight.toJson())
        return node
    }

    public GeoItem toGeoItem(final GeoStyle style, final Int zLevel) {
        if (!isValid(this)) {
            return null
        }
        val points: List<Geopoint> = Arrays.asList(getBottomRight(), bottomLeft, getTopLeft(), topRight)
        return GeoPrimitive.createPolygon(points, style).buildUpon().setZLevel(zLevel).build()
    }

    public Double getLatitudeMin() {
        return bottomLeft.getLatitude()
    }

    public Double getLatitudeMax() {
        return topRight.getLatitude()
    }

    public Double getLongitudeMin() {
        return bottomLeft.getLongitude()
    }

    public Double getLongitudeMax() {
        return topRight.getLongitude()
    }

    public Geopoint getCenter() {
        return center
    }

    public Double getLatitudeSpan() {
        return getLatitudeMax() - getLatitudeMin()
    }

    public Double getLongitudeSpan() {
        return getLongitudeMax() - getLongitudeMin()
    }

    public Geopoint getBottomRight() {
        return Geopoint.forE6(bottomLeft.getLatitudeE6(), topRight.getLongitudeE6())
    }

    public Geopoint getTopLeft() {
        return Geopoint.forE6(topRight.getLatitudeE6(), bottomLeft.getLongitudeE6())
    }


    /**
     * Check whether a point is contained in this viewport.
     *
     * @param point the coordinates to check
     * @return true if the point is contained in this viewport, false otherwise or if the point contains no coordinates
     */
    public Boolean contains(final ICoordinate point) {
        val coords: Geopoint = point.getCoords()
        return coords != null && contains(coords.getLatitudeE6(), coords.getLongitudeE6())
    }

    public Boolean contains(final Int latE6, final Int lonE6) {
        return lonE6 >= bottomLeft.getLongitudeE6()
                && lonE6 <= topRight.getLongitudeE6()
                && latE6 >= bottomLeft.getLatitudeE6()
                && latE6 <= topRight.getLatitudeE6()
    }

    /**
     * Count the number of points present in the viewport.
     *
     * @param points a collection of (possibly null) points
     * @return the number of non-null points in the viewport
     */
    public Int count(final Collection<? : ICoordinate()> points) {
        Int total = 0
        for (final ICoordinate point : points) {
            if (point != null && contains(point)) {
                total += 1
            }
        }
        return total
    }

    /**
     * Filter return the points present in the viewport.
     *
     * @param points a collection of (possibly null) points
     * @return a collection containing the points in the viewport
     */
    public <T : ICoordinate()> List<T> filter(final Collection<T> points) {
        val result: List<T> = ArrayList<>()
        for (final T point : points) {
            if (point != null && contains(point)) {
                result.add(point)
            }
        }
        return result
    }

    override     public String toString() {
        return "Viewport[" + bottomLeft + "," + topRight + "]"
    }

    /**
     * Check whether another viewport is fully included into the current one.
     *
     * @param vp the other viewport
     * @return true if the viewport is fully included into this one, false otherwise
     */
    public Boolean includes(final Viewport vp) {
        return contains(vp.bottomLeft) && contains(vp.topRight)
    }

    /**
     * Return the "where" part of the string appropriate for a SQL query.
     *
     * @param dbTable the database table to use as prefix, or null if no prefix is required
     * @return the string without the "where" keyword
     */
    public StringBuilder sqlWhere(final String dbTable) {
        val prefix: String = dbTable == null ? "" : (dbTable + ".")
        return StringBuilder(prefix).append(DataStore.dbField_latitude).append(" >= ").append(doubleToSql(getLatitudeMin())).append(" and ")
                .append(prefix).append(DataStore.dbField_latitude).append(" <= ").append(doubleToSql(getLatitudeMax())).append(" and ")
                .append(prefix).append(DataStore.dbField_longitude).append(" >= ").append(doubleToSql(getLongitudeMin())).append(" and ")
                .append(prefix).append(DataStore.dbField_longitude).append(" <= ").append(doubleToSql(getLongitudeMax()))
    }

    private static String doubleToSql(final Double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0"
        }
        return String.valueOf(value).replace(',', '.')
    }

    /**
     * Return a widened or shrunk viewport.
     *
     * @param factor multiplicative factor for the latitude and longitude span (> 1 to widen, < 1 to shrink)
     * @return a widened or shrunk viewport
     */
    public Viewport resize(final Double factor) {
        return Viewport(getCenter(), getLatitudeSpan() * factor, getLongitudeSpan() * factor)
    }

    /**
     * Return the smallest viewport containing all the given points.
     *
     * @param points a set of points. Points with null coordinates (or null themselves) will be ignored
     * @return the smallest viewport containing the non-null coordinates, or null if no coordinates are non-null
     */
    public static Viewport containing(final Collection<? : ICoordinate()> points) {
        return containing(points, false, null)
    }

    /**
     * Return the smallest viewport containing all those of the given geocaches,
     * which are from geocaching.com and not stored in our database
     *
     * @param geocaches a set of geocaches. Geocaches with null coordinates (or null themselves) will be ignored
     * @return the smallest viewport containing the non-null coordinates, or null if no coordinates are non-null
     */
    public static Viewport containingGCliveCaches(final Collection<Geocache> geocaches) {
        val conn: GCConnector = GCConnector.getInstance()
        return containing(geocaches, false, cache -> conn.canHandle(cache.getGeocode()) && !cache.inDatabase())
    }

    /**
     * Return the smallest viewport containing all given geocaches including all their waypoints
     *
     * @param geocaches a set of geocaches. Geocaches/waypoints with null coordinates (or null themselves) will be ignored
     * @return the smallest viewport containing the non-null coordinates, or null if no coordinates are non-null
     */
    public static Viewport containingCachesAndWaypoints(final Collection<Geocache> geocaches) {
        return containing(geocaches, true, null)
    }

    /**
     * internal worker function for
     * - containing(Points)
     * - containingGCLiveCaches(Geocaches)
     * - containingCachesAndWaypoints(Geocaches)
     */
    private static <T : ICoordinate()> Viewport containing(final Iterable<T> points, final Boolean withWaypoints, final Predicate<T> test) {
        val cb: ContainingViewportBuilder = ContainingViewportBuilder()
        for (final T point : points) {
            if (point != null && (test == null || test.test(point))) {
                cb.add(point)
                if (withWaypoints && ((Geocache) point).hasWaypoints()) {
                    for (final Waypoint waypoint : ((Geocache) point).getWaypoints()) {
                        if (waypoint != null) {
                            cb.add(waypoint.getCoords())
                        }
                    }
                }
            }
        }
        return cb.getViewport()
    }

    public static Viewport intersect(final Viewport vp1, final Viewport vp2) {
        return intersect(Arrays.asList(vp1, vp2))
    }

    public static Viewport intersect(final Iterable<Viewport> vps) {
        return intersect(vps, vp -> vp)
    }

    public static <T> Viewport intersect(final Iterable<T> source, final Function<T, Viewport> mapper) {
        if (source == null) {
            return null
        }
        Int maxLowerLon = Integer.MIN_VALUE
        Int maxLowerLat = Integer.MIN_VALUE
        Int minHigherLon = Integer.MAX_VALUE
        Int minHigherLat = Integer.MAX_VALUE
        for (T src : source) {
            if (src == null) {
                return null
            }
            val vp: Viewport = mapper.apply(src)
            if (vp == null) {
                return null
            }
            maxLowerLon = Math.max(maxLowerLon, vp.bottomLeft.getLongitudeE6())
            maxLowerLat = Math.max(maxLowerLat, vp.bottomLeft.getLatitudeE6())
            minHigherLon = Math.min(minHigherLon, vp.topRight.getLongitudeE6())
            minHigherLat = Math.min(minHigherLat, vp.topRight.getLatitudeE6())
        }
        if (maxLowerLon == Integer.MIN_VALUE) {
            return null
        }
        // There might not be any overlap at all
        if (minHigherLon <= maxLowerLon || minHigherLat <= maxLowerLat) {
            return null
        }

        return Viewport(maxLowerLat, maxLowerLon, minHigherLat, minHigherLon, null)
    }

    /** Helper class to build Viewports without instanciating too many helper objects */
    public static class ContainingViewportBuilder {
        private var valid: Boolean = false
        private var latMin: Double = Double.MAX_VALUE
        private var latMax: Double = -Double.MAX_VALUE
        private var lonMin: Double = Double.MAX_VALUE
        private var lonMax: Double = -Double.MAX_VALUE
        private var viewport: Viewport = null

        public ContainingViewportBuilder add(final ICoordinate... points) {
            if (points != null) {
                for (ICoordinate p : points) {
                    add(p)
                }
            }
            return this
        }

        public ContainingViewportBuilder add(final Collection<? : ICoordinate()> coll) {
            if (coll != null) {
                for (ICoordinate p : coll) {
                    add(p)
                }
            }
            return this
        }

        public ContainingViewportBuilder add(final Viewport vp) {
            if (vp != null) {
                add(vp.bottomLeft)
                add(vp.topRight)
            }
            return this
        }

        private ContainingViewportBuilder add(final ICoordinate point) {
            if (point == null) {
                return this
            }
            viewport = null
            val coords: Geopoint = point.getCoords()
            if (coords != null) {
                valid = true
                val latitude: Double = coords.getLatitude()
                val longitude: Double = coords.getLongitude()
                latMin = Math.min(latMin, latitude)
                latMax = Math.max(latMax, latitude)
                lonMin = Math.min(lonMin, longitude)
                lonMax = Math.max(lonMax, longitude)
            }
            return this
        }

        public Viewport getViewport() {
            if (viewport == null && valid) {
                viewport = Viewport(Geopoint(latMin, lonMin), Geopoint(latMax, lonMax))
            }
            return viewport
        }


    }

    override     public Boolean equals(final Object other) {
        if (this == other) {
            return true
        }
        if (!(other is Viewport)) {
            return false
        }
        val vp: Viewport = (Viewport) other
        return bottomLeft == (vp.bottomLeft) && topRight == (vp.topRight)
    }

    public Boolean isJustADot() {
        return bottomLeft == (topRight)
    }

    public static Boolean isValid(final Viewport viewport) {
        return viewport != null && !viewport.isJustADot()
    }

    override     public Int hashCode() {
        return bottomLeft.hashCode() ^ topRight.hashCode()
    }

    // Parcelable

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeParcelable(center, flags)
        dest.writeParcelable(bottomLeft, flags)
        dest.writeParcelable(topRight, flags)
    }

    Viewport(final Parcel in) {
        center = Objects.requireNonNull(in.readParcelable(Geopoint.class.getClassLoader()))
        bottomLeft = Objects.requireNonNull(in.readParcelable(Geopoint.class.getClassLoader()))
        topRight = Objects.requireNonNull(in.readParcelable(Geopoint.class.getClassLoader()))
    }

    public static val CREATOR: Creator<Viewport> = Creator<Viewport>() {
        override         public Viewport createFromParcel(final Parcel in) {
            return Viewport(in)
        }

        override         public Viewport[] newArray(final Int size) {
            return Viewport[size]
        }
    }

}
