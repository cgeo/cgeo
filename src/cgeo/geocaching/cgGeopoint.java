package cgeo.geocaching;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstraction of geographic point.
 */
public class cgGeopoint
{
    public static final double kmInMiles = 1 / 1.609344;
    public static final double deg2rad   = Math.PI / 180;
    public static final double rad2deg   = 180 / Math.PI;
    public static final float  erad      = 6371.0f;

    private enum LatLon
    {
        LAT,
        LON
    }
    
    private double latitude;
    private double longitude;

    /**
     * Creates new cgGeopoint with latitude and longitude set to 0.
     */
    public cgGeopoint()
    {
        setLatitude(0);
        setLongitude(0);
    }

    /**
     * Creates new cgGeopoint with given latitude and longitude (both degree).
     *
     * @param lat latitude
     * @param lon longitude
     */
    public cgGeopoint(final double lat, final double lon)
    {
        setLatitude(lat);
        setLongitude(lon);
    }

    /**
     * Creates new cgGeopoint with latitude and longitude parsed from string.
     *
     * @param text string to parse
     * @see parse()
     */
    public cgGeopoint(final String text)
    {
        setLatitude(parseLatitude(text));
        setLongitude(parseLongitude(text));
    }

    /**
     * Creates new cgGeopoint with given cgGeopoint. This is similar to clone().
     *
     * @param gp the gcGeopoint to clone
     */
    public cgGeopoint(final cgGeopoint gp)
    {
        this(gp.getLatitude(), gp.getLongitude());
    }

    /**
     * Parses a pair of coordinates (latitude and longitude) out of a String.
     * Accepts following formats and combinations of it:
     *      X DD
     *      X DD°
     *      X DD° MM
     *      X DD° MM.MMM
     *      X DD° MM SS
     *
     * as well as:
     *      DD.DDDDDDD
     *
     * Both . and , are accepted, also variable count of spaces (also 0)
     *
     * @param text the string to parse
     * @return an cgGeopoint with parsed latitude and longitude
     * @throws ParseException if lat or lon could not be parsed
     */
    public static cgGeopoint parse(final String text)
    {
        
        double lat = parseLatitude(text);
        double lon = parseLongitude(text);
        
        return new cgGeopoint(lat, lon);
    }
    
    /* (non JavaDoc)
     * Helper for coordinates-parsing.
     */
    private static double parseHelper(final String text, final LatLon latlon)
    {
        final Pattern patternLat = Pattern.compile("([NS])\\s*(\\d+)°?(\\s*(\\d+)([\\.,](\\d+)|'?\\s*(\\d+)(''|\")?)?)?");
        final Pattern patternLon = Pattern.compile("([WE])\\s*(\\d+)°?(\\s*(\\d+)([\\.,](\\d+)|'?\\s*(\\d+)(''|\")?)?)?");
        final Pattern patternDec = Pattern.compile("^(-?\\d+([\\.,]\\d+)?)\\s*(-?\\d+([\\.,]\\d+)?)?$");

        Matcher matcher;

        if (LatLon.LAT == latlon)
        {
            matcher = patternLat.matcher(text);
        }
        else
        {
            matcher = patternLon.matcher(text);
        }

        if (matcher.find())
        {
            int sign      = 1;
            int degree    = 0;
            int minutes   = 0;
            int seconds   = 0;
            
            if (matcher.group(1).equalsIgnoreCase("S") || matcher.group(1).equalsIgnoreCase("W"))
            {
                sign = -1;
            }
            
            degree = Integer.parseInt(matcher.group(2));
            
            if (null != matcher.group(4))
            {
                minutes = Integer.parseInt(matcher.group(4));
                
                if (null != matcher.group(6))
                {
                    seconds = Math.round(Float.parseFloat("0." + matcher.group(6)) * 60);
                }
                else if (null != matcher.group(7))
                {
                    seconds = Integer.parseInt(matcher.group(7));
                }
            }
            
            return (double) sign * ((double) degree + (double) minutes / 60 + (double) seconds / 3600);
        }
        else // Nothing found with "N 52...", try to match string as decimaldegree
        {
            matcher = patternDec.matcher(text);

            if (matcher.find())
            {
                if (LatLon.LAT == latlon)
                {
                    return Double.parseDouble(matcher.group(1).replaceAll(",", "."));
                }
                else if (null != matcher.group(3))
                {
                    return Double.parseDouble(matcher.group(3).replaceAll(",", "."));
                }
            }
        }
        
        throw new ParseException("Could not parse coordinates as " + latlon + ": \"" + text + "\"");
    }

