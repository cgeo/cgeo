package cgeo.geocaching.geopoint;

import android.location.Location;

/**
 * Abstraction of geographic point.
 */
public final class Geopoint
{
    public static final double deg2rad = Math.PI / 180;
    public static final double rad2deg = 180 / Math.PI;
    public static final float erad = 6371.0f;

    private final double latitude;
    private final double longitude;

    /**
     * Creates new Geopoint with given latitude and longitude (both degree).
     *
     * @param lat
     *            latitude
     * @param lon
     *            longitude
     * @throws MalformedCoordinateException
     *             if any coordinate is incorrect
     */
    public Geopoint(final double lat, final double lon)
    {
        if (lat <= 90 && lat >= -90) {
            latitude = lat;
        } else {
            throw new MalformedCoordinateException("malformed latitude: " + lat);
        }
        if (lon <= 180 && lon >= -180) {
            // Prefer 180 degrees rather than the equivalent -180.
            longitude = lon == -180 ? 180 : lon;
        } else {
            throw new MalformedCoordinateException("malformed longitude: " + lon);
        }
    }

    /**
     * Creates new Geopoint with given latitude and longitude (both microdegree).
     *
     * @param lat
     *            latitude
     * @param lon
     *            longitude
     * @throws MalformedCoordinateException
     *             if any coordinate is incorrect
     */
    public Geopoint(final int lat, final int lon)
    {
        this(lat / 1e6, lon / 1e6);
    }

    /**
     * Creates new Geopoint with latitude and longitude parsed from string.
     *
     * @param text
     *            string to parse
     * @throws GeopointParser.ParseException
     *             if the string cannot be parsed
     * @throws MalformedCoordinateException
     *             if any coordinate is incorrect
     * @see GeopointParser.parse()
     */
    public Geopoint(final String text) {
        this(GeopointParser.parseLatitude(text), GeopointParser.parseLongitude(text));
    }

    /**
     * Creates new Geopoint with latitude and longitude parsed from strings.
     * 
     * @param latText
     *            latitude string to parse
     * @param lonText
     *            longitude string to parse
     * @throws GeopointParser.ParseException
     *             if any argument string cannot be parsed
     * @throws MalformedCoordinateException
     *             if any coordinate is incorrect
     * @see GeopointParser.parse()
     */
    public Geopoint(final String latText, final String lonText) {
        this(GeopointParser.parseLatitude(latText), GeopointParser.parseLongitude(lonText));
    }

    /**
     * Creates new Geopoint with given Location.
     *
     * @param gp
     *            the Location to clone
     */
    public Geopoint(final Location loc) {
        this(loc.getLatitude(), loc.getLongitude());
    }

    /**
     * Get latitude in degree.
     *
     * @return latitude
     */
    public double getLatitude()
    {
        return latitude;
    }

    /**
     * Get latitude in microdegree.
     *
     * @return latitude
     */
    public int getLatitudeE6()
    {
        return (int) (latitude * 1E6);
    }

    /**
     * Get longitude in degree.
     *
     * @return longitude
     */
    public double getLongitude()
    {
        return longitude;
    }

    /**
     * Get longitude in microdegree.
     *
     * @return longitude
     */
    public int getLongitudeE6()
    {
        return (int) (longitude * 1E6);
    }

    /**
     * Get distance and bearing from the current point to a target.
     *
     * @param target
     *            The target
     * @return An array of floats: the distance in meters, then the bearing in degrees
     */
    private float[] pathTo(final Geopoint target) {
        float[] results = new float[2];
        android.location.Location.distanceBetween(getLatitude(), getLongitude(), target.getLatitude(), target.getLongitude(), results);
        return results;
    }

    /**
     * Calculates distance to given Geopoint in km.
     *
     * @param gp
     *            target
     * @return distance in km
     * @throws GeopointException
     *             if there is an error in distance calculation
     */
    public float distanceTo(final Geopoint gp)
    {
        return pathTo(gp)[0] / 1000;
    }

    /**
     * Calculates bearing to given Geopoint in degree.
     *
     * @param gp
     *            target
     * @return bearing in degree, in the [0,360[ range
     */
    public float bearingTo(final Geopoint gp)
    {
        // Android library returns a bearing in the [-180;180] range
        final float bearing = pathTo(gp)[1];
        return bearing < 0 ? bearing + 360 : bearing;
    }

    /**
     * Calculates geopoint from given bearing and distance.
     *
     * @param bearing
     *            bearing in degree
     * @param distance
     *            distance in km
     * @return the projected geopoint
     */
    public Geopoint project(final double bearing, final double distance)
    {
        final double rlat1 = latitude * deg2rad;
        final double rlon1 = longitude * deg2rad;
        final double rbearing = bearing * deg2rad;
        final double rdistance = distance / erad;

        final double rlat = Math.asin(Math.sin(rlat1) * Math.cos(rdistance) + Math.cos(rlat1) * Math.sin(rdistance) * Math.cos(rbearing));
        final double rlon = rlon1 + Math.atan2(Math.sin(rbearing) * Math.sin(rdistance) * Math.cos(rlat1), Math.cos(rdistance) - Math.sin(rlat1) * Math.sin(rlat));

        return new Geopoint(rlat * rad2deg, rlon * rad2deg);
    }

    /**
     * Checks if given Geopoint is identical with this Geopoint.
     *
     * @param gp
     *            Geopoint to check
     * @return true if identical, false otherwise
     */
    public boolean isEqualTo(Geopoint gp)
    {
        return null != gp && gp.getLatitude() == latitude && gp.getLongitude() == longitude;
    }

    /**
     * Checks if given Geopoint is similar to this Geopoint with tolerance.
     *
     * @param gp
     *            Geopoint to check
     * @param tolerance
     *            tolerance in km
     * @return true if similar, false otherwise
     */
    public boolean isEqualTo(Geopoint gp, double tolerance)
    {
        return null != gp && distanceTo(gp) <= tolerance;
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format
     *            the desired format
     * @see GeopointFormatter
     * @return formatted coordinates
     */
    public String format(GeopointFormatter format)
    {
        return format.format(this);
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format
     *            the desired format
     * @see GeopointFormatter
     * @return formatted coordinates
     */
    public String format(String format)
    {
        return GeopointFormatter.format(format, this);
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format
     *            the desired format
     * @see GeopointFormatter
     * @return formatted coordinates
     */
    public String format(GeopointFormatter.Format format)
    {
        return GeopointFormatter.format(format, this);
    }

    /**
     * Returns formatted coordinates with default format.
     * Default format is decimalminutes, e.g. N 52° 36.123 E 010° 03.456
     *
     * @return formatted coordinates
     */
    public String toString()
    {
        return format(GeopointFormatter.Format.LAT_LON_DECMINUTE);
    }

    abstract public static class GeopointException
            extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public GeopointException(String msg)
        {
            super(msg);
        }
    }

    public static class MalformedCoordinateException
            extends GeopointException
    {
        private static final long serialVersionUID = 1L;

        public MalformedCoordinateException(String msg)
        {
            super(msg);
        }
    }
}
