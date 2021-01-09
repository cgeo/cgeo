package cgeo.geocaching.calculator;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.MatcherWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Parse coordinates with formulas.
 * For plain-format the keyword (FORMULA-PLAIN) has to be used. Use '|' to separate the variables and the user note.
 * example:
 * @name (x) (FORMULA-PLAIN) N48 AB.(C*D/2) E 9 (C-D).(A+B) |A=a+b|a=5| user note
 */
public final class FormulaParser {

    public  static final char WPC_DELIM = '|';
    public  static final String WPC_DELIM_STRING = "|";
    public  static final String WPC_DELIM_PATTERN_STRING = "\\|";

    private static final Pattern PATTERN_BAD_BLANK_COMMA = Pattern.compile("(\\d), (\\d{2,})");
    private static final Pattern PATTERN_BAD_BLANK_DOT = Pattern.compile("(\\d)\\. (\\d{2,})");

    private static final List<AbstractFormulaParser> parsers = Arrays.asList(new MinDecFormulaParser());

    private Settings.CoordInputFormatEnum desiredFormulaFormat = null;

    public FormulaParser() {
    }

    public FormulaParser(final Settings.CoordInputFormatEnum formulaFormat) {
        desiredFormulaFormat = formulaFormat;
    }

    public static class ParseException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        public final int resource;

        public ParseException(final String msg) {
            super(msg);
            resource = R.string.err_parse_lat_lon;
        }

