package cgeo.geocaching.models;

import cgeo.geocaching.calculator.CalcStateEvaluator;
import cgeo.geocaching.calculator.CoordinatesCalculateUtils;
import cgeo.geocaching.calculator.FormulaParser;
import cgeo.geocaching.calculator.FormulaWrapper;
import cgeo.geocaching.calculator.VariableData;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.GeopointParser;
import cgeo.geocaching.location.GeopointWrapper;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.formulas.VariableList;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.jetbrains.annotations.NotNull;

public class WaypointParser {


    //constants for general note parsing
    private static final String BACKUP_TAG_OPEN = "{c:geo-start}";
    private static final String BACKUP_TAG_CLOSE = "{c:geo-end}";

    //Constants for waypoint parsing
    public static final String PARSING_COORD_FORMULA_PLAIN = "(F-PLAIN)";
    public static final String PARSING_CALCULATED_COORD = "{" + CalculatedCoordinate.CONFIG_KEY + "|";
    private static final String PARSING_NAME_PRAEFIX = "@";
    private static final char PARSING_USERNOTE_DELIM = '"';
    private static final char PARSING_USERNOTE_ESCAPE = '\\';
    private static final String PARSING_USERNOTE_CONTINUED = "...";
    private static final String PARSING_PREFIX_OPEN = "[";
    private static final String PARSING_PREFIX_CLOSE = "]";
    private static final String PARSING_TYPE_OPEN = "(";
    private static final String PARSING_TYPE_CLOSE = ")";
    private static final String PARSING_COORD_EMPTY = "(NO-COORD)";

    //Constants for variable parsing

    private static final String PARSING_VAR_LETTERS_FULL = "\\$([a-zA-Z][a-zA-Z0-9]*)\\s*=([^\\n|]*)[\\n|]";
    private static final String PARSING_VAR_LETTERS_NUMERIC = "[^A-Za-z0-9]([A-Za-z]+)\\s*=\\s*([0-9]+(?:[,.][0-9]+)?)[^0-9]";
    private static final Pattern PARSING_VARS = Pattern.compile(PARSING_VAR_LETTERS_FULL + "|" + PARSING_VAR_LETTERS_NUMERIC);

    //general members
    private final Geocache cache;
    private final boolean legacyMode;

    //Waypoints
    private Collection<Waypoint> waypoints;
    private final String namePrefix;
    private int count;

    //Variables
    private Map<String, String> variables;

    /**
     * Detect coordinates in the given text and converts them to user-defined waypoints.
     * Works by rule of thumb.
     *
     * @param namePrefix Prefix of the name of the waypoint
     */
    public WaypointParser(final Geocache cache, @NonNull final String namePrefix) {
        this(cache, namePrefix, false);
    }

    public WaypointParser(final Geocache cache, @NonNull final String namePrefix, final boolean legacyMode) {
        this.legacyMode = legacyMode;
        this.namePrefix = namePrefix;
        this.cache = cache;
    }

    /**
     * Detect coordinates in the given text and converts them to user-defined waypoints.
     * Works by rule of thumb.
     *
     * @param text Text to parse for waypoints
     * @return a collection of found waypoints
     */
    public Collection<Waypoint> parseWaypoints(@NonNull final String text) {
        this.count = 1;
        if (null == waypoints) {
            waypoints = new LinkedList<>();
        } else {
            waypoints.clear();
        }
        if (null == variables) {
            variables = new HashMap<>();
        } else {
            variables.clear();
        }

        //if a backup is found, we parse it first
        for (final String backup : TextUtils.getAll(text, BACKUP_TAG_OPEN, BACKUP_TAG_CLOSE)) {
            parseWaypointsFromString(backup);
            parseVariablesFromString(backup);
        }
        final String remainder = TextUtils.replaceAll(text, BACKUP_TAG_OPEN, BACKUP_TAG_CLOSE, "");
        parseWaypointsFromString(remainder);
        parseVariablesFromString(remainder);

        return waypoints;
    }

