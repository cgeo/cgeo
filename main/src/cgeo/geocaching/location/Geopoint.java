package cgeo.geocaching.location;

import cgeo.geocaching.ICoordinates;
import cgeo.geocaching.R;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.location.Location;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Abstraction of geographic point. This class is immutable.
 */
public final class Geopoint implements ICoordinates, Parcelable {
    /**
     * Reusable default object
     */
    public static final @NonNull Geopoint ZERO = new Geopoint(0.0, 0.0);

    private static final double DEG_TO_RAD = Math.PI / 180;
    private static final double RAD_TO_DEG = 180 / Math.PI;
    private static final float EARTH_RADIUS = 6371.0f;
    /**
     * JIT bug in Android 4.2.1
     */
    private static final boolean DISTANCE_BROKEN = Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1;

    private final double latitude;
    private final double longitude;

    /**
     * Creates new Geopoint with given latitude and longitude (both degree).
     *
     * @param lat
     *            latitude
     * @param lon
     *            longitude
     */
    public Geopoint(final double lat, final double lon) {
        latitude = lat;
        longitude = lon;
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
        latitude = parsed.latitude;
        longitude = parsed.longitude;
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
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    /**
     * Create new Geopoint from individual textual components.
     *
     * @param latDir
     * @param latDeg
     * @param latDegFrac
     * @param lonDir
     * @param lonDeg
     * @param lonDegFrac
     */
    public Geopoint(final String latDir, final String latDeg, final String latDegFrac,
                    final String lonDir, final String lonDeg, final String lonDegFrac) {
        this(getLatSign(latDir) + latDeg + "." + addZeros(latDegFrac, 5),
                getLonSign(lonDir) + lonDeg + "." + addZeros(lonDegFrac, 5));
    }

    /**
     * Create new Geopoint from individual textual components.
     *
     * @param latDir
     * @param latDeg
     * @param latMin
     * @param latMinFrac
     * @param lonDir
     * @param lonDeg
     * @param lonMin
     * @param lonMinFrac
     */
    public Geopoint(final String latDir, final String latDeg, final String latMin, final String latMinFrac,
                    final String lonDir, final String lonDeg, final String lonMin, final String lonMinFrac) {
        this(latDir + " " + latDeg + " " + latMin + "." + addZeros(latMinFrac, 3),
                lonDir + " " + lonDeg + " " + lonMin + "." + addZeros(lonMinFrac, 3));
    }

    /**
     * Create new Geopoint from individual textual components.
     *
     * @param latDir
     * @param latDeg
     * @param latMin
     * @param latSec
     * @param latSecFrac
     * @param lonDir
     * @param lonDeg
     * @param lonMin
     * @param lonSec
     * @param lonSecFrac
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
        return latitude;
    }

    /**
     * Get latitude in microdegree.
     *
     * @return latitude
     */
    public int getLatitudeE6() {
        return (int) Math.round(latitude * 1E6);
    }

    /**
     * Get longitude in degree.
     *
     * @return longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /*
     * Return a waypoint which is the copy of this one rounded to the given limit.
     * For example, to get a waypoint adapter to a display with 3 digits after the
     * seconds decimal point, a rounding factor of 3600*1000 would be appropriate.
     */
    Geopoint roundedAt(final long factor) {
        return new Geopoint(((double) Math.round(latitude * factor)) / factor,
                            ((double) Math.round(longitude * factor)) / factor);
    }

    /**
     * Get longitude in microdegree.
     *
     * @return longitude
     */
    public int getLongitudeE6() {
        return (int) Math.round(longitude * 1E6);
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
        android.location.Location.distanceBetween(latitude, longitude, target.latitude, target.longitude, results);
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
            return (float) (getDistance(latitude, longitude, otherCoords.latitude, otherCoords.longitude) / 1000);
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
        final double rlat1 = latitude * DEG_TO_RAD;
        final double rlon1 = longitude * DEG_TO_RAD;
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
    public String format(GeopointFormatter.Format format) {
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

    abstract public static class GeopointException extends NumberFormatException {
        private static final long serialVersionUID = 1L;

        protected GeopointException(String msg) {
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
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
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
     * @return
     */
    public char getLatDir() {
        return latitude >= 0 ? 'N' : 'S';
    }

    /**
     * Get longitude character (E or W).
     *
     * @return
     */
    public char getLonDir() {
        return longitude >= 0 ? 'E' : 'W';
    }

    /**
     * Get the integral non-negative latitude degrees.
     *
     * @return
     */
    public int getLatDeg() {
        return getDeg(latitude);
    }

    /**
     * Get the integral non-negative longitude degrees.
     *
     * @return
     */
    public int getLonDeg() {
        return getDeg(longitude);
    }

    private static int getDeg(final double deg) {
        return (int) Math.abs(deg);
    }

    /**
     * Get the fractional part of the latitude degrees scaled up by 10^5.
     *
     * @return
     */
    public int getLatDegFrac() {
        return getDegFrac(latitude);
    }

    /**
     * Get the fractional part of the longitude degrees scaled up by 10^5.
     *
     * @return
     */
    public int getLonDegFrac() {
        return getDegFrac(longitude);
    }

    private static int getDegFrac(final double deg) {
        return (int) (Math.round(Math.abs(deg) * 100000) % 100000);
    }

    /**
     * Get the integral latitude minutes.
     *
     * @return
     */
    public int getLatMin() {
        return getMin(latitude);
    }

    /**
     * Get the integral longitude minutes.
     *
     * @return
     */
    public int getLonMin() {
        return getMin(longitude);
    }

    private static int getMin(final double deg) {
        return ((int) Math.abs(deg * 60)) % 60;
    }

    /**
     * Get the fractional part of the latitude minutes scaled up by 1000.
     *
     * @return
     */
    public int getLatMinFrac() {
        return getMinFrac(latitude);
    }

    /**
     * Get the fractional part of the longitude minutes scaled up by 1000.
     *
     * @return
     */
    public int getLonMinFrac() {
        return getMinFrac(longitude);
    }

    private static int getMinFrac(final double deg) {
        return (int) (Math.round(Math.abs(deg) * 60000) % 1000);
    }

    /**
     * Get the latitude minutes.
     *
     * @return
     */
    public double getLatMinRaw() {
        return getMinRaw(latitude);
    }

    /**
     * Get the longitude minutes.
     *
     * @return
     */
    public double getLonMinRaw() {
        return getMinRaw(longitude);
    }

    private static double getMinRaw(final double deg) {
        return (Math.abs(deg) * 60) % 60;
    }

    /**
     * Get the integral part of the latitude seconds.
     *
     * @return
     */
    public int getLatSec() {
        return getSec(latitude);
    }

    /**
     * Get the integral part of the longitude seconds.
     *
     * @return
     */
    public int getLonSec() {
        return getSec(longitude);
    }

    private static int getSec(final double deg) {
        return ((int) Math.abs(deg * 3600)) % 60;
    }

    /**
     * Get the fractional part of the latitude seconds scaled up by 1000.
     *
     * @return
     */

    public int getLatSecFrac() {
        return getSecFrac(latitude);
    }

    /**
     * Get the fractional part of the longitude seconds scaled up by 1000.
     *
     * @return
     */

    public int getLonSecFrac() {
        return getSecFrac(longitude);
    }

    private static int getSecFrac(final double deg) {
        return (int) (Math.round(Math.abs(deg) * 3600000) % 1000);
    }

    /**
     * Get the latitude seconds.
     *
     * @return
     */
    public double getLatSecRaw() {
        return getSecRaw(latitude);
    }

    /**
     * Get the longitude seconds.
     *
     * @return
     */
    public double getLonSecRaw() {
        return getSecRaw(longitude);
    }

    private static double getSecRaw(final double deg) {
        return (Math.abs(deg) * 3600) % 60;
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
    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        // for haversine use R = 6372.8 km instead of 6371 km
        double earthRadius = 6372.8;
        double dLat = toRadians(lat2 - lat1);
        double dLon = toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        // simplify haversine
        return (2 * earthRadius * 1000 * Math.asin(Math.sqrt(a)));
    }

    private static double toRadians(double angdeg) {
        return angdeg * DEG_TO_RAD;
    }

    /**
     * Check whether a latitude built from user supplied data is valid. We accept both N90/S90.
     *
     * @return <tt>true</tt> if the latitude looks valid, <tt>false</tt> otherwise
     */
    public static boolean isValidLatitude(final double latitude) {
        return latitude >= -90 && latitude <= 90;
    }

    /**
     * Check whether a longitude from user supplied data is valid. We accept both E180/W180.
     * 
     * @return <tt>true</tt> if the longitude looks valid, <tt>false</tt> otherwise
     */
    public static boolean isValidLongitude(final double longitude) {
        return longitude >= -180 && longitude <= 180;
    }

    /**
     * Check whether a geopoint built from user supplied data is valid. We accept both N90/S90 and E180/W180.
     *
     * @return <tt>true</tt> if the geopoint looks valid, <tt>false</tt> otherwise
     */
    public boolean isValid() {
        return isValidLatitude(latitude) && isValidLongitude(longitude);
    }

}
