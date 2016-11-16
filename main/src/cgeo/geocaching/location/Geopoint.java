package cgeo.geocaching.location;

import cgeo.geocaching.R;
import cgeo.geocaching.models.ICoordinates;

import android.location.Location;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

/**
 * Abstraction of geographic point. This class is immutable.
 */
public final class Geopoint implements ICoordinates, Parcelable {
    /**
     * Reusable default object
     */
    @NonNull public static final Geopoint ZERO = new Geopoint(0.0, 0.0);

    private static final double DEG_TO_RAD = Math.PI / 180;
    private static final double RAD_TO_DEG = 180 / Math.PI;
    private static final float EARTH_RADIUS = 6371.0f;
    /**
     * JIT bug in Android 4.2.1
     */
    private static final boolean DISTANCE_BROKEN = Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1;

    private final int latitudeE6;
    private final int longitudeE6;

    /**
     * Creates new Geopoint with given latitude and longitude (both degree).
     *
     * @param lat
     *            latitude
     * @param lon
     *            longitude
     */
    public Geopoint(final double lat, final double lon) {
        latitudeE6 = (int) Math.round(lat * 1e6);
        longitudeE6 = (int) Math.round(lon * 1e6);
    }

    /**
     * Creates new Geopoint with given latitude and longitude in microdegrees.
     * The <tt>dummy</tt> parameter is ignored and is only used to prevent the wrong
     * constructor from being used.
     *
     * @param latE6 latitude in microdegrees
     * @param lonE6 longitude in microdegrees
     * @param dummy ignored parameter
     */
    private Geopoint(final int latE6, final int lonE6, final Object dummy) {
        latitudeE6 = latE6;
        longitudeE6 = lonE6;
    }

    /**
     * Creates new Geopoint with latitude and longitude parsed from string.
     *
     * @param text
     *            string to parse
     * @throws Geopoint.ParseException
     *             if the string cannot be parsed
     * @see GeopointParser#parse(String)
     */
    public Geopoint(@NonNull final String text) {
        final Geopoint parsed = GeopointParser.parse(text);
        latitudeE6 = parsed.latitudeE6;
        longitudeE6 = parsed.longitudeE6;
    }

    /**
     * Creates new Geopoint with latitude and longitude parsed from strings.
     *
     * @param latText
     *            latitude string to parse
     * @param lonText
     *            longitude string to parse
     * @throws Geopoint.ParseException
     *             if any argument string cannot be parsed
     */
    public Geopoint(final String latText, final String lonText) {
        this(GeopointParser.parseLatitude(latText), GeopointParser.parseLongitude(lonText));
    }

    /**
     * Creates new Geopoint with given Location.
     *
     * @param loc
     *            the Location to clone
     */
    public Geopoint(final Location loc) {
        this(loc.getLatitude(), loc.getLongitude());
    }

    /**
     * Create new Geopoint from Parcel.
     *
     * @param in
     *            a Parcel to read the saved data from
     */
    public Geopoint(final Parcel in) {
        latitudeE6 = in.readInt();
        longitudeE6 = in.readInt();
    }

    /**
     * Create new Geopoint from individual textual components.
     *
     */
    public Geopoint(final String latDir, final String latDeg, final String latDegFrac,
                    final String lonDir, final String lonDeg, final String lonDegFrac) {
        this(getLatSign(latDir) + latDeg + "." + addZeros(latDegFrac, 5),
                getLonSign(lonDir) + lonDeg + "." + addZeros(lonDegFrac, 5));
    }

    /**
     * Create new Geopoint from individual textual components.
     *
     */
    public Geopoint(final String latDir, final String latDeg, final String latMin, final String latMinFrac,
                    final String lonDir, final String lonDeg, final String lonMin, final String lonMinFrac) {
        this(latDir + " " + latDeg + " " + latMin + "." + addZeros(latMinFrac, 3),
                lonDir + " " + lonDeg + " " + lonMin + "." + addZeros(lonMinFrac, 3));
    }

