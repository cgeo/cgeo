package cgeo.geocaching.location;

import cgeo.geocaching.utils.MatcherWrapper;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A class representing a UTM co-ordinate.
 * <p>
 *
 * Derived from https://github.com/OpenMap-java/openmap
 *
 * Adapted to Java by Colin Mummery (colin_mummery@yahoo.com) from C++ code by
 * Chuck Gantz (chuck.gantz@globalstar.com)
 */
public class UTMPoint {

    private static final double WGS_84_RADIUS = 6378137.0d;
    private static final double WGS_84_ECC_SQUARED = 0.00669438d;
    private static final double ECC_PRIME_SQUARED = WGS_84_ECC_SQUARED / (1 - WGS_84_ECC_SQUARED);
    private static final double ECC_SQUARED_2 = WGS_84_ECC_SQUARED * WGS_84_ECC_SQUARED;
    private static final double ECC_SQUARED_3 = ECC_SQUARED_2 * WGS_84_ECC_SQUARED;
    private static final double E_1 = (1 - Math.sqrt(1 - WGS_84_ECC_SQUARED)) / (1 + Math.sqrt(1 - WGS_84_ECC_SQUARED));
    private static final double K_0 = 0.9996;
    private static final double FALSE_EASTING = 500000.0d;
    private static final double FALSE_NORTHING = 10000000.0d;

    /**
     * The northing component of the coordinate.
     */
    private final double northing;

    /**
     * The easting component of the coordinate.
     */
    private final double easting;

    /**
     * The zone number of the coordinate, must be between 1 and 60.
     */
    private final int zoneNumber;

    /**
     * A-Z
     */
    private final char zoneLetter;

    //                                                  ( 1   )(  2    )       (  3  )       (         4       )       (        5        )
    static final Pattern PATTERN_UTM = Pattern.compile("(^|\\s)(\\d\\d?)[ \\t]*([A-Z])[\\sE]+(\\d+(?:[.,]\\d+)?)[\\sN]+(\\d+(?:[.,]\\d+)?)", Pattern.CASE_INSENSITIVE);

    /**
     * Point to create if you are going to use the static methods to fill the
     * values in.
     */
    public UTMPoint(final String utmString) {
        final MatcherWrapper matcher = new MatcherWrapper(PATTERN_UTM, utmString);
        try {
            if (matcher.find()) {
                this.zoneNumber = Integer.parseInt(matcher.group(2));
                this.zoneLetter = checkZone(matcher.group(3).charAt(0));
                this.easting = Double.parseDouble(matcher.group(4).replace(",", "."));
                this.northing = Double.parseDouble(matcher.group(5).replace(",", "."));
                if (zoneNumber < 0 || zoneNumber > 60) {
                    throw new ParseException("ZoneNumber out of range [0-60] in String '" + utmString + "'");
                }

                // Filter some invalid combinations with small numbers
                if (easting < 160000) {
                    throw new ParseException("Easting too small for UTM format in String '" + utmString + "'");
                }
                if (northing < 650000 && "ABNZ".indexOf(zoneNumber) == -1) {
                    throw new ParseException("Northing too small for UTM zone in String '" + utmString + "'");
                }
            } else {
                throw new ParseException("Unable to recognize UTM format in String '" + utmString + "'");
            }
        } catch (final NumberFormatException ignored) {
            throw new ParseException("Error parsing UTM numbers in String '" + utmString + "'");
        }
    }

    /**
     * Constructs a new UTM instance.
     *
     * @param zoneNumber The zone of the coordinate.
     * @param zoneLetter For UTM, 'A' .. 'Z'
     * @param easting    The easting component.
     * @param northing   The northing component.
     * @throws IllegalArgumentException zoneLetter is out of range.
     */
    public UTMPoint(final int zoneNumber, final char zoneLetter, final double easting, final double northing) {
        this.zoneNumber = zoneNumber;
        this.zoneLetter = checkZone(zoneLetter);
        this.easting = easting;
        this.northing = northing;
    }

