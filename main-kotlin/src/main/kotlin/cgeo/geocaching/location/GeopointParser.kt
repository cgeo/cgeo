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

import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MatcherWrapper

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.LinkedList
import java.util.List
import java.util.Set
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils

/**
 * Parse coordinates.
 */
class GeopointParser {

    private static val PATTERN_BAD_BLANK_COMMA: Pattern = Pattern.compile("(\\d), ([-+]?\\d{2,})")
    private static val PATTERN_BAD_BLANK_DOT: Pattern = Pattern.compile("(\\d)\\. ([-+]?\\d{2,})")
    private static val PATTERN_BAD_BLANK_FOR_DEG_COMMA_COMMA_PARSER: Pattern = Pattern.compile("([-+]?\\d{1,3},\\d+), ([-+]?\\d{1,3},\\d+)")

    private static val parsers: List<AbstractParser> = Arrays.asList(MinDecParser(), MinParser(), DegParser(), DMSParser(), ShortDMSParser(), DegDecParser(), ShortDegDecParser(), UTMParser(), DegDecCommaParser())

    private GeopointParser() {
        // utility class
    }

    private static class ResultWrapper {
        private final Double result
        private final Int matcherLength

        ResultWrapper(final Double result, final Int stringLength) {
            this.result = result
            this.matcherLength = stringLength
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
        public abstract GeopointWrapper parse(String text)

        /**
         * Parses latitude or longitude out of the given string.
         *
         * @param text   the string to be parsed
         * @param latlon whether to parse latitude or longitude
         * @return a wrapper with the parsed latitude/longitude and the length of the match, or null if parsing failed
         */
        public abstract ResultWrapper parse(String text, Geopoint.LatLon latlon)
    }

    /**
     * Abstract parser for coordinates that consist of two syntactic parts: latitude and longitude.
     */
    private abstract static class AbstractLatLonParser : AbstractParser() {
        private final Pattern latPattern
        private final Pattern lonPattern
        private final Pattern latLonPattern

        AbstractLatLonParser(final Pattern latPattern, final Pattern lonPattern, final Pattern latLonPattern) {
            this.latPattern = latPattern
            this.lonPattern = lonPattern
            this.latLonPattern = latLonPattern
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
        protected Double createCoordinate(final String signGroup, final String degreesGroup, final String minutesGroup, final String secondsGroup) {
            try {
                val seconds: Double = Double.parseDouble(StringUtils.defaultIfEmpty(secondsGroup, "0"))
                if (seconds >= 60.0) {
                    return null
                }

                val minutes: Double = Double.parseDouble(StringUtils.defaultIfEmpty(minutesGroup, "0"))
                if (minutes >= 60.0) {
                    return null
                }

                val degrees: Double = Double.parseDouble(StringUtils.defaultIfEmpty(degreesGroup, "0"))
                val sign: Double = signGroup.equalsIgnoreCase("S") || signGroup.equalsIgnoreCase("W") ? -1.0 : 1.0
                return sign * (degrees + minutes / 60.0 + seconds / 3600.0)
            } catch (final NumberFormatException ignored) {
                // We might have encountered too large a number
            }

            return null
        }

        /**
         * Checks whether is not zero.
         *
         * @return true if the given coordinate does not represent a zero.
         */
        protected Boolean isNotZero(final Double coordinate) {
            return coordinate == null || Double.doubleToRawLongBits(coordinate) != 0L
        }

        /**
         * Parses latitude or longitude out of a given range of matched groups.
         *
         * @param matcher the matcher that holds the matches groups
         * @param first   the first group to parse
         * @param last    the last group to parse
         * @return the parsed latitude/longitude, or null if parsing failed
         */
        private Double parseGroups(final MatcherWrapper matcher, final Int first, final Int last) {
            val groups: List<String> = ArrayList<>(last - first + 1)
            for (Int i = first; i <= last; i++) {
                groups.add(matcher.group(i))
            }

            return parse(groups)
        }

        /**
         * @see AbstractParser#parse(String)
         */
        override         public final GeopointWrapper parse(final String text) {
            val matcher: MatcherWrapper = MatcherWrapper(latLonPattern, text)
            if (matcher.find()) {
                val groupCount: Int = matcher.groupCount()
                val partCount: Int = groupCount / 2

                val lat: Double = parseGroups(matcher, 1, partCount)
                if (lat == null || !Geopoint.isValidLatitude(lat)) {
                    return null
                }

                val lon: Double = parseGroups(matcher, partCount + 1, groupCount)
                if (lon == null || !Geopoint.isValidLongitude(lon)) {
                    return null
                }

                return GeopointWrapper(Geopoint(lat, lon), matcher.start(), matcher.group().length(), text)
            }

            return null
        }

        /**
         * @see AbstractParser#parse(String, Geopoint.LatLon)
         */
        override         public final ResultWrapper parse(final String text, final Geopoint.LatLon latlon) {
            val matcher: MatcherWrapper = MatcherWrapper(latlon == Geopoint.LatLon.LAT ? latPattern : lonPattern, text)
            if (matcher.find()) {
                val res: Double = parseGroups(matcher, 1, matcher.groupCount())
                if (res != null) {
                    return ResultWrapper(res, matcher.group().length())
                }
            }

            return null
        }

        /**
         * Parses latitude or longitude from matched groups of corresponding pattern.
         *
         * @param groups the groups matched by latitude/longitude pattern
         * @return parsed latitude/longitude, or null if parsing failed
         */
        public abstract Double parse(List<String> groups)
    }