    /**
     * Parses latitude out of a given string.
     *
     * @see parse()
     * @param text the string to be parsed
     * @return the latitude as decimaldegree
     * @throws ParseException if latitude could not be parsed
     */
    public static double parseLatitude(final String text)
    {
        return parseHelper(text, LatLon.LAT);
    }

    /**
     * Parses longitude out of a given string.
     *
     * @see parse()
     * @param text the string to be parsed
     * @return the longitude as decimaldegree
     * @throws ParseException if longitude could not be parsed
     */
    public static double parseLongitude(final String text)
    {
        return parseHelper(text, LatLon.LON);
    }

    /**
     * Set latitude in degree.
     *
     * @param lat latitude
     * @return this
     * @throws MalformedCoordinateException if not -90 <= lat <= 90
     */
    public cgGeopoint setLatitude(final double lat)
    {
        if (lat <= 90 && lat >= -90)
        {
            latitude  = lat;
        }
        else
        {
            throw new MalformedCoordinateException("malformed latitude: " + lat);
        }

        return this;
    }
    
    /**
     * Set latitude in microdegree.
     *
     * @param lat latitude
     * @return this
     * @see setLatitude(final double lat)
     * @throws MalformedCoordinateException if not -90 <= (lat * 1E-6) <= 90
     */
    public cgGeopoint setLatitudeE6(final int lat)
    {
        return setLatitude(lat * 1E-6);
    }

