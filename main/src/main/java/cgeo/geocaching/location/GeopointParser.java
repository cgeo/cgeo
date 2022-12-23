package cgeo.geocaching.location;

import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Parse coordinates.
 */
public class GeopointParser {

    private static final Pattern PATTERN_BAD_BLANK_COMMA = Pattern.compile("(\\d), (\\d{2,})");
    private static final Pattern PATTERN_BAD_BLANK_DOT = Pattern.compile("(\\d)\\. (\\d{2,})");

    private static final List<AbstractParser> parsers = Arrays.asList(new MinDecParser(), new MinParser(), new DegParser(), new DMSParser(), new ShortDMSParser(), new DegDecParser(), new ShortDegDecParser(), new UTMParser());

    private GeopointParser() {
        // utility class
    }

    private static class ResultWrapper {
        private final double result;
        private final int matcherLength;

        ResultWrapper(final double result, final int stringLength) {
            this.result = result;
            this.matcherLength = stringLength;
        }
    }

    /**
     * Abstract parser for coordinate formats.
     */
    private abstract static class AbstractParser {
        /**
         * Parses coordinates out of the given string.
         *
         * @param text the string to be parsed
         * @return a wrapper with the parsed coordinates and the length of the match, or null if parsing failed
         */
        @Nullable
        public abstract GeopointWrapper parse(@NonNull String text);

        /**
         * Parses latitude or longitude out of the given string.
         *
         * @param text   the string to be parsed
         * @param latlon whether to parse latitude or longitude
         * @return a wrapper with the parsed latitude/longitude and the length of the match, or null if parsing failed
         */
        @Nullable
        public abstract ResultWrapper parse(@NonNull String text, @NonNull Geopoint.LatLon latlon);
    }

    /**
     * Abstract parser for coordinates that consist of two syntactic parts: latitude and longitude.
     */
    private abstract static class AbstractLatLonParser extends AbstractParser {
        private final Pattern latPattern;
        private final Pattern lonPattern;
        private final Pattern latLonPattern;

        AbstractLatLonParser(@NonNull final Pattern latPattern, @NonNull final Pattern lonPattern, @NonNull final Pattern latLonPattern) {
            this.latPattern = latPattern;
            this.lonPattern = lonPattern;
            this.latLonPattern = latLonPattern;
        }

        /**
         * Creates latitude or longitude out of matches groups for sign, degrees, minutes and seconds.
         *
         * @param signGroup    a string representing the sign of the coordinate, ignored if empty
         * @param degreesGroup a string representing the degrees of the coordinate, ignored if empty
         * @param minutesGroup a string representing the minutes of the coordinate, ignored if empty
         * @param secondsGroup a string representing the seconds of the coordinate, ignored if empty
         * @return the latitude/longitude in decimal degrees, or null if creation failed
         */
        @Nullable
        protected Double createCoordinate(@NonNull final String signGroup, @NonNull final String degreesGroup, @NonNull final String minutesGroup, @NonNull final String secondsGroup) {
            try {
                final double seconds = Double.parseDouble(StringUtils.defaultIfEmpty(secondsGroup, "0"));
                if (seconds >= 60.0) {
                    return null;
                }

                final double minutes = Double.parseDouble(StringUtils.defaultIfEmpty(minutesGroup, "0"));
                if (minutes >= 60.0) {
                    return null;
                }

                final double degrees = Double.parseDouble(StringUtils.defaultIfEmpty(degreesGroup, "0"));
                final double sign = signGroup.equalsIgnoreCase("S") || signGroup.equalsIgnoreCase("W") ? -1.0 : 1.0;
                return sign * (degrees + minutes / 60.0 + seconds / 3600.0);
            } catch (final NumberFormatException ignored) {
                // We might have encountered too large a number
            }

            return null;
        }

        /**
         * Checks whether is not zero.
         *
         * @return true if the given coordinate does not represent a zero.
         */
        protected boolean isNotZero(@Nullable final Double coordinate) {
            return coordinate == null || Double.doubleToRawLongBits(coordinate) != 0L;
        }