    public Map<String, String> getParsedVariables() {
        return variables;
    }

    private void parseWaypointsFromString(final String text) {
        // search waypoints with coordinates
        parseWaypointsWithCoords(text);

        // search waypoints with empty coordinates
        parseWaypointsWithSpecificCoords(text, PARSING_COORD_EMPTY, null);

        // search waypoints with formula
        parseWaypointsWithSpecificCoords(text, PARSING_COORD_FORMULA_PLAIN, Settings.CoordInputFormatEnum.Plain);

        //search calculated waypoints
        parseWaypointsWithSpecificCoords(text, PARSING_CALCULATED_COORD, null);

    }

    private void parseWaypointsWithCoords(final String text) {
        final String cleanedText = removeCalculatedCoords(text);
        final Collection<GeopointWrapper> matches = GeopointParser.parseAll(cleanedText);
        for (final GeopointWrapper match : matches) {
            final Waypoint wp = parseSingleWaypoint(match, null);
            if (wp != null) {
                waypoints.add(wp);
                count++;
            }
        }
    }

    private String removeCalculatedCoords(final String text) {
        return text.replaceAll(Pattern.quote(PARSING_CALCULATED_COORD) + ".*?" + Pattern.quote("}"), "");
    }

    private void parseWaypointsWithSpecificCoords(final String text, final String parsingCoord, final Settings.CoordInputFormatEnum coordFormat) {
        int idxWaypoint = text.indexOf(parsingCoord);

        while (idxWaypoint >= 0) {
            final GeopointWrapper match = new GeopointWrapper(null, idxWaypoint, parsingCoord.length(), text);
            final Waypoint wp = parseSingleWaypoint(match, coordFormat);
            if (wp != null) {
                waypoints.add(wp);
                count++;
            }
            idxWaypoint = text.indexOf(parsingCoord, idxWaypoint + parsingCoord.length());
        }
    }

    private Waypoint parseSingleWaypoint(final GeopointWrapper match, final Settings.CoordInputFormatEnum coordFormat) {
        final Geopoint point = match.getGeopoint();
        final Integer start = match.getStart();
        final Integer end = match.getEnd();
        final String text = match.getText();
        final String matchedText = text.substring(start, end);

        final String[] wordsBefore = TextUtils.getWords(TextUtils.getTextBeforeIndexUntil(text, start, "\n"));
        final String lastWordBefore = wordsBefore.length == 0 ? "" : wordsBefore[wordsBefore.length - 1];

        //try to get a waypointType
        final WaypointType wpType = parseWaypointType(text.substring(Math.max(0, start - 20), start), lastWordBefore);

        //try to get a name and a prefix
        final ImmutablePair<String, String> parsedNameAndPrefix = parseNameAndPrefix(wordsBefore, wpType);
        String name = parsedNameAndPrefix.getLeft();
        final String prefix = parsedNameAndPrefix.getRight();
        if (StringUtils.isBlank(name)) {
            name = namePrefix + " " + count;
        }

        //create the waypoint
        final Waypoint waypoint = new Waypoint(name, wpType, true);
        waypoint.setCoords(point);
        waypoint.setPrefix(prefix);

        String afterCoords = TextUtils.getTextAfterIndexUntil(text, end - 1, null);

        if (null != coordFormat) {
            // try to get a formula
            final ImmutableTriple<CalculatedCoordinate, String, CalcState> coordFormula = parseFormula(afterCoords, coordFormat);
            if (this.legacyMode) {
                final CalcState calcState = coordFormula.right;
                if (null != calcState) {
                    waypoint.setCalcStateConfig(calcState.toJSON().toString());
                    // try to evaluate valid coordinates
                    final CalcStateEvaluator eval = new CalcStateEvaluator(calcState.equations, calcState.freeVariables, null);
                    final Geopoint gp = eval.evaluate(calcState.plainLat, calcState.plainLon);
                    waypoint.setCoords(gp);
                }
            } else {
                final CalculatedCoordinate cc = coordFormula.left;
                if (null != cc) {
                    waypoint.setCalcStateConfig(cc.toConfig());
                    // try to evaluate valid coordinates
                    if (this.cache != null && this.cache.getVariables() != null) {
                        waypoint.setCoords(cc.calculateGeopoint(this.cache.getVariables()::getValue));
                    }
                }
            }

            afterCoords = coordFormula.middle;
        }

        if (PARSING_CALCULATED_COORD.equals(matchedText)) {
            final String configAndMore = text.substring(start);
            final CalculatedCoordinate cc = new CalculatedCoordinate();
            final int parseEnd = cc.setFromConfig(configAndMore);
            if (parseEnd < 0 || parseEnd > configAndMore.length() ||
                    configAndMore.charAt(parseEnd - 1) != '}' || !cc.isFilled()) {
                return null;
            }
            waypoint.setCalcStateConfig(cc.toConfig());
            // try to evaluate valid coordinates
            if (this.cache != null && this.cache.getVariables() != null) {
                waypoint.setCoords(cc.calculateGeopoint(this.cache.getVariables()::getValue));
            }

            afterCoords = configAndMore.substring(parseEnd);
        }

        //try to get a user note
        final String userNote = parseUserNote(afterCoords, 0);
        if (!StringUtils.isBlank(userNote)) {
            waypoint.setUserNote(userNote.trim());
        }

        return waypoint;
    }

