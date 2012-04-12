package cgeo.geocaching.geopoint;

import cgeo.geocaching.ICoordinates;
import cgeo.geocaching.Settings;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Abstraction of geographic point.
 */
public final class Geopoint implements ICoordinates {
    public static final double deg2rad = Math.PI / 180;
    public static final double rad2deg = 180 / Math.PI;
    public static final float erad = 6371.0f;

    private final double latitude;
    private final double longitude;

    private Direction direction;
    private DDD ddd;
    private DMM dmm;
    private DMS dms;

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
     * @throws GeopointParser.ParseException
     *             if the string cannot be parsed
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

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Geopoint)) {
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
    @Override
    public String toString()
    {
        return format(GeopointFormatter.Format.LAT_LON_DECMINUTE);
    }

    /**
     * Converts this geopoint to value type Direction.
     *
     * @return Direction
     */
    public Direction asDirection() {
        if (direction == null) { // because geopoint is immutable we can "cache" the result
            direction = new Direction(latitude, longitude);
        }
        return direction;
    }

    /**
     * Converts this geopoint to value type DDD.
     *
     * @return
     */
    public DDD asDDD() {
        if (ddd == null) {
            ddd = new DDD(latitude, longitude);
        }
        return ddd;
    }

    /**
     * Converts this geopoint to value type DMM.
     *
     * @return
     */
    public DMM asDMM() {
        if (dmm == null) {
            dmm = new DMM(latitude, longitude);
        }
        return dmm;
    }

    /**
     * Converts this geopoint to value type DMS.
     *
     * @return
     */
    public DMS asDMS() {
        if (dms == null) {
            dms = new DMS(latitude, longitude);
        }
        return dms;
    }

    /* Constant values needed for calculation */
    private static final double D60 = 60.0d;
    private static final double D1000 = 1000.0d;
    private static final double D3600 = 3600.0d;
    private static final BigDecimal BD_SIXTY = BigDecimal.valueOf(D60);
    private static final BigDecimal BD_THOUSAND = BigDecimal.valueOf(D1000);
    private static final BigDecimal BD_ONEHOUNDREDTHOUSAND = BigDecimal.valueOf(100000.0d);

    /**
     * Value type for the direction.
     */
    public static class Direction {
        /** latitude direction, 'N' or 'S' */
        public final char latDir;
        /** longitude direction, 'E' or 'W' */
        public final char lonDir;

        private Direction(final double latSigned, final double lonSigned) {
            latDir = latSigned < 0 ? 'S' : 'N';
            lonDir = lonSigned < 0 ? 'W' : 'E';
        }

        protected static String addZeros(final int value, final int len) {
            return StringUtils.leftPad(Integer.toString(value), len, '0');
        }
    }

    /**
     * Value type for the DDD.DDDDD format.
     */
    public static final class DDD extends Direction {

        /** latitude degree value */
        public final int latDeg;
        /** fractional part of the latitude degree value */
        public final int latDegFrac;

        public final int lonDeg;
        public final int lonDegFrac;

        private DDD(final double latSigned, final double lonSigned) {
            super(latSigned, lonSigned);
            BigDecimal bdLat = BigDecimal.valueOf(latSigned).abs();
            latDeg = bdLat.intValue();
            BigDecimal bdLatFrac = bdLat.subtract(BigDecimal.valueOf(latDeg)).multiply(BD_ONEHOUNDREDTHOUSAND);
            latDegFrac = bdLatFrac.setScale(0, RoundingMode.HALF_UP).intValue();

            BigDecimal bdlon = BigDecimal.valueOf(lonSigned).abs();
            lonDeg = bdlon.intValue();
            BigDecimal bdLonFrac = bdlon.subtract(BigDecimal.valueOf(lonDeg)).multiply(BD_ONEHOUNDREDTHOUSAND);
            lonDegFrac = bdLonFrac.setScale(0, RoundingMode.HALF_UP).intValue();
        }

        public static Geopoint createGeopoint(final String latDir, final String latDeg, final String latDegFrac,
                final String lonDir, final String lonDeg, final String lonDegFrac) {
            double lat = 0.0d;
            double lon = 0.0d;
            try {
                lat = Double.parseDouble(latDeg + "." + addZeros(Integer.parseInt(latDegFrac), 5));
                lon = Double.parseDouble(lonDeg + "." + addZeros(Integer.parseInt(lonDegFrac), 5));
            } catch (NumberFormatException e) {
            }
            lat *= "S".equalsIgnoreCase(latDir) ? -1 : 1;
            lon *= "W".equalsIgnoreCase(lonDir) ? -1 : 1;
            return new Geopoint(lat, lon);
        }
    }

    public static final class DMM extends Direction {

        public final int latDeg;
        public final double latMinRaw;
        public final int latMin;
        public final int latMinFrac;

        public final int lonDeg;
        public final double lonMinRaw;
        public final int lonMin;
        public final int lonMinFrac;

        private DMM(final double latSigned, final double lonSigned) {
            super(latSigned, lonSigned);
            BigDecimal bdLat = BigDecimal.valueOf(latSigned).abs();
            latDeg = bdLat.intValue();
            BigDecimal bdLatMin = bdLat.subtract(BigDecimal.valueOf(latDeg)).multiply(BD_SIXTY);
            // Rounding here ...
            bdLatMin = bdLatMin.setScale(3, RoundingMode.HALF_UP);
            latMinRaw = bdLatMin.doubleValue();
            latMin = bdLatMin.intValue();
            BigDecimal bdLatMinFrac = bdLatMin.subtract(BigDecimal.valueOf(latMin)).multiply(BD_THOUSAND);
            latMinFrac = bdLatMinFrac.setScale(0, RoundingMode.HALF_UP).intValue();

            BigDecimal bdlon = BigDecimal.valueOf(lonSigned).abs();
            lonDeg = bdlon.intValue();
            BigDecimal bdLonMin = bdlon.subtract(BigDecimal.valueOf(lonDeg)).multiply(BD_SIXTY);
            // Rounding here ...
            bdLonMin = bdLonMin.setScale(3, RoundingMode.HALF_UP);
            lonMinRaw = bdLonMin.doubleValue();
            lonMin = bdLonMin.intValue();
            BigDecimal bdLonMinFrac = bdLonMin.subtract(BigDecimal.valueOf(lonMin)).multiply(BD_THOUSAND);
            lonMinFrac = bdLonMinFrac.setScale(0, RoundingMode.HALF_UP).intValue();
        }

        public static Geopoint createGeopoint(final String latDir, final String latDeg, final String latMin, final String latMinFrac,
                final String lonDir, final String lonDeg, final String lonMin, final String lonMinFrac) {
            double lat = 0.0d;
            double lon = 0.0d;
            try {
                lat = Double.parseDouble(latDeg) + Double.parseDouble(latMin + "." + addZeros(Integer.parseInt(latMinFrac), 3)) / D60;
                lon = Double.parseDouble(lonDeg) + Double.parseDouble(lonMin + "." + addZeros(Integer.parseInt(lonMinFrac), 3)) / D60;
            } catch (NumberFormatException e) {
            }
            lat *= "S".equalsIgnoreCase(latDir) ? -1 : 1;
            lon *= "W".equalsIgnoreCase(lonDir) ? -1 : 1;
            return new Geopoint(lat, lon);
        }
    }

    public static final class DMS extends Direction {

        public final int latDeg;
        public final int latMin;
        public final double latSecRaw;
        public final int latSec;
        public final int latSecFrac;

        public final int lonDeg;
        public final int lonMin;
        public final double lonSecRaw;
        public final int lonSec;
        public final int lonSecFrac;

        private DMS(final double latSigned, final double lonSigned) {
            super(latSigned, lonSigned);
            BigDecimal bdLat = BigDecimal.valueOf(latSigned).abs();
            latDeg = bdLat.intValue();
            BigDecimal bdLatMin = bdLat.subtract(BigDecimal.valueOf(latDeg)).multiply(BD_SIXTY);
            latMin = bdLatMin.intValue();
            BigDecimal bdLatSec = bdLatMin.subtract(BigDecimal.valueOf(latMin)).multiply(BD_SIXTY);
            // Rounding here ...
            bdLatSec = bdLatSec.setScale(3, RoundingMode.HALF_UP);
            latSecRaw = bdLatSec.doubleValue();
            latSec = bdLatSec.intValue();
            BigDecimal bdLatSecFrac = bdLatSec.subtract(BigDecimal.valueOf(latSec)).multiply(BD_THOUSAND);
            latSecFrac = bdLatSecFrac.setScale(0, RoundingMode.HALF_UP).intValue();

            BigDecimal bdlon = BigDecimal.valueOf(lonSigned).abs();
            lonDeg = bdlon.intValue();
            BigDecimal bdLonMin = bdlon.subtract(BigDecimal.valueOf(lonDeg)).multiply(BD_SIXTY);
            lonMin = bdLonMin.intValue();
            BigDecimal bdLonSec = bdLonMin.subtract(BigDecimal.valueOf(lonMin)).multiply(BD_SIXTY);
            // Rounding here ...
            bdLonSec = bdLonSec.setScale(3, RoundingMode.HALF_UP);
            lonSecRaw = bdLonSec.doubleValue();
            lonSec = bdLonSec.intValue();
            BigDecimal bdLonSecFrac = bdLonSec.subtract(BigDecimal.valueOf(lonSec)).multiply(BD_THOUSAND);
            lonSecFrac = bdLonSecFrac.setScale(0, RoundingMode.HALF_UP).intValue();
        }

        public static Geopoint createGeopoint(final String latDir, final String latDeg, final String latMin, final String latSec, final String latSecFrac,
                final String lonDir, final String lonDeg, final String lonMin, final String lonSec, final String lonSecFrac) {
            double lat = 0.0d;
            double lon = 0.0d;
            try {
                lat = Double.parseDouble(latDeg) + Double.parseDouble(latMin) / D60 + Double.parseDouble(latSec + "." + addZeros(Integer.parseInt(latSecFrac), 3)) / D3600;
                lon = Double.parseDouble(lonDeg) + Double.parseDouble(lonMin) / D60 + Double.parseDouble(lonSec + "." + addZeros(Integer.parseInt(lonSecFrac), 3)) / D3600;
            } catch (NumberFormatException e) {
            }
            lat *= "S".equalsIgnoreCase(latDir) ? -1 : 1;
            lon *= "W".equalsIgnoreCase(lonDir) ? -1 : 1;
            return new Geopoint(lat, lon);
        }
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
            Log.w(Settings.tag, "cgBase.getElevation: " + e.toString());
        }

        return null;
    }

    //FIXME: this interface implementation is totally confusing as it returns the class itself.
    // it can therefore be removed completely (and any invocation of it) without any disadvantages
    @Override
    public Geopoint getCoords() {
        return this;
    }

}