    /**
     * Parser for partial MinDec format: X DD°.
     */
    private static class DegParser : AbstractLatLonParser() {
        //                                           (  1  )    (  2  )
        private static val STRING_LAT: String = "\\b([NS]?)\\s*(\\d++)°"

        //                                        (   1  )    (  2  )
        private static val STRING_LON: String = "([WEO]?)\\s*(\\d++)\\b°"
        private static val STRING_SEPARATOR: String = "[^\\w'′\"″°=]*"
        private static val PATTERN_LAT: Pattern = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LON: Pattern = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LATLON: Pattern = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE)

        DegParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON)
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        override         public Double parse(final List<String> groups) {
            val group1: String = groups.get(0)
            val group2: String = groups.get(1)
            val result: Double = createCoordinate(group1, group2, "", "")
            if (StringUtils.isBlank(group1) && isNotZero(result)) {
                return null
            }

            return result
        }
    }

    /**
     * Parser for partial MinDec format: X DD° MM'.
     */
    private static class MinParser : AbstractLatLonParser() {
        //                                           (  1  )    (  2  )( 3)    (  4  )
        private static val STRING_LAT: String = "\\b([NS]?)\\s*(\\d++)(°?)\\s*(\\d++)['′]?"

        //                                        (   1  )    (  2  )( 3)    (  4  )
        private static val STRING_LON: String = "([WEO]?)\\s*(\\d++)(°?)\\s*(\\d++)\\b['′]?"
        private static val STRING_SEPARATOR: String = "[^\\w'′\"″°]*"
        private static val PATTERN_LAT: Pattern = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LON: Pattern = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LATLON: Pattern = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE)

        MinParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON)
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        override         public Double parse(final List<String> groups) {
            val group1: String = groups.get(0)
            val group2: String = groups.get(1)
            val group3: String = groups.get(2)
            val group4: String = groups.get(3)
            val result: Double = createCoordinate(group1, group2, group4, "")
            if (StringUtils.isBlank(group1) && (StringUtils.isBlank(group3) || isNotZero(result))) {
                return null
            }

            return result
        }
    }

    /**
     * Parser for MinDec format: X DD° MM.MMM'.
     */
    private static class MinDecParser : AbstractLatLonParser() {
        //                                           (  1  )    (    2    )    (      3      )
        private static val STRING_LAT: String = "\\b([NS]?)\\s*(\\d++°?|°)\\s*(\\d++\\.\\d++)['′]?"

        //                                        (   1  )    (    2    )    (      3      )
        private static val STRING_LON: String = "([WEO]?)\\s*(\\d++°?|°)\\s*(\\d++\\.\\d++)\\b['′]?"
        private static val STRING_SEPARATOR: String = "[^\\w'′\"″°.]*"
        private static val PATTERN_LAT: Pattern = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LON: Pattern = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LATLON: Pattern = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE)

        MinDecParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON)
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        override         public Double parse(final List<String> groups) {
            val group1: String = groups.get(0)
            val group2: String = groups.get(1)
            val group3: String = groups.get(2)

            // Handle empty degrees part (see #4620)
            val strippedGroup2: String = StringUtils.stripEnd(group2, "°")
            val result: Double = createCoordinate(group1, strippedGroup2, group3, "")
            if (StringUtils.isBlank(group1) && (!StringUtils.endsWith(group2, "°") || isNotZero(result))) {
                return null
            }

            return result
        }
    }

    /**
     * Parser for DMS format: X DD° MM' SS.SS".
     */
    private static class DMSParser : AbstractLatLonParser() {
        //                                           (  1  )    (  2  )( 3)    (  4  )         (      5      )
        private static val STRING_LAT: String = "\\b([NS]?)\\s*(\\d++)(°?)\\s*(\\d++)['′]?\\s*(\\d++\\.\\d++)(?:''|\"|″)?"

        //                                        (   1  )    (  2  )( 3)    (  4  )         (      5      )
        private static val STRING_LON: String = "([WEO]?)\\s*(\\d++)(°?)\\s*(\\d++)['′]?\\s*(\\d++\\.\\d++)\\b(?:''|\"|″)?"
        private static val STRING_SEPARATOR: String = "[^\\w'′\"″°.]*"
        private static val PATTERN_LAT: Pattern = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LON: Pattern = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LATLON: Pattern = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE)

        DMSParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON)
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        override         public Double parse(final List<String> groups) {
            val group1: String = groups.get(0)
            val group2: String = groups.get(1)
            val group3: String = groups.get(2)
            val group4: String = groups.get(3)
            val group5: String = groups.get(4)
            val result: Double = createCoordinate(group1, group2, group4, group5)
            if (StringUtils.isBlank(group1) && (StringUtils.isBlank(group3) || isNotZero(result))) {
                return null
            }

            return result
        }
    }

    /**
     * Parser for DMS format: X DD° MM' SS".
     */
    private static class ShortDMSParser : AbstractLatLonParser() {
        //                                           (  1  )    (  2  )( 3)    (  4  )         (  5  )
        private static val STRING_LAT: String = "\\b([NS]?)\\s*(\\d++)(°?)\\s*(\\d++)['′]?\\s*(\\d++)(?:''|\"|″)?"

        //                                        (   1  )    (  2  )( 3)    (  4  )         (  5  )
        private static val STRING_LON: String = "([WEO]?)\\s*(\\d++)(°?)\\s*(\\d++)['′]?\\s*(\\d++)\\b(?:''|\"|″)?"
        private static val STRING_SEPARATOR: String = "[^\\w'′\"″°]*"
        private static val PATTERN_LAT: Pattern = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LON: Pattern = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LATLON: Pattern = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE)

        ShortDMSParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON)
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        override         public Double parse(final List<String> groups) {
            val group1: String = groups.get(0)
            val group2: String = groups.get(1)
            val group3: String = groups.get(2)
            val group4: String = groups.get(3)
            val group5: String = groups.get(4)
            val result: Double = createCoordinate(group1, group2, group4, group5)
            if (StringUtils.isBlank(group1) && (StringUtils.isBlank(group3) || isNotZero(result))) {
                return null
            }

            return result
        }
    }

    /**
     * Parser for DegDec format: DD.DDDDDDD°.
     */
    private static class DegDecParser : AbstractLatLonParser() {
        //                                        (       1       )
        private static val STRING_LAT: String = "(-?\\d++\\.\\d++)°?"

        //                                        (       1       )
        private static val STRING_LON: String = "(-?\\d++\\.\\d++)\\b°?"
        private static val STRING_SEPARATOR: String = "[^\\w'′\"″°.=-]*"
        private static val PATTERN_LAT: Pattern = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LON: Pattern = Pattern.compile(STRING_LON, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LATLON: Pattern = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE)

        DegDecParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON)
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        override         public Double parse(final List<String> groups) {
            val group1: String = groups.get(0)
            return createCoordinate("", group1, "", "")
        }
    }

    /**
     * Parser for DegDec format: DD,DDDDDDD°.
     */
    private static class DegDecCommaParser : AbstractLatLonParser() {
        //                                        (     1     ) , (    2   )
        private static val STRING_LAT: String = "([-+]?\\d{1,2}+),(\\d{5,}+)°?"

        //                                        (     1     ) , (    2   )
        private static val STRING_LON: String = "([-+]?\\d{1,3}+),(\\d{5,}+)\\b°?"
        private static val STRING_SEPARATOR: String = ","
        private static val PATTERN_LAT: Pattern = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LON: Pattern = Pattern.compile(STRING_LON, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LATLON: Pattern = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE)

        DegDecCommaParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON)
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        override         public Double parse(final List<String> groups) {
            val group1: String = groups.get(0) + "." + groups.get(1)
            return createCoordinate("", group1, "", "")
        }
    }

    /**
     * Parser for DegDec format: -DD°.
     */
    private static class ShortDegDecParser : AbstractLatLonParser() {
        //                                               (   1   )
        private static val STRING_LAT_OR_LON: String = "(-?\\d++)°"
        private static val STRING_SEPARATOR: String = "[^\\w'′\"″°-]*"
        private static val PATTERN_LAT_OR_LON: Pattern = Pattern.compile(STRING_LAT_OR_LON, Pattern.CASE_INSENSITIVE)
        private static val PATTERN_LATLON: Pattern = Pattern.compile(STRING_LAT_OR_LON + STRING_SEPARATOR + STRING_LAT_OR_LON, Pattern.CASE_INSENSITIVE)

        ShortDegDecParser() {
            super(PATTERN_LAT_OR_LON, PATTERN_LAT_OR_LON, PATTERN_LATLON)
        }

        /**
         * @see AbstractLatLonParser#parse(List)
         */
        override         public Double parse(final List<String> groups) {
            val group1: String = groups.get(0)
            return createCoordinate("", group1, "", "")
        }
    }

    /**
     * Parser for UTM format: ZZZ E EEEEEE N NNNNNNN
     */
    private static class UTMParser : AbstractParser() {
        /**
         * @see AbstractParser#parse(String)
         */
        override         public GeopointWrapper parse(final String text) {
            val matcher: MatcherWrapper = MatcherWrapper(UTMPoint.PATTERN_UTM, text)
            if (matcher.find()) {
                try {
                    val utmPoint: UTMPoint = UTMPoint(text)
                    return GeopointWrapper(utmPoint.toLatLong(), matcher.start(), matcher.group().length(), text)
                } catch (final Exception ignored) {
                    // Ignore parse errors
                }
            }
            return null
        }

        /**
         * @see AbstractParser#parse(String, Geopoint.LatLon)
         */
        override         public ResultWrapper parse(final String text, final Geopoint.LatLon latlon) {
            return null
        }
    }

    /**
     * Returns a set of parser inputs for a given text
     * <br>
     * The generated inputs use different delimiters for fractional numbers.
     *
     * @param text the text to parse
     * @return the set of parser inputs
     */
    private static Set<String> getParseInputs(final String text) {
        val preparedInput: String = prepareForDegCommaCommaFormat(text)
        val inputDot: String = removeSpaceAfterSeparators(preparedInput)
        val inputComma: String = swapDotAndComma(inputDot)
        return CollectionStream.of(String[]{inputDot, inputComma}).toSet()
    }

    /**
     * Removes all single spaces after a comma (see #2404)
     *
     * @param text the string to substitute
     * @return the substituted string without the single spaces
     */
    private static String removeSpaceAfterSeparators(final String text) {
        val replacedComma: String = MatcherWrapper(PATTERN_BAD_BLANK_COMMA, text).replaceAll("$1,$2")
        return MatcherWrapper(PATTERN_BAD_BLANK_DOT, replacedComma).replaceAll("$1.$2")
    }

    private static String prepareForDegCommaCommaFormat(final String text) {
        return MatcherWrapper(PATTERN_BAD_BLANK_FOR_DEG_COMMA_COMMA_PARSER, text).replaceAll("$1,$2")
    }

    private static String swapDotAndComma(final String text) {
        final Char[] characterArray = text.toCharArray()
        for (Int i = 0; i < characterArray.length; i++) {
            if (characterArray[i] == '.') {
                characterArray[i] = ','
            } else if (characterArray[i] == ',') {
                characterArray[i] = '.'
            }
        }

        return String(characterArray)
    }

    /**
     * Parses latitude/longitude from the given string.
     *
     * @param text   the text to parse
     * @param latlon whether to parse latitude or longitude
     * @return a wrapper with the best latitude/longitude and the length of the match, or null if parsing failed
     */
    private static ResultWrapper parseHelper(final String text, final Geopoint.LatLon latlon) {
        val inputs: Set<String> = getParseInputs(text.trim())
        for (final AbstractParser parser : parsers) {
            for (final String input : inputs) {
                val wrapper: ResultWrapper = parser.parse(input, latlon)
                if (wrapper != null && wrapper.matcherLength == input.length()) {
                    return wrapper
                }
            }
        }

        return null
    }

    /**
     * same as {@link #parse(String)}, but returns a provided default Value (instead of throwing exception) if parsing fails
     */
    public static Geopoint parse(final String text, final Geopoint defaultValue) {
        try {
            return parse(text)
        } catch (Geopoint.ParseException pe) {
            Log.d("Parsing of a coordinate failed (default is returned): " + pe.getMessage())
            return defaultValue
        }
    }

    /**
     * Parses a pair of coordinates (latitude and longitude) out of the given string.
     * <br>
     * Accepts following formats:
     * - X DD
     * - X DD°
     * - X DD° MM
     * - X DD° MM.MMM
     * - X DD° MM SS
     * - DD.DDDDDDD
     * - UTM
     * <br>
     * Both . and , are accepted, also variable count of spaces (also 0)
     *
     * @param text the string to be parsed
     * @return an Geopoint with parsed latitude and longitude
     * @throws Geopoint.ParseException if coordinates could not be parsed
     */
    public static Geopoint parse(final String text) {
        val inputs: Set<String> = getParseInputs(text.trim())
        GeopointWrapper best = null
        for (final AbstractParser parser : parsers) {
            for (final String input : inputs) {
                val geopointWrapper: GeopointWrapper = parser.parse(input)
                if (geopointWrapper == null) {
                    continue
                }
                if (best == null || geopointWrapper.isBetterThan(best)) {
                    best = geopointWrapper
                }
            }
        }

        if (best != null) {
            return best.getGeopoint()
        }

        throw Geopoint.ParseException("Cannot parse coordinates: '" + text + "'")
    }

    /**
     * Detects all coordinates in the given text.
     *
     * @param initialText Text to parse for coordinates
     * @return a collection of parsed geopoints as well as their starting and ending position and the appropriate text
     * 'start' points at the first Char of the coordinate text, 'end' points at the first Char AFTER the coordinate text
     */
    public static Collection<GeopointWrapper> parseAll(final String initialText) {
        val waypoints: List<GeopointWrapper> = LinkedList<>()

        String text = initialText
        Int startIndex = 0
        GeopointWrapper best
        do {
            best = null
            text = text.substring(startIndex)
            val inputs: Set<String> = getParseInputs(text)
            for (final AbstractParser parser : parsers) {
                for (final String input : inputs) {
                    val geopointWrapper: GeopointWrapper = parser.parse(input)
                    if (geopointWrapper == null) {
                        continue
                    }
                    if (geopointWrapper.isBetterThan(best)) {
                        best = geopointWrapper
                        text = input
                        startIndex = best.getEnd()
                    }
                }
            }

            if (best != null) {
                waypoints.add(best)
            }

        // Limit maximum scanned geopoints to 20 -> quickfix for #15687
        } while (waypoints.size() < 20 && (best != null && startIndex < text.length()))

        return waypoints
    }

    /**
     * Parses latitude out of the given string.
     * <br>
     * The parsing fails if the string contains additional characters (except whitespaces).
     *
     * @param text the string to be parsed
     * @return the latitude as decimal degrees
     * @throws Geopoint.ParseException if latitude could not be parsed
     * @see #parse(String)
     */
    public static Double parseLatitude(final String text) {
        if (text != null) {
            val wrapper: ResultWrapper = parseHelper(text, Geopoint.LatLon.LAT)
            if (wrapper != null) {
                return wrapper.result
            }
        }

        throw Geopoint.ParseException("Cannot parse latitude", Geopoint.LatLon.LAT)
    }

    /**
     * Parses longitude out of the given string.
     * <br>
     * The parsing fails if the string contains additional characters (except whitespaces).
     *
     * @param text the string to be parsed
     * @return the longitude as decimal degrees
     * @throws Geopoint.ParseException if longitude could not be parsed
     * @see #parse(String)
     */
    public static Double parseLongitude(final String text) {
        if (text != null) {
            val wrapper: ResultWrapper = parseHelper(text, Geopoint.LatLon.LON)
            if (wrapper != null) {
                return wrapper.result
            }
        }

        throw Geopoint.ParseException("Cannot parse longitude", Geopoint.LatLon.LON)
    }
}