    private String parseUserNote(final String text, final int end) {
        final String after = TextUtils.getTextAfterIndexUntil(text, end - 1, null).trim();
        if (after.startsWith("" + PARSING_USERNOTE_DELIM)) {
            return TextUtils.parseNextDelimitedValue(after, PARSING_USERNOTE_DELIM, PARSING_USERNOTE_ESCAPE);
        }
        return TextUtils.getTextAfterIndexUntil(text, end - 1, "\n");
    }

    /**
     * try to parse a name out of given words. If not possible, empty is returned
     */
    @NotNull
    private ImmutablePair<String, String> parseNameAndPrefix(final String[] words, final WaypointType wpType) {
        if (words.length == 0 || !words[0].startsWith(PARSING_NAME_PRAEFIX)) {
            return new ImmutablePair<>("", "");
        }
        //first word handling
        String name = words[0].substring(PARSING_NAME_PRAEFIX.length());
        String prefix = "";
        final int idx = name.indexOf(PARSING_PREFIX_CLOSE);
        if (idx > 0 && name.startsWith(PARSING_PREFIX_OPEN)) {
            prefix = name.substring(PARSING_PREFIX_OPEN.length(), idx).trim();
            name = name.substring(idx + 1);
        }

        //handle additional words if any
        for (int i = 1; i < words.length; i++) {
            if (useWordForParsedName(words[i], i == words.length - 1, wpType)) {
                if (name.length() > 0) {
                    name += " ";
                }
                name += words[i];
            }
        }
        return new ImmutablePair<>(StringUtils.isBlank(name) ? "" : name.trim(), prefix);
    }

    private boolean useWordForParsedName(final String word, final boolean isLast, final WaypointType wpType) {
        return
                (!StringUtils.isBlank(word)) &&
                        //remove words which are in parenthesis (is usually the waypoint type)
                        !(word.startsWith(PARSING_TYPE_OPEN) && word.endsWith(PARSING_TYPE_CLOSE)) &&
                        //remove last word if it is just the waypoint type id
                        !(isLast && word.toLowerCase(Locale.getDefault()).equals(wpType.getShortId().toLowerCase(Locale.getDefault())));
    }

