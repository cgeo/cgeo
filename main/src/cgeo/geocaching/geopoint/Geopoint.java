package cgeo.geocaching.geopoint;

import cgeo.geocaching.ICoordinates;
import cgeo.geocaching.R;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Abstraction of geographic point.
 */
public final class Geopoint implements ICoordinates, Parcelable {
    public static final double deg2rad = Math.PI / 180;
    public static final double rad2deg = 180 / Math.PI;
    public static final float erad = 6371.0f;

    public static final Geopoint ZERO = new Geopoint(0.0, 0.0);

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
    public Geopoint(final double lat, final double lon)
    {
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
    public Geopoint(final String text) {
        final Geopoint parsed = GeopointParser.parse(text);
        this.latitude = parsed.latitude;
        this.longitude = parsed.longitude;
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
     * @see GeopointParser#parse(String, String)
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
        latitude = Double.parseDouble(latDeg + "." + addZeros(Integer.parseInt(latDegFrac), 5)) *
                getLatSign(latDir);
        longitude = Double.parseDouble(lonDeg + "." + addZeros(Integer.parseInt(lonDegFrac), 5)) *
                getLonSign(lonDir);
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
        latitude = (Double.parseDouble(latDeg) + Double.parseDouble(latMin + "." + addZeros(Integer.parseInt(latMinFrac), 3)) / 60) *
                (getLatSign(latDir));
        longitude = (Double.parseDouble(lonDeg) + Double.parseDouble(lonMin + "." + addZeros(Integer.parseInt(lonMinFrac), 3)) / 60) *
                (getLonSign(lonDir));
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
        latitude = (Double.parseDouble(latDeg) + Double.parseDouble(latMin) / 60 + Double.parseDouble(latSec + "." + addZeros(Integer.parseInt(latSecFrac), 3)) / 3600) *
                (getLatSign(latDir));
        longitude = (Double.parseDouble(lonDeg) + Double.parseDouble(lonMin) / 60 + Double.parseDouble(lonSec + "." + addZeros(Integer.parseInt(lonSecFrac), 3)) / 3600) *
                (getLonSign(lonDir));
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
        return (int) Math.round(latitude * 1E6);
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
    public float distanceTo(final ICoordinates point)
    {
        return pathTo(point.getCoords())[0] / 1000;
    }

    /**
     * Calculates bearing to given Geopoint in degree.
     *
     * @param point
     *            target
     * @return bearing in degree, in the [0,360[ range
     */
    public float bearingTo(final ICoordinates point)
    {
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
     * Checks if given Geopoint is similar to this Geopoint with tolerance.
     *
     * @param gp
     *            Geopoint to check
     * @param tolerance
     *            tolerance in km
     * @return true if similar, false otherwise
     */
    public boolean isEqualTo(Geopoint gp, double tolerance) {
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

        protected GeopointException(String msg)
        {
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

    public Double getElevation() {
        try {
            final String uri = "http://maps.googleapis.com/maps/api/elevation/json";
            final Parameters params = new Parameters(
                    "sensor", "false",
                    "locations", format(Format.LAT_LON_DECDEGREE_COMMA));
            final JSONObject response = Network.requestJSON(uri, params);

            if (response == null) {
                return null;
            }

            if (!StringUtils.equalsIgnoreCase(response.getString("status"), "OK")) {
                return null;
            }

            if (response.has("results")) {
                JSONArray results = response.getJSONArray("results");
                JSONObject result = results.getJSONObject(0);
                return result.getDouble("elevation");
            }
        } catch (Exception e) {
            Log.w("cgBase.getElevation: " + e.toString());
        }

        return null;
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
     * Get longitude chararcter (E or W).
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

    private static String addZeros(final int value, final int len) {
        return StringUtils.leftPad(Integer.toString(value), len, '0');
    }

    private static int getLonSign(final String lonDir) {
        return "W".equalsIgnoreCase(lonDir) ? -1 : 1;
    }

    private static int getLatSign(final String latDir) {
        return "S".equalsIgnoreCase(latDir) ? -1 : 1;
    }

}