    /**
     * Create new Geopoint from individual textual components.
     *
     */
    public Geopoint(final String latDir, final String latDeg, final String latMin, final String latSec, final String latSecFrac,
            final String lonDir, final String lonDeg, final String lonMin, final String lonSec, final String lonSecFrac) {
        this(latDir + " " + latDeg + " " + latMin + " " + latSec + "." + addZeros(latSecFrac, 3),
                lonDir + " " + lonDeg + " " + lonMin + " " + lonSec + "." + addZeros(lonSecFrac, 3));
    }

    /**
     * Get latitude in degree.
     *
     * @return latitude
     */
    public double getLatitude() {
        return latitudeE6 / 1e6;
    }

    /**
     * Get latitude in microdegree.
     *
     * @return latitude
     */
    public int getLatitudeE6() {
        return latitudeE6;
    }

    /**
     * Get longitude in degree.
     *
     * @return longitude
     */
    public double getLongitude() {
        return longitudeE6 / 1e6;
    }

    /*
     * Return a waypoint which is the copy of this one rounded to the given limit.
     * For example, to get a waypoint adapter to a display with 3 digits after the
     * seconds decimal point, a rounding factor of 3600*1000 would be appropriate.
     */
    Geopoint roundedAt(final long factor) {
        final double df = 1e6d / factor;
        return new Geopoint((int) Math.round(Math.round(getLatitudeE6() / df) * df),
                (int) Math.round(Math.round(getLongitudeE6() / df) * df),
                this);
    }

    /**
     * Get longitude in microdegree.
     *
     * @return longitude
     */
    public int getLongitudeE6() {
        return longitudeE6;
    }

    /**
     * Get distance and bearing from the current point to a target.
     *
     * @param target
     *            The target
     * @return An array of floats: the distance in meters, then the bearing in degrees
     */
    private float[] pathTo(final Geopoint target) {
        final float[] results = new float[2];
        Location.distanceBetween(getLatitude(), getLongitude(), target.getLatitude(), target.getLongitude(), results);
        return results;
    }

    /**
     * Calculates distance to given Geopoint in km.
     *
     * @param point
     *            target
     * @return distance in km
     * @throws GeopointException
     *             if there is an error in distance calculation
     */
    public float distanceTo(@NonNull final ICoordinates point) {
        final Geopoint otherCoords = point.getCoords();
        if (DISTANCE_BROKEN) {
            return (float) (getDistance(getLatitude(), getLongitude(), otherCoords.getLatitude(), otherCoords.getLongitude()) / 1000);
        }
        return pathTo(otherCoords)[0] / 1000;
    }