    /**
     * Detect waypoint types in the personal note text. Tries to find various ways that
     * the waypoints name or id is written in given text.
     */
    // method readability will not improve by splitting it up or using lambda-expressions
    @SuppressWarnings("PMD.NPathComplexity")
    private WaypointType parseWaypointType(final String input, final String lastWord) {
        final String lowerInput = input.toLowerCase(Locale.getDefault());
        final String lowerLastWord = lastWord.toLowerCase(Locale.getDefault());

        //find the LAST (if any) enclosed one-letter-word in the input
        String enclosedShortIdCandidate = null;
        final int lastClosingIdx = lowerInput.lastIndexOf(PARSING_TYPE_CLOSE);
        if (lastClosingIdx > 0) {
            final int lastOpeningIdx = lowerInput.lastIndexOf(PARSING_TYPE_OPEN, lastClosingIdx);
            if (lastOpeningIdx >= 0 && lastOpeningIdx + PARSING_TYPE_OPEN.length() + 1 == lastClosingIdx) {
                enclosedShortIdCandidate = lowerInput.substring(lastClosingIdx - 1, lastClosingIdx);
            }
        }

        for (final WaypointType wpType : WaypointType.ALL_TYPES) {
            final String lowerShortId = wpType.getShortId().toLowerCase(Locale.getDefault());
            if (lowerLastWord.equals(lowerShortId) || lowerLastWord.contains(PARSING_TYPE_OPEN + lowerShortId + PARSING_TYPE_CLOSE)) {
                return wpType;
            }
        }
        if (enclosedShortIdCandidate != null) {
            for (final WaypointType wpType : WaypointType.ALL_TYPES) {
                if (enclosedShortIdCandidate.equals(wpType.getShortId().toLowerCase(Locale.getDefault()))) {
                    return wpType;
                }
            }
        }
        for (final WaypointType wpType : WaypointType.ALL_TYPES) {
            // check the old, longer waypoint names first (to not interfere with the shortened versions)
            if (lowerInput.contains(wpType.getL10n().toLowerCase(Locale.getDefault()))) {
                return wpType;
            }
        }
        for (final WaypointType wpType : WaypointType.ALL_TYPES) {
            // then check the new, shortened versions
            if (lowerInput.contains(wpType.getNameForNewWaypoint().toLowerCase(Locale.getDefault()))) {
                return wpType;
            }
        }
        for (final WaypointType wpType : WaypointType.ALL_TYPES) {
            if (lowerInput.contains(wpType.id)) {
                return wpType;
            }
        }
        for (final WaypointType wpType : WaypointType.ALL_TYPES) {
            if (lowerInput.contains(wpType.name().toLowerCase(Locale.US))) {
                return wpType;
            }
        }
        return WaypointType.WAYPOINT;
    }

