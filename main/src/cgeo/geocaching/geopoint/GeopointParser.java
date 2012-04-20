package cgeo.geocaching.geopoint;

import cgeo.geocaching.R;
import cgeo.geocaching.geopoint.Geopoint.GeopointException;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse coordinates.
 */
public class GeopointParser
{
    private static class ResultWrapper {
        final double result;
        final int matcherPos;
        final int matcherLength;

        public ResultWrapper(final double result, int matcherPos, int stringLength) {
            this.result = result;
            this.matcherPos = matcherPos;
            this.matcherLength = stringLength;
        }
    }

    //                                                         ( 1  )    ( 2  )         ( 3  )       ( 4  )       (        5        )
    private static final Pattern patternLat = Pattern.compile("\\b([NS])\\s*(\\d+)°?(?:\\s*(\\d+)(?:[.,](\\d+)|'?\\s*(\\d+(?:[.,]\\d+)?)(?:''|\")?)?)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLon = Pattern.compile("\\b([WE])\\s*(\\d+)°?(?:\\s*(\\d+)(?:[.,](\\d+)|'?\\s*(\\d+(?:[.,]\\d+)?)(?:''|\")?)?)?", Pattern.CASE_INSENSITIVE);

    private enum LatLon
    {
        LAT,
        LON
    }

    /**
     * Parses a pair of coordinates (latitude and longitude) out of a String.
     * Accepts following formats and combinations of it:
     * X DD
     * X DD°
     * X DD° MM
     * X DD° MM.MMM
     * X DD° MM SS
     *
     * as well as:
     * DD.DDDDDDD
     *
     * Both . and , are accepted, also variable count of spaces (also 0)
     *
     * @param text
     *            the string to parse
     * @return an Geopoint with parsed latitude and longitude
     * @throws ParseException
     *             if lat or lon could not be parsed
     */
    public static Geopoint parse(final String text)
    {
        final ResultWrapper latitudeWrapper = parseHelper(text, LatLon.LAT);
        final double lat = latitudeWrapper.result;
        // cut away the latitude part when parsing the longitude
        final ResultWrapper longitudeWrapper = parseHelper(text.substring(latitudeWrapper.matcherPos + latitudeWrapper.matcherLength), LatLon.LON);

        if (longitudeWrapper.matcherPos - (latitudeWrapper.matcherPos + latitudeWrapper.matcherLength) >= 10) {
            throw new ParseException("Distance between latitude and longitude text is to large.", LatLon.LON);
        }

        final double lon = longitudeWrapper.result;
        return new Geopoint(lat, lon);
    }

    /**
     * Parses a pair of coordinates (latitude and longitude) out of a String.
     * Accepts following formats and combinations of it:
     * X DD
     * X DD°
     * X DD° MM
     * X DD° MM.MMM
     * X DD° MM SS
     *
     * as well as:
     * DD.DDDDDDD
     *
     * Both . and , are accepted, also variable count of spaces (also 0)
     *
     * @param latitude
     *            the latitude string to parse
     * @param longitude
     *            the longitude string to parse
     * @return an Geopoint with parsed latitude and longitude
     * @throws ParseException
     *             if lat or lon could not be parsed
     */
    public static Geopoint parse(final String latitude, final String longitude)
    {
        final double lat = parseLatitude(latitude);
        final double lon = parseLongitude(longitude);

        return new Geopoint(lat, lon);
    }

    /*
     * (non JavaDoc)
     * Helper for coordinates-parsing.
     */
    private static ResultWrapper parseHelper(final String text, final LatLon latlon)
    {

        final Pattern pattern = LatLon.LAT == latlon ? patternLat : patternLon;
        final Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            final double sign = matcher.group(1).equalsIgnoreCase("S") || matcher.group(1).equalsIgnoreCase("W") ? -1.0 : 1.0;
            final double degree = Integer.valueOf(matcher.group(2)).doubleValue();

            double minutes = 0.0;
            double seconds = 0.0;

            if (null != matcher.group(3)) {
                minutes = Integer.valueOf(matcher.group(3)).doubleValue();

                if (null != matcher.group(4)) {
                    seconds = Double.parseDouble("0." + matcher.group(4)) * 60.0;
                } else if (null != matcher.group(5)) {
                    seconds = Double.parseDouble(matcher.group(5).replace(",", "."));
                }
            }

            return new ResultWrapper(sign * (degree + minutes / 60.0 + seconds / 3600.0), matcher.start(), matcher.group().length());

        }

        // Nothing found with "N 52...", try to match string as decimaldegree
        try {
            final String[] items = StringUtils.split(text.trim());
            if (items.length > 0) {
                final int index = (latlon == LatLon.LON ? items.length - 1 : 0);
                final int pos = (latlon == LatLon.LON ? text.lastIndexOf(items[index]) : text.indexOf(items[index]));
                return new ResultWrapper(Double.parseDouble(items[index]), pos, items[index].length());
            }
        } catch (NumberFormatException e) {
            // The right exception will be raised below.
        }

        throw new ParseException("Could not parse coordinates as " + latlon + ": \"" + text + "\"", latlon);
    }

    /**
     * Parses latitude out of a given string.
     *
     * @see parse()
     * @param text
     *            the string to be parsed
     * @return the latitude as decimal degree
     * @throws ParseException
     *             if latitude could not be parsed
     */
    public static double parseLatitude(final String text)
    {
        return parseHelper(text, LatLon.LAT).result;
    }

    /**
     * Parses longitude out of a given string.
     *
     * @see parse()
     * @param text
     *            the string to be parsed
     * @return the longitude as decimal degree
     * @throws ParseException
     *             if longitude could not be parsed
     */
    public static double parseLongitude(final String text)
    {
        return parseHelper(text, LatLon.LON).result;
    }

    public static class ParseException
            extends GeopointException
    {
        private static final long serialVersionUID = 1L;
        public final int resource;

        public ParseException(final String msg, final LatLon faulty)
        {
            super(msg);
            resource = faulty == LatLon.LAT ? R.string.err_parse_lat : R.string.err_parse_lon;
        }
    }
}