    /**
     * Calculates bearing to given Geopoint in degree.
     *
     * @param point
     *            target
     * @return bearing in degree, in the [0,360[ range
     */
    public float bearingTo(final ICoordinates point) {
        // Android library returns a bearing in the [-180;180] range
        final float bearing = pathTo(point.getCoords())[1];
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
    public Geopoint project(final double bearing, final double distance) {
        final double rlat1 = getLatitude() * DEG_TO_RAD;
        final double rlon1 = getLongitude() * DEG_TO_RAD;
        final double rbearing = bearing * DEG_TO_RAD;
        final double rdistance = distance / EARTH_RADIUS;

        final double rlat = Math.asin(Math.sin(rlat1) * Math.cos(rdistance) + Math.cos(rlat1) * Math.sin(rdistance) * Math.cos(rbearing));
        final double rlon = rlon1 + Math.atan2(Math.sin(rbearing) * Math.sin(rdistance) * Math.cos(rlat1), Math.cos(rdistance) - Math.sin(rlat1) * Math.sin(rlat));

        return new Geopoint(rlat * RAD_TO_DEG, rlon * RAD_TO_DEG);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Geopoint)) {
            return false;
        }
        final Geopoint gp = (Geopoint) obj;
        return getLatitudeE6() == gp.getLatitudeE6() && getLongitudeE6() == gp.getLongitudeE6();
    }

    @Override
    public int hashCode() {
        return getLatitudeE6() ^ getLongitudeE6();
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format
     *            the desired format
     * @see GeopointFormatter
     * @return formatted coordinates
     */
    public String format(final GeopointFormatter.Format format) {
        return GeopointFormatter.format(format, this);
    }

    /**
     * Returns formatted coordinates with default format.
     * Default format is decimalminutes, e.g. N 52° 36.123 E 010° 03.456
     *
     * @return formatted coordinates
     */
    @Override
    public String toString() {
        return format(GeopointFormatter.Format.LAT_LON_DECMINUTE);
    }

    public abstract static class GeopointException extends NumberFormatException {
        private static final long serialVersionUID = 1L;

        protected GeopointException(final String msg) {
            super(msg);
        }
    }

    public static class ParseException extends GeopointException {
        private static final long serialVersionUID = 1L;
        public final int resource;

        public ParseException(final String msg, final GeopointParser.LatLon faulty) {
            super(msg);
            resource = faulty == GeopointParser.LatLon.LAT ? R.string.err_parse_lat : R.string.err_parse_lon;
        }
    }

    @Override
    public Geopoint getCoords() {
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(latitudeE6);
        dest.writeInt(longitudeE6);
    }

    public static final Parcelable.Creator<Geopoint> CREATOR = new Parcelable.Creator<Geopoint>() {
        @Override
        public Geopoint createFromParcel(final Parcel in) {
            return new Geopoint(in);
        }

        @Override
        public Geopoint[] newArray(final int size) {
            return new Geopoint[size];
        }
    };

    /**
     * Get latitude character (N or S).
     *
     */
    public char getLatDir() {
        return latitudeE6 >= 0 ? 'N' : 'S';
    }

    /**
     * Get longitude character (E or W).
     *
     */
    public char getLonDir() {
        return longitudeE6 >= 0 ? 'E' : 'W';
    }

    /**
     * Get the integral non-negative latitude degrees.
     *
     */
    public int getLatDeg() {
        return getDeg(getLatitudeE6());
    }

    /**
     * Get the integral non-negative longitude degrees.
     *
     */
    public int getLonDeg() {
        return getDeg(getLongitudeE6());
    }

    private static int getDeg(final int degE6) {
        return Math.abs(degE6 / 1000000);
    }

    /**
     * Get the fractional part of the latitude degrees scaled up by 10^5.
     *
     */
    public int getLatDegFrac() {
        return getDegFrac(getLatitudeE6());
    }

    /**
     * Get the fractional part of the longitude degrees scaled up by 10^5.
     *
     */
    public int getLonDegFrac() {
        return getDegFrac(getLongitudeE6());
    }

    private static int getDegFrac(final int degE6) {
        return Math.min(99999, (int) Math.round((Math.abs(degE6) % 1000000) / 10.0d));
    }

    /**
     * Get the integral latitude minutes.
     *
     */
    public int getLatMin() {
        return getMin(getLatitudeE6());
    }

    /**
     * Get the integral longitude minutes.
     *
     */
    public int getLonMin() {
        return getMin(getLongitudeE6());
    }

    private static int getMin(final int degE6) {
        return (int) ((Math.abs(degE6) * 60L / 1000000L) % 60L);
    }

    /**
     * Get the fractional part of the latitude minutes scaled up by 1000.
     *
     */
    public int getLatMinFrac() {
        return getMinFrac(getLatitudeE6());
    }

    /**
     * Get the fractional part of the longitude minutes scaled up by 1000.
     *
     */
    public int getLonMinFrac() {
        return getMinFrac(getLongitudeE6());
    }

    private static int getMinFrac(final int degE6) {
        return (int) Math.min(999L, Math.round((Math.abs(degE6) * 60L) % 1000000L / 1000d));
    }

    /**
     * Get the latitude minutes.
     *
     */
    public double getLatMinRaw() {
        return getMinRaw(getLatitudeE6());
    }

    /**
     * Get the longitude minutes.
     *
     */
    public double getLonMinRaw() {
        return getMinRaw(getLongitudeE6());
    }

    private static double getMinRaw(final int degE6) {
        return (Math.abs(degE6) * 60L % 60000000L) / 1000000d;
    }

    /**
     * Get the integral part of the latitude seconds.
     *
     */
    public int getLatSec() {
        return getSec(getLatitudeE6());
    }

    /**
     * Get the integral part of the longitude seconds.
     *
     */
    public int getLonSec() {
        return getSec(getLongitudeE6());
    }

    private static int getSec(final int degE6) {
        return (int) ((Math.abs(degE6) * 3600L / 1000000L) % 60L);
    }

    /**
     * Get the fractional part of the latitude seconds scaled up by 1000.
     *
     */

    public int getLatSecFrac() {
        return getSecFrac(getLatitudeE6());
    }

    /**
     * Get the fractional part of the longitude seconds scaled up by 1000.
     *
     */

    public int getLonSecFrac() {
        return getSecFrac(getLongitudeE6());
    }

    private static int getSecFrac(final int degE6) {
        return (int) (Math.min(999L, Math.round(Math.abs(degE6) * 3600L % 1000000L) / 1000d));
    }

    /**
     * Get the latitude seconds.
     *
     */
    public double getLatSecRaw() {
        return getSecRaw(getLatitudeE6());
    }

    /**
     * Get the longitude seconds.
     *
     */
    public double getLonSecRaw() {
        return getSecRaw(getLongitudeE6());
    }

    private static double getSecRaw(final int degE6) {
        return ((Math.abs(degE6) * 3600L) % 60000000L) / 1000000d;
    }

    private static String addZeros(final String value, final int len) {
        return StringUtils.leftPad(value.trim(), len, '0');
    }

    private static String getLonSign(final String lonDir) {
        return "W".equalsIgnoreCase(lonDir) ? "-" : "";
    }

    private static String getLatSign(final String latDir) {
        return "S".equalsIgnoreCase(latDir) ? "-" : "";
    }

    /**
     * Gets distance in meters (workaround for 4.2.1 JIT bug).
     */
    private static double getDistance(final double lat1, final double lon1, final double lat2, final double lon2) {
        // for haversine use R = 6372.8 km instead of 6371 km
        final double earthRadius = 6372.8;
        final double dLat = toRadians(lat2 - lat1);
        final double dLon = toRadians(lon2 - lon1);
        final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        // simplify haversine
        return 2 * earthRadius * 1000 * Math.asin(Math.sqrt(a));
    }

    private static double toRadians(final double angdeg) {
        return angdeg * DEG_TO_RAD;
    }

    /**
     * Check whether a latitude built from user supplied data is valid. We accept both N90/S90.
     *
     * @return <tt>true</tt> if the latitude looks valid, <tt>false</tt> otherwise
     */
    static boolean isValidLatitude(final double latitude) {
        return latitude >= -90 && latitude <= 90;
    }

    /**
     * Check whether a longitude from user supplied data is valid. We accept both E180/W180.
     *
     * @return <tt>true</tt> if the longitude looks valid, <tt>false</tt> otherwise
     */
    static boolean isValidLongitude(final double longitude) {
        return longitude >= -180 && longitude <= 180;
    }

    /**
     * Check whether a geopoint built from user supplied data is valid. We accept both N90/S90 and E180/W180.
     *
     * @return <tt>true</tt> if the geopoint looks valid, <tt>false</tt> otherwise
     */
    public boolean isValid() {
        return isValidLatitude(getLatitude()) && isValidLongitude(getLongitude());
    }

    /**
     * Check whether two geopoints represent the same latitude and longitude or are both <tt>null</tt>.
     *
     * @param p1 the first Geopoint, or <tt>null</tt>
     * @param p2 the second Geopoint, or <tt>null</tt>
     * @return <tt>true</tt> if both geopoints represent the same latitude and longitude or are both <tt>null</tt>,
     *         <tt>false</tt> otherwise
     */
    public static boolean equals(@Nullable final Geopoint p1, @Nullable final Geopoint p2) {
        return p1 == null ? p2 == null : p2 != null && p1.equals(p2);
    }

}