    /**
     * try to parse a name out of given words. If not possible, null is returned
     * Typical example: @Plain Example 1 (W) (F-PLAIN) N 53° 33.AAA E 009° 59.BCD' |A=1|B=2|C=3|D=4| "Plain_example_1"
     */
    @NotNull
    private ImmutableTriple<CalculatedCoordinate, String, CalcState> parseFormula(final String text, final Settings.CoordInputFormatEnum formulaFormat) {
        try {
            final FormulaParser formulaParser = new FormulaParser(formulaFormat);
            final FormulaWrapper parsedFullCoordinates = formulaParser.parse(text);
            if (null != parsedFullCoordinates) {
                final String latText = parsedFullCoordinates.getFormulaLat();
                final String lonText = parsedFullCoordinates.getFormulaLon();
                final List<VariableData> variables = new ArrayList<>();

                // all text after the formula, keep newline
                String remainingString = parsedFullCoordinates.getText().substring(parsedFullCoordinates.getEnd());

                final String[] formulaList = remainingString.split(FormulaParser.WPC_DELIM_PATTERN_STRING);
                for (final String varSplitText : formulaList
                ) {
                    final String varText = varSplitText.trim();
                    boolean removeDelimiter = varText.isEmpty();
                    final String[] equations = varText.split("=", -1);
                    if (1 < equations.length) {
                        final String varName = equations[0].trim();
                        if (1 == varName.length()) {
                            removeDelimiter = true;
                            final String varExpression = equations[1].trim();
                            if (!varExpression.isEmpty()) {
                                variables.add(new VariableData(varName.charAt(0), varExpression));
                            }
                        }
                    }
                    if (removeDelimiter) {
                        final int idxWpcDelim = remainingString.indexOf(FormulaParser.WPC_DELIM);
                        remainingString = remainingString.substring(idxWpcDelim + 1);
                    } else {
                        break;
                    }
                }

                for (VariableData vd : variables) {
                    addVariable("" + vd.getName(), vd.getExpression(), false);
                }
                final CalculatedCoordinate cc = new CalculatedCoordinate();
                cc.setLatitudePattern(latText);
                cc.setLongitudePattern(lonText);
                cc.setType(CalculatedCoordinateType.PLAIN);

                final CalcState calcState = CoordinatesCalculateUtils.createCalcState(latText, lonText, variables);

                return new ImmutableTriple<>(cc, remainingString, calcState);
            }
        } catch (final FormulaParser.ParseException ignored) {
            // no formula
        }

        return new ImmutableTriple<>(null, text, null);
    }