        /**
         * Parses latitude or longitude out of a given range of matched groups.
         *
         * @param matcher the matcher that holds the matches groups
         * @param first   the first group to parse
         * @param last    the last group to parse
         * @return the parsed latitude/longitude, or null if parsing failed
         */
        @Nullable
        private Double parseGroups(@NonNull final MatcherWrapper matcher, final int first, final int last) {
            final List<String> groups = new ArrayList<>(last - first + 1);
            for (int i = first; i <= last; i++) {
                groups.add(matcher.group(i));
            }

            return parse(groups);
        }

        /**
         * @see AbstractParser#parse(String)
         */
        @Override
        @Nullable
        public final GeopointWrapper parse(@NonNull final String text) {
            final MatcherWrapper matcher = new MatcherWrapper(latLonPattern, text);
            if (matcher.find()) {
                final int groupCount = matcher.groupCount();
                final int partCount = groupCount / 2;

                final Double lat = parseGroups(matcher, 1, partCount);
                if (lat == null || !Geopoint.isValidLatitude(lat)) {
                    return null;
                }

                final Double lon = parseGroups(matcher, partCount + 1, groupCount);
                if (lon == null || !Geopoint.isValidLongitude(lon)) {
                    return null;
                }

                return new GeopointWrapper(new Geopoint(lat, lon), matcher.start(), matcher.group().length(), text);
            }

            return null;
        }

        /**
         * @see AbstractParser#parse(String, Geopoint.LatLon)
         */
        @Override
        @Nullable
        public final ResultWrapper parse(@NonNull final String text, @NonNull final Geopoint.LatLon latlon) {
            final MatcherWrapper matcher = new MatcherWrapper(latlon == Geopoint.LatLon.LAT ? latPattern : lonPattern, text);
            if (matcher.find()) {
                final Double res = parseGroups(matcher, 1, matcher.groupCount());
                if (res != null) {
                    return new ResultWrapper(res, matcher.group().length());
                }
            }

            return null;
        }

        /**
         * Parses latitude or longitude from matched groups of corresponding pattern.
         *
         * @param groups the groups matched by latitude/longitude pattern
         * @return parsed latitude/longitude, or null if parsing failed
         */
        @Nullable
        public abstract Double parse(@NonNull List<String> groups);
    }

    /**
     * Parser for partial MinDec format: X DD°.
     */
    private static final class DegParser extends AbstractLatLonParser {
        //                                           (  1  )    (  2  )
        private static final String STRING_LAT = "\\b([NS]?)\\s*(\\d++)°";

        //                                        (   1  )    (  2  )
        private static final String STRING_LON = "([WEO]?)\\s*(\\d++)\\b°";
        private static final String STRING_SEPARATOR = "[^\\w'′\"″°=]*";
        private static final Pattern PATTERN_LAT = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LON = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LATLON = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE);

        DegParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON);
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        @Override
        @Nullable
        public Double parse(@NonNull final List<String> groups) {
            final String group1 = groups.get(0);
            final String group2 = groups.get(1);
            final Double result = createCoordinate(group1, group2, "", "");
            if (StringUtils.isBlank(group1) && isNotZero(result)) {
                return null;
            }

            return result;
        }
    }

    /**
     * Parser for partial MinDec format: X DD° MM'.
     */
    private static final class MinParser extends AbstractLatLonParser {
        //                                           (  1  )    (  2  )( 3)    (  4  )
        private static final String STRING_LAT = "\\b([NS]?)\\s*(\\d++)(°?)\\s*(\\d++)['′]?";

        //                                        (   1  )    (  2  )( 3)    (  4  )
        private static final String STRING_LON = "([WEO]?)\\s*(\\d++)(°?)\\s*(\\d++)\\b['′]?";
        private static final String STRING_SEPARATOR = "[^\\w'′\"″°]*";
        private static final Pattern PATTERN_LAT = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LON = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LATLON = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE);

        MinParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON);
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        @Override
        @Nullable
        public Double parse(@NonNull final List<String> groups) {
            final String group1 = groups.get(0);
            final String group2 = groups.get(1);
            final String group3 = groups.get(2);
            final String group4 = groups.get(3);
            final Double result = createCoordinate(group1, group2, group4, "");
            if (StringUtils.isBlank(group1) && (StringUtils.isBlank(group3) || isNotZero(result))) {
                return null;
            }

            return result;
        }
    }

    /**
     * Parser for MinDec format: X DD° MM.MMM'.
     */
    private static final class MinDecParser extends AbstractLatLonParser {
        //                                           (  1  )    (    2    )    (      3      )
        private static final String STRING_LAT = "\\b([NS]?)\\s*(\\d++°?|°)\\s*(\\d++\\.\\d++)['′]?";

        //                                        (   1  )    (    2    )    (      3      )
        private static final String STRING_LON = "([WEO]?)\\s*(\\d++°?|°)\\s*(\\d++\\.\\d++)\\b['′]?";
        private static final String STRING_SEPARATOR = "[^\\w'′\"″°.]*";
        private static final Pattern PATTERN_LAT = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LON = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LATLON = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE);

        MinDecParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON);
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        @Override
        @Nullable
        public Double parse(@NonNull final List<String> groups) {
            final String group1 = groups.get(0);
            final String group2 = groups.get(1);
            final String group3 = groups.get(2);

            // Handle empty degrees part (see #4620)
            final String strippedGroup2 = StringUtils.stripEnd(group2, "°");
            final Double result = createCoordinate(group1, strippedGroup2, group3, "");
            if (StringUtils.isBlank(group1) && (!StringUtils.endsWith(group2, "°") || isNotZero(result))) {
                return null;
            }

            return result;
        }
    }

    /**
     * Parser for DMS format: X DD° MM' SS.SS".
     */
    private static final class DMSParser extends AbstractLatLonParser {
        //                                           (  1  )    (  2  )( 3)    (  4  )         (      5      )
        private static final String STRING_LAT = "\\b([NS]?)\\s*(\\d++)(°?)\\s*(\\d++)['′]?\\s*(\\d++\\.\\d++)(?:''|\"|″)?";

        //                                        (   1  )    (  2  )( 3)    (  4  )         (      5      )
        private static final String STRING_LON = "([WEO]?)\\s*(\\d++)(°?)\\s*(\\d++)['′]?\\s*(\\d++\\.\\d++)\\b(?:''|\"|″)?";
        private static final String STRING_SEPARATOR = "[^\\w'′\"″°.]*";
        private static final Pattern PATTERN_LAT = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LON = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LATLON = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE);

        DMSParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON);
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        @Override
        @Nullable
        public Double parse(@NonNull final List<String> groups) {
            final String group1 = groups.get(0);
            final String group2 = groups.get(1);
            final String group3 = groups.get(2);
            final String group4 = groups.get(3);
            final String group5 = groups.get(4);
            final Double result = createCoordinate(group1, group2, group4, group5);
            if (StringUtils.isBlank(group1) && (StringUtils.isBlank(group3) || isNotZero(result))) {
                return null;
            }

            return result;
        }
    }

    /**
     * Parser for DMS format: X DD° MM' SS".
     */
    private static final class ShortDMSParser extends AbstractLatLonParser {
        //                                           (  1  )    (  2  )( 3)    (  4  )         (  5  )
        private static final String STRING_LAT = "\\b([NS]?)\\s*(\\d++)(°?)\\s*(\\d++)['′]?\\s*(\\d++)(?:''|\"|″)?";

        //                                        (   1  )    (  2  )( 3)    (  4  )         (  5  )
        private static final String STRING_LON = "([WEO]?)\\s*(\\d++)(°?)\\s*(\\d++)['′]?\\s*(\\d++)\\b(?:''|\"|″)?";
        private static final String STRING_SEPARATOR = "[^\\w'′\"″°]*";
        private static final Pattern PATTERN_LAT = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LON = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LATLON = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE);

        ShortDMSParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON);
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        @Override
        @Nullable
        public Double parse(@NonNull final List<String> groups) {
            final String group1 = groups.get(0);
            final String group2 = groups.get(1);
            final String group3 = groups.get(2);
            final String group4 = groups.get(3);
            final String group5 = groups.get(4);
            final Double result = createCoordinate(group1, group2, group4, group5);
            if (StringUtils.isBlank(group1) && (StringUtils.isBlank(group3) || isNotZero(result))) {
                return null;
            }

            return result;
        }
    }

    /**
     * Parser for DegDec format: DD.DDDDDDD°.
     */
    private static final class DegDecParser extends AbstractLatLonParser {
        //                                        (       1       )
        private static final String STRING_LAT = "(-?\\d++\\.\\d++)°?";

        //                                        (       1       )
        private static final String STRING_LON = "(-?\\d++\\.\\d++)\\b°?";
        private static final String STRING_SEPARATOR = "[^\\w'′\"″°.=-]*";
        private static final Pattern PATTERN_LAT = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LON = Pattern.compile(STRING_LON, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LATLON = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE);

        DegDecParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON);
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        @Override
        @Nullable
        public Double parse(@NonNull final List<String> groups) {
            final String group1 = groups.get(0);
            return createCoordinate("", group1, "", "");
        }
    }

    /**
     * Parser for DegDec format: -DD°.
     */
    private static final class ShortDegDecParser extends AbstractLatLonParser {
        //                                               (   1   )
        private static final String STRING_LAT_OR_LON = "(-?\\d++)°";
        private static final String STRING_SEPARATOR = "[^\\w'′\"″°-]*";
        private static final Pattern PATTERN_LAT_OR_LON = Pattern.compile(STRING_LAT_OR_LON, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LATLON = Pattern.compile(STRING_LAT_OR_LON + STRING_SEPARATOR + STRING_LAT_OR_LON, Pattern.CASE_INSENSITIVE);

        ShortDegDecParser() {
            super(PATTERN_LAT_OR_LON, PATTERN_LAT_OR_LON, PATTERN_LATLON);
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        @Override
        @Nullable
        public Double parse(@NonNull final List<String> groups) {
            final String group1 = groups.get(0);
            return createCoordinate("", group1, "", "");
        }
    }

    /**
     * Parser for UTM format: ZZZ E EEEEEE N NNNNNNN
     */
    private static final class UTMParser extends AbstractParser {
        /**
         * @see AbstractParser#parse(String)
         */
        @Override
        @Nullable
        public GeopointWrapper parse(@NonNull final String text) {
            final MatcherWrapper matcher = new MatcherWrapper(UTMPoint.PATTERN_UTM, text);
            if (matcher.find()) {
                try {
                    final UTMPoint utmPoint = new UTMPoint(text);
                    return new GeopointWrapper(utmPoint.toLatLong(), matcher.start(), matcher.group().length(), text);
                } catch (final Exception ignored) {
                    // Ignore parse errors
                }
            }
            return null;
        }

        /**
         * @see AbstractParser#parse(String, Geopoint.LatLon)
         */
        @Override
        @Nullable
        public ResultWrapper parse(@NonNull final String text, @NonNull final Geopoint.LatLon latlon) {
            return null;
        }
    }

    /**
     * Returns a set of parser inputs for a given text
     *
     * The generated inputs use different delimiters for fractional numbers.
     *
     * @param text the text to parse
     * @return the set of parser inputs
     */
    @NonNull
    private static Set<String> getParseInputs(@NonNull final String text) {
        final String inputDot = removeSpaceAfterSeparators(text);
        final String inputComma = swapDotAndComma(inputDot);
        return CollectionStream.of(new String[]{inputDot, inputComma}).toSet();
    }

    /**
     * Removes all single spaces after a comma (see #2404)
     *
     * @param text the string to substitute
     * @return the substituted string without the single spaces
     */
    @NonNull
    private static String removeSpaceAfterSeparators(@NonNull final String text) {
        final String replacedComma = new MatcherWrapper(PATTERN_BAD_BLANK_COMMA, text).replaceAll("$1,$2");
        return new MatcherWrapper(PATTERN_BAD_BLANK_DOT, replacedComma).replaceAll("$1.$2");
    }

    private static String swapDotAndComma(@NonNull final String text) {
        final char[] characterArray = text.toCharArray();
        for (int i = 0; i < characterArray.length; i++) {
            if (characterArray[i] == '.') {
                characterArray[i] = ',';
            } else if (characterArray[i] == ',') {
                characterArray[i] = '.';
            }
        }

        return new String(characterArray);
    }

    /**
     * Parses latitude/longitude from the given string.
     *
     * @param text   the text to parse
     * @param latlon whether to parse latitude or longitude
     * @return a wrapper with the best latitude/longitude and the length of the match, or null if parsing failed
     */
    @Nullable
    private static ResultWrapper parseHelper(@NonNull final String text, @NonNull final Geopoint.LatLon latlon) {
        final Set<String> inputs = getParseInputs(text.trim());
        for (final AbstractParser parser : parsers) {
            for (final String input : inputs) {
                final ResultWrapper wrapper = parser.parse(input, latlon);
                if (wrapper != null && wrapper.matcherLength == input.length()) {
                    return wrapper;
                }
            }
        }

        return null;
    }

    /**
     * same as {@link #parse(String)}, but returns a provided default Value (instead of throwing exception) if parsing fails
     */
    @NonNull
    public static Geopoint parse(@NonNull final String text, @Nullable final Geopoint defaultValue) {
        try {
            return parse(text);
        } catch (Geopoint.ParseException pe) {
            Log.d("Parsing of a coordinate failed (default is returned): " + pe.getMessage());
            return defaultValue;
        }
    }

    /**
     * Parses a pair of coordinates (latitude and longitude) out of the given string.
     *
     * Accepts following formats:
     * - X DD
     * - X DD°
     * - X DD° MM
     * - X DD° MM.MMM
     * - X DD° MM SS
     * - DD.DDDDDDD
     * - UTM
     *
     * Both . and , are accepted, also variable count of spaces (also 0)
     *
     * @param text the string to be parsed
     * @return an Geopoint with parsed latitude and longitude
     * @throws Geopoint.ParseException if coordinates could not be parsed
     */
    @NonNull
    public static Geopoint parse(@NonNull final String text) {
        final Set<String> inputs = getParseInputs(text.trim());
        GeopointWrapper best = null;
        for (final AbstractParser parser : parsers) {
            for (final String input : inputs) {
                final GeopointWrapper geopointWrapper = parser.parse(input);
                if (geopointWrapper == null) {
                    continue;
                }
                if (best == null || geopointWrapper.isBetterThan(best)) {
                    best = geopointWrapper;
                }
            }
        }

        if (best != null) {
            return best.getGeopoint();
        }

        throw new Geopoint.ParseException("Cannot parse coordinates: '" + text + "'");
    }

    /**
     * Detects all coordinates in the given text.
     *
     * @param initialText Text to parse for coordinates
     * @return a collection of parsed geopoints as well as their starting and ending position and the appropriate text
     * 'start' points at the first char of the coordinate text, 'end' points at the first char AFTER the coordinate text
     */
    @NonNull
    public static Collection<GeopointWrapper> parseAll(@NonNull final String initialText) {
        final List<GeopointWrapper> waypoints = new LinkedList<>();

        String text = initialText;
        int startIndex = 0;
        GeopointWrapper best;
        do {
            best = null;
            text = text.substring(startIndex);
            final Set<String> inputs = getParseInputs(text);
            for (final AbstractParser parser : parsers) {
                for (final String input : inputs) {
                    final GeopointWrapper geopointWrapper = parser.parse(input);
                    if (geopointWrapper == null) {
                        continue;
                    }
                    if (geopointWrapper.isBetterThan(best)) {
                        best = geopointWrapper;
                        text = input;
                        startIndex = best.getEnd();
                    }
                }
            }

            if (best != null) {
                waypoints.add(best);
            }

        } while (best != null && startIndex < text.length());

        return waypoints;
    }

    /**
     * Parses latitude out of the given string.
     *
     * The parsing fails if the string contains additional characters (except whitespaces).
     *
     * @param text the string to be parsed
     * @return the latitude as decimal degrees
     * @throws Geopoint.ParseException if latitude could not be parsed
     * @see #parse(String)
     */
    public static double parseLatitude(@Nullable final String text) {
        if (text != null) {
            final ResultWrapper wrapper = parseHelper(text, Geopoint.LatLon.LAT);
            if (wrapper != null) {
                return wrapper.result;
            }
        }

        throw new Geopoint.ParseException("Cannot parse latitude", Geopoint.LatLon.LAT);
    }

    /**
     * Parses longitude out of the given string.
     *
     * The parsing fails if the string contains additional characters (except whitespaces).
     *
     * @param text the string to be parsed
     * @return the longitude as decimal degrees
     * @throws Geopoint.ParseException if longitude could not be parsed
     * @see #parse(String)
     */
    public static double parseLongitude(@Nullable final String text) {
        if (text != null) {
            final ResultWrapper wrapper = parseHelper(text, Geopoint.LatLon.LON);
            if (wrapper != null) {
                return wrapper.result;
            }
        }

        throw new Geopoint.ParseException("Cannot parse longitude", Geopoint.LatLon.LON);
    }
}
