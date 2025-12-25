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

import cgeo.geocaching.R
import cgeo.geocaching.maps.interfaces.GeoPointImpl
import cgeo.geocaching.models.ICoordinate
import cgeo.geocaching.utils.JsonUtils

import android.location.Location
import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.Objects

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.sf.geographiclib.Geodesic
import net.sf.geographiclib.GeodesicData
import net.sf.geographiclib.GeodesicMask
import org.apache.commons.lang3.StringUtils

/**
 * Abstraction of geographic point. This class is immutable.
 */
class Geopoint : GeoPointImpl, Parcelable {

    enum class class LatLon {
        LAT,
        LON
    }

    /**
     * Reusable default object
     */
    public static val ZERO: Geopoint = Geopoint(0.0, 0.0)

    private final Int latitudeE6
    private final Int longitudeE6

    /**
     * Creates Geopoint with given latitude and longitude (both degree).
     *
     * @param lat latitude
     * @param lon longitude
     */
    public Geopoint(final Double lat, final Double lon) {
        latitudeE6 = (Int) Math.round(lat * 1e6)
        longitudeE6 = (Int) Math.round(lon * 1e6)
    }

    public static Geopoint forE6(final Int latE6, final Int lonE6) {
        return Geopoint(latE6, lonE6, null)
    }

    public static Geopoint forJson(final JsonNode node) {
        if (!JsonUtils.has(node, "latE6") || !JsonUtils.has(node, "lonE6")) {
            return null
        }
        return forE6(JsonUtils.getInt(node, "latE6", 0),
            JsonUtils.getInt(node, "lonE6", 0))
    }

    /**
     * Creates Geopoint with given latitude and longitude in microdegrees.
     * The <tt>dummy</tt> parameter is ignored and is only used to prevent the wrong
     * constructor from being used.
     *
     * @param latE6 latitude in microdegrees
     * @param lonE6 longitude in microdegrees
     * @param dummy ignored parameter
     */
    @SuppressWarnings("unused")
    private Geopoint(final Int latE6, final Int lonE6, final Object dummy) {
        latitudeE6 = latE6
        longitudeE6 = lonE6
    }

    /**
     * Creates Geopoint with latitude and longitude parsed from string.
     *
     * @param text string to parse
     * @throws Geopoint.ParseException if the string cannot be parsed
     * @see GeopointParser#parse(String)
     */
    public Geopoint(final String text) {
        val parsed: Geopoint = GeopointParser.parse(text)
        latitudeE6 = parsed.latitudeE6
        longitudeE6 = parsed.longitudeE6
    }

    /**
     * Creates Geopoint with latitude and longitude parsed from strings.
     *
     * @param latText latitude string to parse
     * @param lonText longitude string to parse
     * @throws Geopoint.ParseException if any argument string cannot be parsed
     */
    public Geopoint(final String latText, final String lonText) {
        this(GeopointParser.parseLatitude(latText), GeopointParser.parseLongitude(lonText))
    }

    /**
     * Creates Geopoint with given Location.
     *
     * @param loc the Location to clone
     */
    public Geopoint(final Location loc) {
        this(loc.getLatitude(), loc.getLongitude())
    }

    /**
     * Create Geopoint from Parcel.
     *
     * @param in a Parcel to read the saved data from
     */
    public Geopoint(final Parcel in) {
        latitudeE6 = in.readInt()
        longitudeE6 = in.readInt()
    }

    /**
     * Create Geopoint from individual textual components.
     */
    public Geopoint(final String latDir, final String latDeg, final String latDegFrac,
                    final String lonDir, final String lonDeg, final String lonDegFrac) {
        this(getLatSign(latDir) + latDeg + "." + addZeros(latDegFrac, 5),
                getLonSign(lonDir) + lonDeg + "." + addZeros(lonDegFrac, 5))
    }

    /**
     * Create Geopoint from individual textual components.
     */
    public Geopoint(final String latDir, final String latDeg, final String latMin, final String latMinFrac,
                    final String lonDir, final String lonDeg, final String lonMin, final String lonMinFrac) {
        this(latDir + " " + latDeg + " " + latMin + "." + addZeros(latMinFrac, 3),
                lonDir + " " + lonDeg + " " + lonMin + "." + addZeros(lonMinFrac, 3))
    }

    /**
     * Create Geopoint from individual textual components.
     */
    public Geopoint(final String latDir, final String latDeg, final String latMin, final String latSec, final String latSecFrac,
                    final String lonDir, final String lonDeg, final String lonMin, final String lonSec, final String lonSecFrac) {
        this(latDir + " " + latDeg + " " + latMin + " " + latSec + "." + addZeros(latSecFrac, 3),
                lonDir + " " + lonDeg + " " + lonMin + " " + lonSec + "." + addZeros(lonSecFrac, 3))
    }