    private void addVariable(final String name, final String expression, final boolean highPrio) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        final String varName = name.trim();
        final String varExpression = expression == null ? "" : expression.trim();
        final boolean varNotSet = StringUtils.isBlank(variables.get(varName));
        if (varNotSet || (highPrio && !StringUtils.isBlank(varExpression))) {
            variables.put(varName, varExpression);
        }
    }

    public static String removeParseableWaypointsFromText(final String text) {
        return TextUtils.replaceAll(text, BACKUP_TAG_OPEN, BACKUP_TAG_CLOSE, "").trim();
    }

    /**
     * Replaces waypoints stored in text with the ones passed as parameter.
     *
     * @param text      text to search and replace waypoints in
     * @param waypoints new waypoints to store
     * @return new text, or null if waypoints could not be placed due to size restrictions
     */
    public static String putParseableWaypointsInText(final String text, final Collection<Waypoint> waypoints, final VariableList vars) {
        String cleanText = removeParseableWaypointsFromText(text);
        if (!cleanText.isEmpty()) {
            cleanText = cleanText + "\n\n";
        }

        final String newWaypoints = getParseableText(waypoints, vars, true);
        return cleanText + newWaypoints;
    }

    /**
     * Tries to create a parseable text containing all  information from given waypoints
     * and meeting a given maximum text size. Different strategies are applied to meet
     * that text size.
     * if 'includeBackupTags' is set, then returned text is surrounded by tags
     *
     * @return parseable text for wayppints, or null if maxsize cannot be met
     */
    public static String getParseableText(final Collection<Waypoint> waypoints, final VariableList vars, final boolean includeBackupTags) {
        //no streaming allowed
        final List<String> waypointsAsStrings = new ArrayList<>();
        for (final Waypoint wp : waypoints) {
            waypointsAsStrings.add(getParseableText(wp));
        }
        if (vars != null) {
            waypointsAsStrings.add(getParseableVariableString(vars.toMap()));
        }
        return (includeBackupTags ? BACKUP_TAG_OPEN + "\n" : "") +
                StringUtils.join(waypointsAsStrings, "\n") +
                (includeBackupTags ? "\n" + BACKUP_TAG_CLOSE : "");
    }

    /**
     * creates parseable waypoint text
     *
     * @return parseable waypoint text
     */
    public static String getParseableText(final Waypoint wp) {
        return getParseableText(wp, false);
    }

    public static String getParseableText(final Waypoint wp, final boolean legacyMode) {
        final StringBuilder sb = new StringBuilder();
        //name
        sb.append(PARSING_NAME_PRAEFIX);
        if (!wp.isUserDefined()) {
            sb.append(PARSING_PREFIX_OPEN).append(wp.getPrefix()).append(PARSING_PREFIX_CLOSE);
        }
        sb.append(wp.getName()).append(" ");

        //type
        sb.append(PARSING_TYPE_OPEN).append(wp.getWaypointType().getShortId().toUpperCase(Locale.US))
                .append(PARSING_TYPE_CLOSE).append(" ");

        // formula
        final String formulaString = getParseableFormula(wp, legacyMode);
        if (StringUtils.isNotEmpty(formulaString)) {
            sb.append(formulaString);
        } else {
            //coordinate
            if (wp.getCoords() == null) {
                sb.append(PARSING_COORD_EMPTY);
            } else {
                sb.append(wp.getCoords().format(GeopointFormatter.Format.LAT_LON_DECMINUTE_SHORT_RAW));
            }
        }

        //user note
        final String userNote = wp.getUserNote();
        if (!StringUtils.isBlank(userNote)) {
            //if user note contains itself newlines, then start user note on a second line
            sb.append(userNote.contains("\n") ? "\n" : " ");
            sb.append(TextUtils.createDelimitedValue(userNote, PARSING_USERNOTE_DELIM, PARSING_USERNOTE_ESCAPE));
        }

        return sb.toString();
    }

    private static String getParseableFormula(final Waypoint wp, final boolean legacyMode) {
        final StringBuilder sb = new StringBuilder();

        final CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig(wp.getCalcStateConfig());
        if (cc.isFilled()) {
            sb.append(cc.toConfig());
        } else if (legacyMode) {
            final String calcStateJson = wp.getCalcStateConfig();
            if (null != calcStateJson) {
                final CalcState calcState = CalcState.fromJSON(calcStateJson);

                if (calcState != null) {
                    final String formulaString = getParseableFormulaString(calcState);
                    if (!formulaString.isEmpty()) {
                        sb.append(formulaString);
                        final String variableString = getParseableVariablesString(calcState);
                        if (!variableString.isEmpty()) {
                            sb.append(FormulaParser.WPC_DELIM).append(variableString);
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public static String getParseableVariableString(final Map<String, String> variables) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (!first) {
                sb.append(" | ");
            }
            first = false;
            sb.append("$").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private void parseVariablesFromString(final String pText) {
        final String text = " " + pText + "\n";
        final Matcher matcher = PARSING_VARS.matcher(text);
        int pos = 0;
        while (matcher.find(pos)) {
            final int group = matcher.group(1) == null ? 3 : 1;
            final String varName = matcher.group(group);
            final String value = matcher.group(group + 1);
            pos = matcher.end(group + 1);
            addVariable(varName, value, true);
        }
    }

    private static String getParseableFormulaString(final CalcState calcState) {
        final StringBuilder sb = new StringBuilder();
        if (calcState.format == Settings.CoordInputFormatEnum.Plain) {
            sb.append(PARSING_COORD_FORMULA_PLAIN + " ");
            sb.append(calcState.plainLat).append(" ").append(calcState.plainLon).append(" ");
        }
        return sb.toString();
    }

    private static String getParseableVariablesString(final CalcState calcState) {
        final StringBuilder sb = new StringBuilder();
        for (VariableData equ : calcState.equations) {
            final String equExpr = equ.getExpression().trim();
            if (!equExpr.isEmpty()) {
                sb.append(equ.getName()).append("=").append(equExpr).append(FormulaParser.WPC_DELIM);
            }
        }
        for (VariableData var : calcState.freeVariables) {
            final String varExpr = var.getExpression().trim();
            if (!varExpr.isEmpty()) {
                sb.append(var.getName()).append("=").append(varExpr).append(FormulaParser.WPC_DELIM);
            }
        }
        return sb.toString();
    }
}