        public ParseException(final String msg, final Geopoint.LatLon faulty) {
            super(msg);
            resource = faulty == Geopoint.LatLon.LAT ? R.string.err_parse_lat : R.string.err_parse_lon;
        }
    }

    private static class ResultWrapper {
        private final String result;
        private final int matcherLength;

        ResultWrapper(final String result, final int stringLength) {
            this.result = result;
            this.matcherLength = stringLength;
        }
    }

    /**
     * Abstract parser for coordinate formats.
     */
    private abstract static class AbstractFormulaParser {
        /**
         * Parses coordinates (with formula) out of the given string for a specific coordinate format.
         *
         * @param text the string to be parsed
         * @return an pair of strings with parsed formula for latitude and longitude
         */
        @Nullable
        public abstract ImmutablePair<String, String> parse(@NonNull String text);

        /**
         * Parses latitude or longitude out of the given string.
         *
         * @param text
         *            the string to be parsed
         * @param latlon
         *            whether to parse latitude or longitude
         * @return a wrapper with the parsed latitude/longitude and the length of the match, or null if parsing failed
         */
        @Nullable
        public abstract ResultWrapper parse(@NonNull String text, @NonNull Geopoint.LatLon latlon);

        /**
         * Indicates which format is parsed
         * @return coordInputFormat which can be parsed
         */
        public abstract Settings.CoordInputFormatEnum formulaFormat();
    }

    /**
     * Abstract parser for coordinates that consist of two syntactic parts: latitude and longitude.
     */
    private abstract static class AbstractLatLonFormulaParser extends AbstractFormulaParser {
        private final Pattern latPattern;
        private final Pattern lonPattern;
        private final Pattern latLonPattern;

        AbstractLatLonFormulaParser(@NonNull final Pattern latPattern, @NonNull final Pattern lonPattern, @NonNull final Pattern latLonPattern) {
            this.latPattern = latPattern;
            this.lonPattern = lonPattern;
            this.latLonPattern = latLonPattern;
        }

        /**
         * Parses latitude or longitude out of a given range of matched groups.
         *
         * @param matcher
         *            the matcher that holds the matches groups
         * @param first
         *            the first group to parse
         * @param last
         *            the last group to parse
         * @return the parsed latitude/longitude, or null if parsing failed
         */
        @Nullable
        private String parseGroups(@NonNull final MatcherWrapper matcher, final int first, final int last) {
            final List<String> groups = new ArrayList<>(last - first + 1);
            for (int i = first; i <= last; i++) {
                groups.add(matcher.group(i));
            }

            return parse(groups);
        }

        /**
         * @see AbstractFormulaParser#parse(String)
         */
        @Override
        @Nullable
        public final ImmutablePair<String, String> parse(@NonNull final String text) {
            final MatcherWrapper matcher = new MatcherWrapper(latLonPattern, text);
            if (matcher.find()) {
                final int groupCount = matcher.groupCount();
                final int partCount = groupCount / 2;

                final String lat = parseGroups(matcher, 1, partCount);
                if (lat == null || lat.isEmpty()) {
                    return null;
                }

                final String lon = parseGroups(matcher, partCount + 1, groupCount);
                if (lon == null || lon.isEmpty()) {
                    return null;
                }

                return new ImmutablePair<>(lat, lon);
            }

            return null;
        }

        /**
         * @see AbstractFormulaParser#parse(String, Geopoint.LatLon)
         */
        @Override
        @Nullable
        public final ResultWrapper parse(@NonNull final String text, @NonNull final Geopoint.LatLon latlon) {
            final MatcherWrapper matcher = new MatcherWrapper(latlon == Geopoint.LatLon.LAT ? latPattern : lonPattern, text);
            if (matcher.find()) {
                final String res = parseGroups(matcher, 1, matcher.groupCount());
                if (res != null) {
                    return new ResultWrapper(res, matcher.group().length());
                }
            }

            return null;
        }

        /**
         * Parses latitude or longitude from matched groups of corresponding pattern.
         *
         * @param groups
         *            the groups matched by latitude/longitude pattern
         * @return parsed latitude/longitude, or null if parsing failed
         */
        @Nullable
        public abstract String parse(@NonNull List<String> groups);
    }


    /**
     * Parser for MinDec format: X DD° MM.MMM'.
     */
    private static final class MinDecFormulaParser extends AbstractLatLonFormulaParser {

        private static final String COORD_FORMULA_PATTERN_STRING = "[\\[\\]\\(\\){}" + CalculationUtils.VALID_OPERATOR_PATTERN + "A-Za-z\\d]+";
        private static final String STRING_MINDEC = "\\s*(" + COORD_FORMULA_PATTERN_STRING + ")[°\\s]+(" + COORD_FORMULA_PATTERN_STRING + "\\." + COORD_FORMULA_PATTERN_STRING + ")['′\\s]?";

        // private static final String FORMULA_PATTERN_STRING = "\\s*[NS]" + COORD_FORMULA_PATTERN_STRING + "\\s*[EW]" + COORD_FORMULA_PATTERN_STRING ;
        // private static final Pattern FORMULA_PATTERN = Pattern.compile(FORMULA_PATTERN_STRING);



        //                                           (  1  )    (    2    )    (      3      )
        private static final String STRING_LAT = "([NS]?)" + STRING_MINDEC;

        //                                        (   1  )    (    2    )    (      3      )
        private static final String STRING_LON = "([WEO]?)" + STRING_MINDEC;
        private static final String STRING_SEPARATOR = "[^\\w'′\"″°." + WPC_DELIM + "]*";
        private static final Pattern PATTERN_LAT = Pattern.compile(STRING_LAT, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LON = Pattern.compile("\\b" + STRING_LON, Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_LATLON = Pattern.compile(STRING_LAT + STRING_SEPARATOR + STRING_LON, Pattern.CASE_INSENSITIVE);

        MinDecFormulaParser() {
            super(PATTERN_LAT, PATTERN_LON, PATTERN_LATLON);
        }

        /**
         * @see AbstractLatLonFormulaParser#parse(List)
         */
        @Override
        @Nullable
        public String parse(@NonNull final List<String> groups) {
            final String group1 = groups.get(0).trim();
            final String group2 = groups.get(1).trim();
            final String group3 = groups.get(2).trim();

            final String strippedGroup2 = StringUtils.stripEnd(group2, "°").trim();

            final String result = group1 + " " + strippedGroup2 + "° " + group3 + "'";
            return result;
        }

        public Settings.CoordInputFormatEnum formulaFormat() {
            return Settings.CoordInputFormatEnum.Plain;
        }
    }

    /**
     * Returns a set of parser inputs for a given text
     *
     * The generated inputs use different delimiters for fractional numbers.
     *
     * @param text
     *            the text to parse
     * @return the set of parser inputs
     */
    @NonNull
    private static Set<String> getParseInputs(@NonNull final String text) {
        final String inputDot = removeSpaceAfterSeparators(text);
        return CollectionStream.of(new String[]{inputDot}).toSet();
    }

    /**
     * Removes all single spaces after a comma (see #2404)
     *
     * @param text
     *            the string to substitute
     * @return the substituted string without the single spaces
     */
    @NonNull
    private static String removeSpaceAfterSeparators(@NonNull final String text) {
        final String replacedComma = new MatcherWrapper(PATTERN_BAD_BLANK_COMMA, text).replaceAll("$1,$2");
        return new MatcherWrapper(PATTERN_BAD_BLANK_DOT, replacedComma).replaceAll("$1.$2");
    }

    /**
     * Parses latitude/longitude from the given string.
     *
     * @param text
     *            the text to parse
     * @param latlon
     *            whether to parse latitude or longitude
     * @return a wrapper with the best latitude/longitude and the length of the match, or null if parsing failed
     */
    @Nullable
    private ResultWrapper parseHelper(@NonNull final String text, @NonNull final Geopoint.LatLon latlon) {
        final String input = removeSpaceAfterSeparators(text.trim());
        for (final AbstractFormulaParser parser : parsers) {
            if (isValidParser(parser)) {
                final ResultWrapper wrapper = parser.parse(input, latlon);
                if (wrapper != null && wrapper.matcherLength == input.length()) {
                    return wrapper;
                }
            }
        }

        return null;
    }

    /**
     * Parses a pair of coordinates (latitude and longitude) with formula out of the given string.
     *
     * Accepts following formats with formula:
     * - X DD° MM.MMM
     *
     * variable count of spaces (also 0) are excepted
     *
     * @param text the string to be parsed
     * @return an pair of strings with parsed formula for latitude and longitude
     * @throws FormulaParser.ParseException
     *             if coordinates could not be parsed
     */
    @NonNull
    public ImmutablePair<String, String> parse(@NonNull final String text) {
        final Set<String> inputs = getParseInputs(text.trim());
        ImmutablePair<String, String> foundPair = null;
        for (final AbstractFormulaParser parser : parsers) {
            if (isValidParser(parser)) {
                for (final String input : inputs) {
                    final ImmutablePair<String, String> coordinatePair = parser.parse(input);
                    if (coordinatePair == null) {
                        continue;
                    }
                    foundPair = coordinatePair;
                }
            }
        }

        if (foundPair != null) {
            return foundPair;
        }

        throw new FormulaParser.ParseException("Cannot parse coordinates with formula");
    }

    /**
     * Parses latitude out of the given string.
     *
     * The parsing fails if the string contains additional characters (except whitespaces).
     *
     * @see #parse(String)
     * @param text
     *            the string to be parsed
     * @return the latitude as decimal degrees
     * @throws FormulaParser.ParseException
     *             if latitude could not be parsed
     */
    public String parseLatitude(@Nullable final String text) {
        if (text != null) {
            final ResultWrapper wrapper = parseHelper(text, Geopoint.LatLon.LAT);
            if (wrapper != null) {
                return wrapper.result;
            }
        }

        throw new FormulaParser.ParseException("Cannot parse latitude", Geopoint.LatLon.LAT);
    }

    /**
     * Parses longitude out of the given string.
     *
     * The parsing fails if the string contains additional characters (except whitespaces).
     *
     * @see #parse(String)
     * @param text
     *            the string to be parsed
     * @return the longitude as decimal degrees
     * @throws FormulaParser.ParseException
     *             if longitude could not be parsed
     */
    public String parseLongitude(@Nullable final String text) {
        if (text != null) {
            final ResultWrapper wrapper = parseHelper(text, Geopoint.LatLon.LON);
            if (wrapper != null) {
                return wrapper.result;
            }
        }

        throw new FormulaParser.ParseException("Cannot parse longitude", Geopoint.LatLon.LON);
    }

    private Boolean isValidParser(final AbstractFormulaParser parser) {
        if (null != desiredFormulaFormat) {
            return desiredFormulaFormat == parser.formulaFormat();
        }
        return true;
    }
}