    /**
     * Set latitude by parsing string.
     *
     * @param lat latitude
     * @return this
     * @see setLatitude(final double lat)
     * @throws ParseException if lat could not be parsed
     * @throws MalformedCoordinateException if not -90 <= lat <= 90
     */
    public cgGeopoint setLatitude(final String lat)
    {
        return setLatitude(parseLatitude(lat));
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
     * Set longitude in degree.
     *
     * @param lon longitude
     * @return this
     * @throws MalformedCoordinateException if not -180 <= lon <= 180
     */
    public cgGeopoint setLongitude(final double lon)
    {
        if (lon <= 180 && lon >=-180)
        {
            longitude = lon;
        }
        else
        {
            throw new MalformedCoordinateException("malformed longitude: " + lon);
        }

        return this;
    }
    
    /**
     * Set longitude in microdegree.
     *
     * @param lon longitude
     * @return this
     * @see setLongitude(final double lon)
     * @throws MalformedCoordinateException if not -180 <= (lon * 1E-6) <= 180
     */
    public cgGeopoint setLongitudeE6(final int lon)
    {
        return setLongitude(lon * 1E-6);
    }

    /**
     * Set longitude by parsing string.
     *
     * @param lon longitude
     * @return this
     * @see setLongitude(final double lon)
     * @throws ParseException if lon could not be parsed
     * @throws MalformedCoordinateException if not -180 <= lon <= 180
     */
    public cgGeopoint setLongitude(final String lon)
    {
        return setLongitude(parseLongitude(lon));
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
     * Calculates distance to given cgGeopoint in km.
     *
     * @param gp target
     * @return distance in km
     * @throws GeopointException if there is an error in distance calculation
     */
    public double distanceTo(final cgGeopoint gp)
    {
        final double lat1 = deg2rad * latitude;
        final double lon1 = deg2rad * longitude;
        final double lat2 = deg2rad * gp.getLatitude();
        final double lon2 = deg2rad * gp.getLongitude();

        final double d = Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2);
        final double distance = erad * Math.acos(d); // distance in km

        if (!Double.isNaN(distance) && distance > 0)
        {
            return distance;
        }
        else
        {
            throw new GeopointException("Error in distance calculation.");
        }
    }

    /**
     * Calculates bearing to given cgGeopoint in degree.
     *
     * @param gp target
     * @return bearing in degree.
     */
    public double bearingTo(final cgGeopoint gp)
    {
        final int ilat1 = (int) Math.round(0.5 + latitude * 360000);
        final int ilon1 = (int) Math.round(0.5 + longitude * 360000);
        final int ilat2 = (int) Math.round(0.5 + gp.getLatitude() * 360000);
        final int ilon2 = (int) Math.round(0.5 + gp.getLongitude() * 360000);

        final double lat1 = deg2rad * latitude;
        final double lon1 = deg2rad * longitude;
        final double lat2 = deg2rad * gp.getLatitude();
        final double lon2 = deg2rad * gp.getLongitude();

        if (ilat1 == ilat2 && ilon1 == ilon2)
        {
            return 0;
        }
        else if (ilat1 == ilat2)
        {
            if (ilon1 > ilon2)
            {
                return 270;
            }
            else
            {
                return 90;
            }
        }
        else if (ilon1 == ilon2)
        {
            if (ilat1 > ilat2)
            {
                return 180;
            }
            else
            {
                return 0;
            }
        }
        else
        {
            double c = Math.acos(Math.sin(lat2) * Math.sin(lat1) + Math.cos(lat2) * Math.cos(lat1) * Math.cos(lon2 - lon1));
            double A = Math.asin(Math.cos(lat2) * Math.sin(lon2 - lon1) / Math.sin(c));
            double result = A * rad2deg;
            
            if (ilat2 > ilat1 && ilon2 > ilon1)
            {
                // result don't need change
            }
            else if (ilat2 < ilat1 && ilon2 < ilon1)
            {
                result = 180f - result;
            }
            else if (ilat2 < ilat1 && ilon2 > ilon1)
            {
                result = 180f - result;
            }
            else if (ilat2 > ilat1 && ilon2 < ilon1)
            {
                result += 360f;
            }
            
            return result;
        }
    }

    /**
     * Calculates geopoint from given bearing and distance.
     *
     * @param bearing bearing in degree
     * @param distance distance in km
     * @return the projected geopoint
     */
    public cgGeopoint project(final double bearing, final double distance)
    {
        final double rlat1     = latitude * deg2rad;
        final double rlon1     = longitude * deg2rad;
        final double rbearing  = bearing * deg2rad;
        final double rdistance = distance / erad;

        final double rlat = Math.asin(Math.sin(rlat1) * Math.cos(rdistance) + Math.cos(rlat1) * Math.sin(rdistance) * Math.cos(rbearing));
        final double rlon = rlon1 + Math.atan2(Math.sin(rbearing) * Math.sin(rdistance) * Math.cos(rlat1), Math.cos(rdistance) - Math.sin(rlat1) * Math.sin(rlat));

        return new cgGeopoint(rlat * rad2deg, rlon * rad2deg);
    }

    /**
     * Checks if given cgGeopoint is identical with this cgGeopoint.
     *
     * @param gp cgGeopoint to check
     * @return true if identical, false otherwise
     */
    public boolean equals(cgGeopoint gp)
    {
        return (gp.getLatitude() == latitude && gp.getLongitude() == longitude);
    }

    /**
     * Checks if given cgGeopoint is similar to this cgGeopoint with tolerance.
     *
     * @param gp cgGeopoint to check
     * @param tolerance tolerance in km
     * @return true if similar, false otherwise
     */
    public boolean equals(cgGeopoint gp, double tolerance)
    {
        return (distanceTo(gp) <= tolerance);
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format the desired format
     * @see cgeo.geocaching.cgGeopointFormatter
     * @return formatted coordinates
     */
    public String format(cgGeopointFormatter format)
    {
        return format.format(this);
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format the desired format
     * @see cgeo.geocaching.cgGeopointFormatter
     * @return formatted coordinates
     */
    public String format(String format)
    {
        return cgGeopointFormatter.format(format, this);
    }

    /**
     * Returns formatted coordinates.
     *
     * @param format the desired format
     * @see cgeo.geocaching.cgGeopointFormatter
     * @return formatted coordinates
     */
    public String format(cgGeopointFormatter.Format format)
    {
        return cgGeopointFormatter.format(format, this);
    }

    /**
     * Returns formatted coordinates with default format.
     * Default format is decimalminutes, e.g. N 52° 36.123 E 010° 03.456
     *
     * @return formatted coordinates
     */
    public String toString()
    {
        return format(cgGeopointFormatter.Format.LAT_LON_DECMINUTE);
    }

    public static class GeopointException
        extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public GeopointException(String msg)
        {
            super(msg);
        }
    }

    public static class ParseException
        extends GeopointException
    {
        private static final long serialVersionUID = 1L;

        public ParseException(String msg)
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