    /**
     * Method that provides a check for UTM zone letters. Returns an uppercase
     * version of any valid letter passed in, 'A' .. 'Z'.
     *
     * @throws IllegalArgumentException if zone letter is invalid.
     */
    private static char checkZone(final char inZone) {
        final char zone = Character.toUpperCase(inZone);

        if (zone < 'A' || zone > 'Z') {
            throw new IllegalArgumentException("Invalid UTMPoint zone letter: " + zone);
        }

        return zone;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return String representation
     */
    @Override
    @NonNull
    public String toString() {
        return String.format(Locale.getDefault(), "%d%c E %d N %d", zoneNumber, zoneLetter, Math.round(easting), Math.round(northing));
    }

    /**
     * Converts UTM coords to lat/long.
     * <p>
     * Equations from USGS Bulletin 1532 <br>
     * East Longitudes are positive, West longitudes are negative. <br>
     * North latitudes are positive, South latitudes are negative.
     *
     * @throws IllegalArgumentException if zoneNumber is out of range.
     */
    @NonNull
    public Geopoint toLatLong() {
        // check the ZoneNumber is valid
        if (zoneNumber < 0 || zoneNumber > 60) {
            throw new IllegalArgumentException("ZoneNumber out of range [0-60]: " + zoneNumber);
        }

        // remove 500,000 meter offset for longitude
        final double x = easting - FALSE_EASTING;
        final double y = zoneLetter < 'N' ? northing - FALSE_NORTHING : northing;

        // There are 60 zones with zone 1 being at West -180 to -174
        final double longOrigin = (zoneNumber - 1) * 6 - 180 + 3; // +3 puts origin in middle of zone

        final double m = y / K_0;
        final double mu = m / (WGS_84_RADIUS * (1 - WGS_84_ECC_SQUARED / 4 - 3 * ECC_SQUARED_2 / 64 - 5 * ECC_SQUARED_3 / 256));

        final double phi1Rad =
                mu + (3 * E_1 / 2 - 27 * E_1 * E_1 * E_1 / 32) * Math.sin(2 * mu) + (21 * E_1 * E_1 / 16 - 55 * E_1 * E_1 * E_1 * E_1 / 32)
                        * Math.sin(4 * mu) + (151 * E_1 * E_1 * E_1 / 96) * Math.sin(6 * mu);

        final double n1 = WGS_84_RADIUS / Math.sqrt(1 - WGS_84_ECC_SQUARED * Math.sin(phi1Rad) * Math.sin(phi1Rad));
        final double t1 = Math.tan(phi1Rad) * Math.tan(phi1Rad);
        final double c1 = ECC_PRIME_SQUARED * Math.cos(phi1Rad) * Math.cos(phi1Rad);
        final double r1 = WGS_84_RADIUS * (1 - WGS_84_ECC_SQUARED) / Math.pow(1 - WGS_84_ECC_SQUARED * Math.sin(phi1Rad) * Math.sin(phi1Rad), 1.5);
        final double d = x / (n1 * K_0);

        final double latRad =
                phi1Rad
                        - (n1 * Math.tan(phi1Rad) / r1)
                        * (d * d / 2 - (5 + 3 * t1 + 10 * c1 - 4 * c1 * c1 - 9 * ECC_PRIME_SQUARED) * d * d * d * d / 24 + (61 + 90
                        * t1 + 298 * c1 + 45 * t1 * t1 - 252 * ECC_PRIME_SQUARED - 3 * c1 * c1)
                        * d * d * d * d * d * d / 720);
        final double lonRad =
                (d - (1 + 2 * t1 + c1) * d * d * d / 6 + (5 - 2 * c1 + 28 * t1 - 3 * c1 * c1 + 8 * ECC_PRIME_SQUARED + 24 * t1 * t1)
                        * d * d * d * d * d / 120)
                        / Math.cos(phi1Rad);
        return new Geopoint(Math.toDegrees(latRad), longOrigin + Math.toDegrees(lonRad));
    }

    /**
     * Converts a set of Longitude and Latitude co-ordinates to UTM.
     *
     * @param geopoint the coordinate
     * @return An UTM class instance
     */
    @NonNull
    public static UTMPoint latLong2UTM(final Geopoint geopoint) {
        final int zoneNumber = getZoneNumber(geopoint.getLatitude(), geopoint.getLongitude());
        final double latRad = Math.toRadians(geopoint.getLatitude());
        final double longRad = Math.toRadians(geopoint.getLongitude());

        // in middle of zone
        final double longOrigin = (zoneNumber - 1) * 6 - 180 + 3; // +3 puts origin
        final double longOriginRad = Math.toRadians(longOrigin);

        final double tanLatRad = Math.tan(latRad);
        final double sinLatRad = Math.sin(latRad);
        final double cosLatRad = Math.cos(latRad);

        final double n = WGS_84_RADIUS / Math.sqrt(1 - WGS_84_ECC_SQUARED * sinLatRad * sinLatRad);
        final double t = tanLatRad * tanLatRad;
        final double c = ECC_PRIME_SQUARED * cosLatRad * cosLatRad;
        final double a = cosLatRad * (longRad - longOriginRad);

        final double m =
                WGS_84_RADIUS
                        * ((1 - WGS_84_ECC_SQUARED / 4 - 3 * ECC_SQUARED_2 / 64 - 5 * ECC_SQUARED_3 / 256) * latRad
                        - (3 * WGS_84_ECC_SQUARED / 8 + 3 * ECC_SQUARED_2 / 32 + 45 * ECC_SQUARED_3 / 1024) * Math.sin(2 * latRad)
                        + (15 * ECC_SQUARED_2 / 256 + 45 * ECC_SQUARED_3 / 1024) * Math.sin(4 * latRad) - (35 * ECC_SQUARED_3 / 3072)
                        * Math.sin(6 * latRad));

        final double utmEasting =
                K_0
                        * n
                        * (a + (1 - t + c) * a * a * a / 6.0d + (5 - 18 * t + t * t + 72 * c - 58 * ECC_PRIME_SQUARED) * a * a * a
                        * a * a / 120.0d) + FALSE_EASTING;

        final double utmNorthing =
                K_0 * (m + n
                        * Math.tan(latRad)
                        * (a * a / 2 + (5 - t + 9 * c + 4 * c * c) * a * a * a * a / 24.0d + (61 - 58 * t + t * t + 600 * c - 330 * ECC_PRIME_SQUARED)
                        * a * a * a * a * a * a / 720.0d));

        final char zoneLetter = getLetterDesignator(geopoint.getLatitude());

        return new UTMPoint(zoneNumber, zoneLetter, utmEasting, geopoint.getLatitude() < 0f ? utmNorthing + FALSE_NORTHING : utmNorthing);
    }

    /**
     * Find zone number based on the given latitude and longitude in *degrees*.
     *
     * @param lat in decimal degrees
     * @param lon in decimal degrees
     * @return zone number for UTM zone for lat, lon
     */
    private static int getZoneNumber(final double lat, final double lon) {
        // Make sure the longitude 180.00 is in Zone 60
        if (lon == 180) {
            return 60;
        }

        // Special zone for Norway
        if (lat >= 56.0f && lat < 64.0f && lon >= 3.0f && lon < 12.0f) {
            return 32;
        }

        // Special zones for Svalbard
        if (lat >= 72.0f && lat < 84.0f) {
            if (lon >= 0.0f && lon < 9.0f) {
                return 31;
            } else if (lon >= 9.0f && lon < 21.0f) {
                return 33;
            } else if (lon >= 21.0f && lon < 33.0f) {
                return 35;
            } else if (lon >= 33.0f && lon < 42.0f) {
                return 37;
            }
        }

        return (int) ((lon + 180) / 6) + 1;
    }

    /**
     * Determines the correct MGRS letter designator for the given latitude
     * returns 'Z' if latitude is outside the MGRS limits of 84N to 80S.
     *
     * TODO: maybe we should handle the zones A, B, Y and Z
     *
     * @param lat The float value of the latitude.
     * @return A char value which is the MGRS zone letter.
     */
    private static char getLetterDesignator(final double lat) {
        if ((84 >= lat) && (lat >= 72)) {
            return 'X';
        } else if ((72 > lat) && (lat >= 64)) {
            return 'W';
        } else if ((64 > lat) && (lat >= 56)) {
            return 'V';
        } else if ((56 > lat) && (lat >= 48)) {
            return 'U';
        } else if ((48 > lat) && (lat >= 40)) {
            return 'T';
        } else if ((40 > lat) && (lat >= 32)) {
            return 'S';
        } else if ((32 > lat) && (lat >= 24)) {
            return 'R';
        } else if ((24 > lat) && (lat >= 16)) {
            return 'Q';
        } else if ((16 > lat) && (lat >= 8)) {
            return 'P';
        } else if ((8 > lat) && (lat >= 0)) {
            return 'N';
        } else if ((0 > lat) && (lat >= -8)) {
            return 'M';
        } else if ((-8 > lat) && (lat >= -16)) {
            return 'L';
        } else if ((-16 > lat) && (lat >= -24)) {
            return 'K';
        } else if ((-24 > lat) && (lat >= -32)) {
            return 'J';
        } else if ((-32 > lat) && (lat >= -40)) {
            return 'H';
        } else if ((-40 > lat) && (lat >= -48)) {
            return 'G';
        } else if ((-48 > lat) && (lat >= -56)) {
            return 'F';
        } else if ((-56 > lat) && (lat >= -64)) {
            return 'E';
        } else if ((-64 > lat) && (lat >= -72)) {
            return 'D';
        } else if ((-72 > lat) && (lat >= -80)) {
            return 'C';
        }

        // This is here as an error flag to show that the Latitude is outside MGRS limits
        return 'Z';
    }

    public int getZoneNumber() {
        return this.zoneNumber;
    }

    public char getZoneLetter() {
        return this.zoneLetter;
    }

    public double getEasting() {
        return easting;
    }

    public double getNorthing() {
        return northing;
    }

    public static class ParseException extends NumberFormatException {
        private static final long serialVersionUID = 1L;

        public ParseException(final String msg) {
            super(msg);
        }
    }

}