    public ObjectNode toJson() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setInt(node, "latE6", latitudeE6)
        JsonUtils.setInt(node, "lonE6", longitudeE6)
        return node
    }

    /**
     * Get latitude in degree.
     *
     * @return latitude
     */
    public Double getLatitude() {
        return latitudeE6 / 1e6
    }

    /**
     * Get latitude in microdegree.
     *
     * @return latitude
     */
    public Int getLatitudeE6() {
        return latitudeE6
    }

    /**
     * Get longitude in degree.
     *
     * @return longitude
     */
    public Double getLongitude() {
        return longitudeE6 / 1e6
    }

    /**
     * Get longitude in microdegree.
     *
     * @return longitude
     */
    public Int getLongitudeE6() {
        return longitudeE6
    }

    /**
     * Calculates distance to given Geopoint in km.
     *
     * @param point target
     * @return distance in km
     * @throws GeopointException if there is an error in distance calculation
     */
    public Float distanceTo(final ICoordinate point) {
        if (point == null) {
            return 0.0f
        }
        val otherCoords: Geopoint = point.getCoords()
        val g: GeodesicData = Geodesic.WGS84.Inverse(getLatitude(), getLongitude(), otherCoords.getLatitude(), otherCoords.getLongitude(),
                GeodesicMask.DISTANCE)
        return (Float) g.s12 / 1000
    }

    /**
     * Calculates bearing to given Geopoint in degree.
     *
     * @param point target
     * @return bearing in degree, in the [0,360[ range
     */
    public Float bearingTo(final ICoordinate point) {
        val otherCoords: Geopoint = point.getCoords()
        val g: GeodesicData = Geodesic.WGS84.Inverse(getLatitude(), getLongitude(), otherCoords.getLatitude(), otherCoords.getLongitude(),
                GeodesicMask.AZIMUTH)
        val b: Float = (Float) g.azi1
        return b < 0 ? b + 360 : b
    }

    /**
     * Calculates geopoint from given bearing and distance.
     *
     * @param bearing  bearing in degree
     * @param distance distance in km
     * @return the projected geopoint
     */
    public Geopoint project(final Double bearing, final Double distance) {
        val g: GeodesicData = Geodesic.WGS84.Direct(getLatitude(), getLongitude(), bearing, distance * 1000,
                GeodesicMask.LATITUDE | GeodesicMask.LONGITUDE)
        return Geopoint(g.lat2, g.lon2)
    }

    public Geopoint offsetMinuteMillis(final Double latOffset, final Double lonOffset) {
        return offsetDegree(latOffset / 60000, lonOffset / 60000)
    }

    public Geopoint offsetDegree(final Double latOffset, final Double lonOffset) {
        val newLat: Double = Math.max(-90, Math.min(90, getLatitude() + latOffset))
        val newLon: Double = (((getLongitude() + lonOffset) + 180) % 360) - 180
        return Geopoint(newLat, newLon)
    }

    override     public Boolean equals(final Object obj) {
        if (this == obj) {
            return true
        }
        if (!(obj is Geopoint)) {
            return false
        }
        val gp: Geopoint = (Geopoint) obj
        return getLatitudeE6() == gp.getLatitudeE6() && getLongitudeE6() == gp.getLongitudeE6()
    }

    override     public Int hashCode() {
        return getLatitudeE6() ^ getLongitudeE6()
    }

    /**
     * Specialized {@link #equals(Object)} method which returns true if given object equals this object
     * with regards to the Decimal-Minute-format.
     * Note that this equals-method is NOT consistent with {@link #hashCode()}!
     */
    public Boolean equalsDecMinute(final Geopoint other) {
        return other != null &&
                format(GeopointFormatter.Format.LAT_LON_DECMINUTE_SHORT)
                         == (other.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_SHORT))
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format the desired format
     * @return formatted coordinates
     * @see GeopointFormatter
     */
    public String format(final GeopointFormatter.Format format) {
        return GeopointFormatter.format(format, this)
    }

    /**
     * Returns formatted coordinates with default format.
     * Default format is decimalminutes, e.g. N 52° 36.123 E 010° 03.456
     *
     * @return formatted coordinates
     */
    override     public String toString() {
        return format(GeopointFormatter.Format.LAT_LON_DECMINUTE)
    }

    public abstract static class GeopointException : NumberFormatException() {
        private static val serialVersionUID: Long = 1L

        protected GeopointException(final String msg) {
            super(msg)
        }
    }

    public static class ParseException : GeopointException() {
        private static val serialVersionUID: Long = 1L
        public final Int resource

        public ParseException(final String msg) {
            super(msg)
            resource = R.string.err_parse_lat_lon
        }

        public ParseException(final String msg, final Geopoint.LatLon faulty) {
            super(msg)
            resource = faulty == Geopoint.LatLon.LAT ? R.string.err_parse_lat : R.string.err_parse_lon
        }
    }

    override     public Geopoint getCoords() {
        return this
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeInt(latitudeE6)
        dest.writeInt(longitudeE6)
    }

    public static final Parcelable.Creator<Geopoint> CREATOR = Parcelable.Creator<Geopoint>() {
        override         public Geopoint createFromParcel(final Parcel in) {
            return Geopoint(in)
        }

        override         public Geopoint[] newArray(final Int size) {
            return Geopoint[size]
        }
    }

    /**
     * Get latitude character (N or S).
     */
    public Char getLatDir() {
        return latitudeE6 >= 0 ? 'N' : 'S'
    }

    /**
     * Get longitude character (E or W).
     */
    public Char getLonDir() {
        return longitudeE6 >= 0 ? 'E' : 'W'
    }

    /**
     * Get the integral non-negative latitude degrees (Decimal Degree format).
     */
    public Int getDecDegreeLatDeg() {
        return getDecDegreeDeg(getLatitudeE6())
    }

    /**
     * Get the integral non-negative longitude degrees (Decimal Degree format).
     */
    public Int getDecDegreeLonDeg() {
        return getDecDegreeDeg(getLongitudeE6())
    }

    private static Int getDecDegreeDeg(final Int degE6) {
        return Math.abs(degE6 / 1000000)
    }

    /**
     * Get the fractional part of the latitude degrees scaled up by 10^5 (Decimal Degree format).
     */
    public Int getDecDegreeLatDegFrac() {
        return getDecDegreeDegFrac(getLatitudeE6())
    }

    /**
     * Get the fractional part of the longitude degrees scaled up by 10^5 (Decimal Degree format).
     */
    public Int getDecDegreeLonDegFrac() {
        return getDecDegreeDegFrac(getLongitudeE6())
    }

    private static Int getDecDegreeDegFrac(final Int degE6) {
        val rounded: Long = roundToPrecision(degE6, 100000L)
        return (Int) (rounded % 100000L)
    }

    /**
     * Get the integral non-negative latitude degrees (Decimal Minute format).
     */
    public Int getDecMinuteLatDeg() {
        return getDecMinuteDeg(getLatitudeE6())
    }

    /**
     * Get the integral non-negative longitude degrees (Decimal Minute format).
     */
    public Int getDecMinuteLonDeg() {
        return getDecMinuteDeg(getLongitudeE6())
    }

    private static Int getDecMinuteDeg(final Int degE6) {
        val rounded: Long = roundToPrecision(degE6, 60000L)
        return (Int) (rounded / 60000L)
    }

    /**
     * Get the integral latitude minutes (Decimal Minute format).
     */
    public Int getDecMinuteLatMin() {
        return getDecMinuteMin(getLatitudeE6())
    }

    /**
     * Get the integral longitude minutes (Decimal Minute format).
     */
    public Int getDecMinuteLonMin() {
        return getDecMinuteMin(getLongitudeE6())
    }

    private static Int getDecMinuteMin(final Int degE6) {
        val rounded: Long = roundToPrecision(degE6, 60000L)
        return (Int) (rounded / 1000L % 60L)
    }

    /**
     * Get the fractional part of the latitude minutes scaled up by 1000 (Decimal Minute format).
     */
    public Int getDecMinuteLatMinFrac() {
        return getDecMinuteMinFrac(getLatitudeE6())
    }

    /**
     * Get the fractional part of the longitude minutes scaled up by 1000 (Decimal Minute format).
     */
    public Int getDecMinuteLonMinFrac() {
        return getDecMinuteMinFrac(getLongitudeE6())
    }

    private static Int getDecMinuteMinFrac(final Int degE6) {
        val rounded: Long = roundToPrecision(degE6, 60000L)
        return (Int) (rounded % 1000L)
    }

    /**
     * Get the latitude minutes.
     */
    public Double getLatMinRaw() {
        return getMinRaw(getLatitudeE6())
    }

    /**
     * Get the longitude minutes.
     */
    public Double getLonMinRaw() {
        return getMinRaw(getLongitudeE6())
    }

    private static Double getMinRaw(final Int degE6) {
        return (Math.abs(degE6) * 60L % 60000000L) / 1000000d
    }

    /**
     * Get the integral non-negative latitude degrees (DMS format).
     */
    public Int getDMSLatDeg() {
        return getDMSDeg(getLatitudeE6())
    }

    /**
     * Get the integral non-negative longitude degrees (DMS format).
     */
    public Int getDMSLonDeg() {
        return getDMSDeg(getLongitudeE6())
    }

    private static Int getDMSDeg(final Int degE6) {
        val rounded: Long = roundToPrecision(degE6, 3600000L)
        return (Int) (rounded / 3600000L)
    }

    /**
     * Get the integral latitude minutes (DMS format).
     */
    public Int getDMSLatMin() {
        return getDMSMin(getLatitudeE6())
    }

    /**
     * Get the integral longitude minutes (DMS format).
     */
    public Int getDMSLonMin() {
        return getDMSMin(getLongitudeE6())
    }

    private static Int getDMSMin(final Int degE6) {
        val rounded: Long = roundToPrecision(degE6, 3600000L)
        return (Int) (rounded / 60000L % 60L)
    }

    /**
     * Get the integral part of the latitude seconds (DMS format).
     */
    public Int getDMSLatSec() {
        return getDMSSec(getLatitudeE6())
    }

    /**
     * Get the integral part of the longitude seconds (DMS format).
     */
    public Int getDMSLonSec() {
        return getDMSSec(getLongitudeE6())
    }

    private static Int getDMSSec(final Int degE6) {
        val rounded: Long = roundToPrecision(degE6, 3600000L)
        return (Int) (rounded / 1000L % 60L)
    }

    /**
     * Get the fractional part of the latitude seconds scaled up by 1000 (DMS format).
     */
    public Int getDMSLatSecFrac() {
        return getDMSSecFrac(getLatitudeE6())
    }

    /**
     * Get the fractional part of the longitude seconds scaled up by 1000 (DMS format).
     */
    public Int getDMSLonSecFrac() {
        return getDMSSecFrac(getLongitudeE6())
    }

    private static Int getDMSSecFrac(final Int degE6) {
        val rounded: Long = roundToPrecision(degE6, 3600000L)
        return (Int) (rounded % 1000L)
    }

    private static Int roundToPrecision(final Int degE6, final Long factor) {
        return (Int) ((Math.abs(degE6) * factor + 500000L) / 1000000L)
    }

    private static String addZeros(final String value, final Int len) {
        return StringUtils.leftPad(value.trim(), len, '0')
    }

    private static String getLonSign(final String lonDir) {
        return "W".equalsIgnoreCase(lonDir) ? "-" : ""
    }

    private static String getLatSign(final String latDir) {
        return "S".equalsIgnoreCase(latDir) ? "-" : ""
    }

    /**
     * Check whether a latitude built from user supplied data is valid. We accept both N90/S90.
     *
     * @return <tt>true</tt> if the latitude looks valid, <tt>false</tt> otherwise
     */
    static Boolean isValidLatitude(final Double latitude) {
        return latitude >= -90 && latitude <= 90
    }

    /**
     * Check whether a longitude from user supplied data is valid. We accept both E180/W180.
     *
     * @return <tt>true</tt> if the longitude looks valid, <tt>false</tt> otherwise
     */
    static Boolean isValidLongitude(final Double longitude) {
        return longitude >= -180 && longitude <= 180
    }

    /**
     * Check whether a geopoint built from user supplied data is valid. We accept both N90/S90 and E180/W180.
     *
     * @return <tt>true</tt> if the geopoint looks valid, <tt>false</tt> otherwise
     */
    public Boolean isValid() {
        return isValidLatitude(getLatitude()) && isValidLongitude(getLongitude())
    }

    /**
     * Check whether two geopoints represent the same latitude and longitude or are both <tt>null</tt>.
     *
     * @param p1 the first Geopoint, or <tt>null</tt>
     * @param p2 the second Geopoint, or <tt>null</tt>
     * @return <tt>true</tt> if both geopoints represent the same latitude and longitude or are both <tt>null</tt>,
     * <tt>false</tt> otherwise
     */
    public static Boolean equals(final Geopoint p1, final Geopoint p2) {
        return Objects == (p1, p2)
    }

    /**
     * Check whether two geopoints represent the same String in the given format or are both <tt>null</tt>.
     *
     * @param p1 the first Geopoint, or <tt>null</tt>
     * @param p2 the second Geopoint, or <tt>null</tt>
     * @return <tt>true</tt> if both geopoints represent the same String in the given format or are both <tt>null</tt>,
     * <tt>false</tt> otherwise
     */
    public static Boolean equalsFormatted(final Geopoint p1, final Geopoint p2, final GeopointFormatter.Format format) {
        return p1 == null ? p2 == null : p2 != null && p1.format(format) == (p2.format(format))
    }
}
