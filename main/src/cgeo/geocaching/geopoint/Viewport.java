package cgeo.geocaching.geopoint;



public class Viewport {

    public final Geopoint center;
    public final Geopoint bottomLeft;
    public final Geopoint topRight;

    public Viewport(final Geopoint gp1, final Geopoint gp2) {
        this.bottomLeft = new Geopoint(Math.min(gp1.getLatitude(), gp2.getLatitude()),
                Math.min(gp1.getLongitude(), gp2.getLongitude()));
        this.topRight = new Geopoint(Math.max(gp1.getLatitude(), gp2.getLatitude()),
                Math.max(gp1.getLongitude(), gp2.getLongitude()));
        this.center = new Geopoint((gp1.getLatitude() + gp2.getLatitude()) / 2,
                (gp1.getLongitude() + gp2.getLongitude()) / 2);
    }

    public Viewport(final Geopoint center, final double latSpan, final double lonSpan) {
        this.center = center;
        final double centerLat = center.getLatitude();
        final double centerLon = center.getLongitude();
        final double latHalfSpan = Math.abs(latSpan) / 2;
        final double lonHalfSpan = Math.abs(lonSpan) / 2;
        bottomLeft = new Geopoint(centerLat - latHalfSpan, centerLon - lonHalfSpan);
        topRight = new Geopoint(centerLat + latHalfSpan, centerLon + lonHalfSpan);
    }

    public Viewport(final double lat1, final double lat2, final double lon1, final double lon2) {
        this(new Geopoint(lat1, lon1), new Geopoint(lat2, lon2));
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

    public Geopoint getCenter() {
        return center;
    }

    public double getLatitudeSpan() {
        return getLatitudeMax() - getLatitudeMin();
    }

    public double getLongitudeSpan() {
        return getLongitudeMax() - getLongitudeMin();
    }

    public boolean isInViewport(final Geopoint coords) {

        return coords.getLongitudeE6() >= bottomLeft.getLongitudeE6()
                && coords.getLongitudeE6() <= topRight.getLongitudeE6()
                && coords.getLatitudeE6() >= bottomLeft.getLatitudeE6()
                && coords.getLatitudeE6() <= topRight.getLatitudeE6();
    }

    @Override
    public String toString() {
        return "(" + bottomLeft.toString() + "," + topRight.toString() + ")";
    }

    /**
     * Check whether another viewport is fully included into the current one.
     *
     * @param vp
     *            the other viewport
     * @return true if the vp is fully included into this one, false otherwise
     */
    public boolean includes(final Viewport vp) {
        return isInViewport(vp.bottomLeft) && isInViewport(vp.topRight);
    }

    /**
     * Check if coordinates are located in a viewport (defined by its center and span
     * in each direction).
     *
     * @param centerLat
     *            the viewport center latitude
     * @param centerLon
     *            the viewport center longitude
     * @param spanLat
     *            the latitude span
     * @param spanLon
     *            the longitude span
     * @param coords
     *            the coordinates to check
     * @return true if the coordinates are in the viewport
     */
    // FIXME: this static method has nothing to do here and should be used with a viewport, not some int numbers,
    // when CGeoMap.java gets rewritten
    public static boolean isCacheInViewPort(int centerLat, int centerLon, int spanLat, int spanLon, final Geopoint coords) {
        final Viewport viewport = new Viewport(new Geopoint(centerLat / 1e6, centerLon / 1e6), spanLat / 1e6, spanLon / 1e6);
        return viewport.isInViewport(coords);
    }

    /**
     * Check if an area is located in a viewport (defined by its center and span
     * in each direction).
     *
     * expects coordinates in E6 format
     *
     * @param centerLat1
     * @param centerLon1
     * @param centerLat2
     * @param centerLon2
     * @param spanLat1
     * @param spanLon1
     * @param spanLat2
     * @param spanLon2
     * @return
     */
    // FIXME: this static method has nothing to do here and should be used with a viewport, not some int numbers,
    // when CGeoMap.java gets rewritten
    public static boolean isInViewPort(int centerLat1, int centerLon1, int centerLat2, int centerLon2, int spanLat1, int spanLon1, int spanLat2, int spanLon2) {
        final Viewport outer = new Viewport(new Geopoint(centerLat1 / 1e6, centerLon1 / 1e6), spanLat1 / 1e6, spanLon1 / 1e6);
        final Viewport inner = new Viewport(new Geopoint(centerLat2 / 1e6, centerLon2 / 1e6), spanLat2 / 1e6, spanLon2 / 1e6);
        return outer.includes(inner);
    }

    /**
     * Return the "where" part of the string appropriate for a SQL query.
     *
     * @return the string without the "where" keyword
     */
    public String sqlWhere() {
        return "latitude >= " + getLatitudeMin() + " and " +
                "latitude <= " + getLatitudeMax() + " and " +
                "longitude >= " + getLongitudeMin() + " and " +
                "longitude <= " + getLongitudeMax();
    }

    /**
     * Return a widened or shrunk viewport.
     *
     * @param factor
     *            multiplicative factor for the latitude and longitude span (> 1 to widen, < 1 to shrink)
     * @return a widened or shrunk viewport
     */
    public Viewport resize(final double factor) {
        return new Viewport(getCenter(), getLatitudeSpan() * factor, getLongitudeSpan() * factor);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null || !(other instanceof Viewport)) {
            return false;
        }
        final Viewport vp = (Viewport) other;
        return bottomLeft.equals(vp.bottomLeft) && topRight.equals(vp.topRight);
    }

    @Override
    public int hashCode() {
        return bottomLeft.hashCode() ^ topRight.hashCode();
    }
}
