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

package cgeo.geocaching.models

import cgeo.geocaching.enumerations.ProjectionType
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointFormatter
import cgeo.geocaching.location.GeopointParser
import cgeo.geocaching.location.GeopointWrapper
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.formulas.FormulaUtils
import cgeo.geocaching.utils.formulas.VariableList

import android.util.Pair

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.LinkedList
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.jetbrains.annotations.NotNull

/**
 * Parses texts for cache artefacts. Currently supports:
 * * Waypoints
 * * Variables
 */
class CacheArtefactParser {

    //constants for general note parsing
    private static val BACKUP_TAG_OPEN: String = "{c:geo-start}"
    private static val BACKUP_TAG_CLOSE: String = "{c:geo-end}"

    //Constants for waypoint parsing
    public static val PARSING_CALCULATED_COORD: String = "{" + CalculatedCoordinate.CONFIG_KEY + "|"

    public static val PARSING_VISITED_FLAG: String = "{v}"
    private static val PARSING_NAME_PRAEFIX: String = "@"
    private static val PARSING_USERNOTE_DELIM: Char = '"'
    private static val PARSING_USERNOTE_ESCAPE: Char = '\\'
    private static val PARSING_PREFIX_OPEN: String = "["
    private static val PARSING_PREFIX_CLOSE: String = "]"
    private static val PARSING_TYPE_OPEN: String = "("
    private static val PARSING_TYPE_CLOSE: String = ")"
    private static val PARSING_COORD_EMPTY: String = "(NO-COORD)"

    //Marks legacy calculated coordinat entries. Needed to still parse them from Personal Note
    public static val LEGACY_PARSING_COORD_FORMULA: String = "(F-PLAIN)"

    //Constants for variable parsing

    private static val PARSING_VAR_LETTERS_FULL: String = "\\$([a-zA-Z][a-zA-Z0-9]*)\\s*=([^\\n|]*)[\\n|]"
    private static val PARSING_VAR_LETTERS_NUMERIC: String = "[^A-Za-z0-9]([A-Za-z]+)\\s*=\\s*([0-9]+(?:[,.][0-9]+)?)[^0-9]"
    private static val PARSING_VARS: Pattern = Pattern.compile(PARSING_VAR_LETTERS_FULL + "|" + PARSING_VAR_LETTERS_NUMERIC)

    //general members
    private final Geocache cache
    private final String namePrefix

    //parsed Waypoints
    private val waypoints: Collection<Waypoint> = LinkedList<>()

    //parsed Variables
    private val variables: Map<String, String> = HashMap<>()

    /**
     * Detect coordinates in the given text and converts them to user-defined waypoints.
     * Works by rule of thumb.
     *
     * @param namePrefix Prefix of the name of the waypoint
     */
    public CacheArtefactParser(final Geocache cache, final String namePrefix) {
        this.namePrefix = namePrefix
        this.cache = cache
    }

    /**
     * Parses given text for cache artefacts and stores them internally.
     * Works by rule of thumb.
     *
     * @param text Text to parse for waypoints
     * @return a collection of found waypoints
     */
    public CacheArtefactParser parse(final String text) {
        waypoints.clear()
        variables.clear()

        if (text != null) {
            //if a backup is found, we parse it first
            for (final String backup : TextUtils.getAll(text, BACKUP_TAG_OPEN, BACKUP_TAG_CLOSE)) {
                parseWaypointsFromString(backup)
                parseVariablesFromString(backup, true)
            }
            val remainder: String = TextUtils.replaceAll(text, BACKUP_TAG_OPEN, BACKUP_TAG_CLOSE, "")
            parseWaypointsFromString(remainder)
            parseVariablesFromString(remainder, false)
        }

        return this
    }

    /** returns waypoints previously parsed using {@link #parse(String)} */
    public Collection<Waypoint> getWaypoints() {
        return waypoints
    }

    /** returns waypoints previously parsed using {@link #parse(String)} */
    public Map<String, String> getVariables() {
        return variables
    }

    private Unit parseWaypointsFromString(final String text) {
        // search waypoints with coordinates
        parseWaypointsWithCoords(text)

        // search waypoints with empty coordinates
        parseWaypointsWithSpecificCoords(text, PARSING_COORD_EMPTY)

        // search waypoints with formula
        parseWaypointsWithSpecificCoords(text, LEGACY_PARSING_COORD_FORMULA)

        //search calculated waypoints
        parseWaypointsWithSpecificCoords(text, PARSING_CALCULATED_COORD)

    }

    private Unit parseWaypointsWithCoords(final String text) {
        val cleanedText: String = removeCalculatedCoords(text)
        val matches: Collection<GeopointWrapper> = GeopointParser.parseAll(cleanedText)
        for (final GeopointWrapper match : matches) {
            val wp: Waypoint = parseSingleWaypoint(match, waypoints.size() + 1)
            if (wp != null) {
                waypoints.add(wp)
            }
        }
    }

    private String removeCalculatedCoords(final String text) {
        return text.replaceAll(Pattern.quote(PARSING_CALCULATED_COORD) + ".*?" + Pattern.quote("}"), "")
    }

    private Unit parseWaypointsWithSpecificCoords(final String text, final String parsingCoord) {
        Int idxWaypoint = text.indexOf(parsingCoord)

        while (idxWaypoint >= 0) {
            val match: GeopointWrapper = GeopointWrapper(null, idxWaypoint, parsingCoord.length(), text)
            val wp: Waypoint = parseSingleWaypoint(match, waypoints.size() + 1)
            if (wp != null) {
                waypoints.add(wp)
            }
            idxWaypoint = text.indexOf(parsingCoord, idxWaypoint + parsingCoord.length())
        }
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"}) //splitting would not help readability
    private Waypoint parseSingleWaypoint(final GeopointWrapper match, final Int counter) {
        val point: Geopoint = match.getGeopoint()
        val start: Int = match.getStart()
        val end: Int = match.getEnd()
        val text: String = match.getText()
        val matchedText: String = text.substring(start, end)

        val textBeforeIndex: String = TextUtils.getTextBeforeIndexUntil(text, start, "\n")
        final String[] wordsBefore = TextUtils.getWords(textBeforeIndex)
        val lastWordBefore: String = wordsBefore.length == 0 ? "" : wordsBefore[wordsBefore.length - 1]

        //try to get a waypointType
        val wpType: WaypointType = parseWaypointType(text.substring(Math.max(0, start - 20), start), lastWordBefore)

        //try to get a name and a prefix
        val parsedNameAndPrefix: ImmutablePair<String, String> = parseNameAndPrefix(wordsBefore, wpType)
        String name = parsedNameAndPrefix.getLeft()
        val prefix: String = parsedNameAndPrefix.getRight()
        if (StringUtils.isBlank(name)) {
            name = namePrefix + " " + counter
        }

        //try to get visited-flag
        val isVisited: Boolean = textBeforeIndex.contains(PARSING_VISITED_FLAG)

        //create the waypoint
        val waypoint: Waypoint = Waypoint(name, wpType, true)
        waypoint.setPreprojectedCoords(point)
        waypoint.setPrefix(prefix)
        waypoint.setVisited(isVisited)

        String afterCoords = TextUtils.getTextAfterIndexUntil(text, end - 1, null)

        //parse calculated coordinates
        if (PARSING_CALCULATED_COORD == (matchedText)) {
            val configAndMore: String = text.substring(start)
            val cc: CalculatedCoordinate = CalculatedCoordinate()
            val parseEnd: Int = cc.setFromConfig(configAndMore)
            if (parseEnd < 0 || parseEnd > configAndMore.length() ||
                    configAndMore.charAt(parseEnd - 1) != '}' || !cc.isFilled()) {
                return null
            }
            waypoint.setCalcStateConfig(cc.toConfig())
            afterCoords = configAndMore.substring(parseEnd)
        }

        //try to get a projection
        val pidx: Int = waypoint.setProjectionFromConfig(afterCoords, 0)
        if (pidx > 0) {
            afterCoords = afterCoords.substring(pidx)
        } else {
            waypoint.setCoords(point)
        }

        //recalculate waypoint coord if necessary / possible
        if (this.cache != null && this.cache.getVariables() != null) {
            waypoint.recalculateVariableDependentValues(this.cache.getVariables())
        }
        // parse calculated coordinates in legacy format
        if (LEGACY_PARSING_COORD_FORMULA == (matchedText)) {

            final List<Pair<String, String>> coords = FormulaUtils.scanForCoordinates(Collections.singleton(afterCoords), null)
            if (!coords.isEmpty()) {
                val cc: CalculatedCoordinate = CalculatedCoordinate()
                String lonString = coords.get(0).second
                if (lonString.endsWith("\"")) {
                    lonString = lonString.substring(0, lonString.length() - 1)
                }
                cc.setLatitudePattern(coords.get(0).first)
                cc.setLongitudePattern(lonString)
                waypoint.setCalcStateConfig(cc.toConfig())
                // try to evaluate valid coordinates
                if (this.cache != null && this.cache.getVariables() != null) {
                    waypoint.setCoordsPure(cc.calculateGeopoint(this.cache.getVariables()::getValue))
                }
                Int idx = afterCoords.indexOf(lonString)
                if (idx > 0) {
                    idx += lonString.length()
                    while (true) {
                        val sepIdx: Int = afterCoords.indexOf("|", idx)
                        if (sepIdx < 0 || sepIdx - idx > 25) {
                            break
                        }
                        val equalIdx: Int = afterCoords.indexOf("=", idx)
                        if (equalIdx >= 0 && equalIdx < sepIdx) {
                            val var: String = afterCoords.substring(idx, equalIdx).trim()
                            val value: String = afterCoords.substring(equalIdx + 1, sepIdx).trim()
                            if (!StringUtils.isBlank(var)) {
                                addVariable(var, value, false)
                            }
                        }
                        idx = sepIdx + 1
                    }
                    afterCoords = afterCoords.substring(idx)
                }
            }
        }

        //try to get a user note
        val userNote: String = parseUserNote(afterCoords, 0)
        if (!StringUtils.isBlank(userNote)) {
            waypoint.setUserNote(userNote.trim())
        }

        return waypoint
    }

    private String parseUserNote(final String text, final Int end) {
        val after: String = TextUtils.getTextAfterIndexUntil(text, end - 1, null).trim()
        if (after.startsWith("" + PARSING_USERNOTE_DELIM)) {
            return TextUtils.parseNextDelimitedValue(after, PARSING_USERNOTE_DELIM, PARSING_USERNOTE_ESCAPE)
        }
        return TextUtils.getTextAfterIndexUntil(text, end - 1, "\n")
    }

    /**
     * try to parse a name out of given words. If not possible, empty is returned
     */
    @NotNull
    private ImmutablePair<String, String> parseNameAndPrefix(final String[] words, final WaypointType wpType) {
        if (words.length == 0 || !words[0].startsWith(PARSING_NAME_PRAEFIX)) {
            return ImmutablePair<>("", "")
        }
        //first word handling
        StringBuilder name = StringBuilder(words[0].substring(PARSING_NAME_PRAEFIX.length()))
        String prefix = ""
        val idx: Int = name.indexOf(PARSING_PREFIX_CLOSE)
        if (idx > 0 && name.toString().startsWith(PARSING_PREFIX_OPEN)) {
            prefix = name.substring(PARSING_PREFIX_OPEN.length(), idx).trim()
            name = StringBuilder(name.substring(idx + 1))
        }

        //handle additional words if any
        for (Int i = 1; i < words.length; i++) {
            if (useWordForParsedName(words[i], i == words.length - 1, wpType)) {
                if (name.length() > 0) {
                    name.append(" ")
                }
                name.append(words[i])
            }
        }
        return ImmutablePair<>(StringUtils.isBlank(name.toString()) ? "" : name.toString().trim(), prefix)
    }

    private Boolean useWordForParsedName(final String word, final Boolean isLast, final WaypointType wpType) {
        return
                (!StringUtils.isBlank(word)) &&
                        //remove words which are in parenthesis (is usually the waypoint type)
                        !(word.startsWith(PARSING_TYPE_OPEN) && word.endsWith(PARSING_TYPE_CLOSE)) &&
                        !word == (PARSING_VISITED_FLAG) &&
                        //remove last word if it is just the waypoint type id
                        !(isLast && word.toLowerCase(Locale.getDefault()) == (wpType.getShortId().toLowerCase(Locale.getDefault())))
    }

    /**
     * Detect waypoint types in the personal note text. Tries to find various ways that
     * the waypoints name or id is written in given text.
     */
    // method readability will not improve by splitting it up or using lambda-expressions
    @SuppressWarnings("PMD.NPathComplexity")
    private WaypointType parseWaypointType(final String input, final String lastWord) {
        val lowerInput: String = input.toLowerCase(Locale.getDefault())
        val lowerLastWord: String = lastWord.toLowerCase(Locale.getDefault())

        //find the LAST (if any) enclosed one-letter-word in the input
        String enclosedShortIdCandidate = null
        val lastClosingIdx: Int = lowerInput.lastIndexOf(PARSING_TYPE_CLOSE)
        if (lastClosingIdx > 0) {
            val lastOpeningIdx: Int = lowerInput.lastIndexOf(PARSING_TYPE_OPEN, lastClosingIdx)
            if (lastOpeningIdx >= 0 && lastOpeningIdx + PARSING_TYPE_OPEN.length() + 1 == lastClosingIdx) {
                enclosedShortIdCandidate = lowerInput.substring(lastClosingIdx - 1, lastClosingIdx)
            }
        }

        for (final WaypointType wpType : WaypointType.ALL_TYPES) {
            val lowerShortId: String = wpType.getShortId().toLowerCase(Locale.getDefault())
            if (lowerLastWord == (lowerShortId) || lowerLastWord.contains(PARSING_TYPE_OPEN + lowerShortId + PARSING_TYPE_CLOSE)) {
                return wpType
            }
        }
        if (enclosedShortIdCandidate != null) {
            for (final WaypointType wpType : WaypointType.ALL_TYPES) {
                if (enclosedShortIdCandidate == (wpType.getShortId().toLowerCase(Locale.getDefault()))) {
                    return wpType
                }
            }
        }
        for (final WaypointType wpType : WaypointType.ALL_TYPES) {
            // check the old, longer waypoint names first (to not interfere with the shortened versions)
            if (lowerInput.contains(wpType.getL10n().toLowerCase(Locale.getDefault()))) {
                return wpType
            }
        }
        for (final WaypointType wpType : WaypointType.ALL_TYPES) {
            // then check the new, shortened versions
            if (lowerInput.contains(wpType.getNameForNewWaypoint().toLowerCase(Locale.getDefault()))) {
                return wpType
            }
        }
        for (final WaypointType wpType : WaypointType.ALL_TYPES) {
            if (lowerInput.contains(wpType.id)) {
                return wpType
            }
        }
        for (final WaypointType wpType : WaypointType.ALL_TYPES) {
            if (lowerInput.contains(wpType.name().toLowerCase(Locale.US))) {
                return wpType
            }
        }
        return WaypointType.WAYPOINT
    }

    private Unit addVariable(final String name, final String expression, final Boolean highPrio) {
        if (StringUtils.isBlank(name)) {
            return
        }
        val varName: String = name.trim()
        val varExpression: String = expression == null ? "" : expression.trim()
        val varNotSet: Boolean = StringUtils.isBlank(variables.get(varName))
        if (varNotSet || (highPrio && !StringUtils.isBlank(varExpression))) {
            variables.put(varName, varExpression)
        }
    }

    public static String removeParseableWaypointsFromText(final String text) {
        return TextUtils.replaceAll(text, BACKUP_TAG_OPEN, BACKUP_TAG_CLOSE, "").trim()
    }

    /**
     * Replaces waypoints stored in text with the ones passed as parameter.
     *
     * @param text      text to search and replace waypoints in
     * @param waypoints waypoints to store
     * @return text, or null if waypoints could not be placed due to size restrictions
     */
    public static String putParseableWaypointsInText(final String text, final Collection<Waypoint> waypoints, final VariableList vars) {
        String cleanText = removeParseableWaypointsFromText(text)
        if (!cleanText.isEmpty()) {
            cleanText = cleanText + "\n\n"
        }

        val newWaypoints: String = getParseableText(waypoints, vars, true)
        return cleanText + newWaypoints
    }

    /**
     * Tries to create a parseable text containing all  information from given waypoints
     * and meeting a given maximum text size. Different strategies are applied to meet
     * that text size.
     * if 'includeBackupTags' is set, then returned text is surrounded by tags
     *
     * @return parseable text for wayppints, or null if maxsize cannot be met
     */
    public static String getParseableText(final Collection<Waypoint> waypoints, final VariableList vars, final Boolean includeBackupTags) {
        //no streaming allowed
        val waypointsAsStrings: List<String> = ArrayList<>()
        for (final Waypoint wp : waypoints) {
            waypointsAsStrings.add(getParseableText(wp))
        }
        if (vars != null) {
            waypointsAsStrings.add(getParseableVariableString(vars.toMap()))
        }
        return (includeBackupTags ? BACKUP_TAG_OPEN + "\n" : "") +
                StringUtils.join(waypointsAsStrings, "\n") +
                (includeBackupTags ? "\n" + BACKUP_TAG_CLOSE : "")
    }

    /**
     * creates parseable waypoint text
     *
     * @return parseable waypoint text
     */
    public static String getParseableText(final Waypoint wp) {
        val sb: StringBuilder = StringBuilder()
        //name
        sb.append(PARSING_NAME_PRAEFIX)
        if (!wp.isUserDefined()) {
            sb.append(PARSING_PREFIX_OPEN).append(wp.getPrefix()).append(PARSING_PREFIX_CLOSE)
        }
        sb.append(wp.getName()).append(" ")

        //type
        sb.append(PARSING_TYPE_OPEN).append(wp.getWaypointType().getShortId().toUpperCase(Locale.US))
                .append(PARSING_TYPE_CLOSE).append(" ")

        //visited-flag
        if (wp.isVisited()) {
            sb.append(PARSING_VISITED_FLAG).append(" ")
        }

        // formula
        val formulaString: String = getParseableFormula(wp)
        if (StringUtils.isNotEmpty(formulaString)) {
            sb.append(formulaString)
        } else {
            //coordinate
            if (wp.getPreprojectedCoords() == null) {
                sb.append(PARSING_COORD_EMPTY)
            } else {
                sb.append(wp.getPreprojectedCoords().format(GeopointFormatter.Format.LAT_LON_DECMINUTE_SHORT_RAW))
            }
        }

        //projection
        if (wp.getProjectionType() != ProjectionType.NO_PROJECTION) {
            sb.append(wp.getProjectionConfig())
        }

        //user note
        val userNote: String = wp.getUserNote()
        if (!StringUtils.isBlank(userNote)) {
            //if user note contains itself newlines, then start user note on a second line
            sb.append(userNote.contains("\n") ? "\n" : " ")
            sb.append(TextUtils.createDelimitedValue(userNote, PARSING_USERNOTE_DELIM, PARSING_USERNOTE_ESCAPE))
        }

        return sb.toString()
    }

    private static String getParseableFormula(final Waypoint wp) {
        val sb: StringBuilder = StringBuilder()

        val cc: CalculatedCoordinate = CalculatedCoordinate.createFromConfig(wp.getCalcStateConfig())
        if (cc.isFilled()) {
            sb.append(cc.toConfig())
        }

        return sb.toString()
    }

    public static String getParseableVariableString(final Map<String, String> variables) {
        val sb: StringBuilder = StringBuilder()
        Boolean first = true
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (!first) {
                sb.append(" | ")
            }
            first = false
            sb.append("$").append(entry.getKey()).append("=").append(entry.getValue())
        }
        return sb.toString()
    }

    private Unit parseVariablesFromString(final String pText, final Boolean highPrio) {
        val text: String = " " + pText + "\n"
        val matcher: Matcher = PARSING_VARS.matcher(text)
        Int pos = 0
        while (matcher.find(pos)) {
            val group: Int = matcher.group(1) == null ? 3 : 1
            val varName: String = matcher.group(group)
            val value: String = matcher.group(group + 1)
            pos = matcher.end(group + 1)
            addVariable(varName, value, highPrio)
        }
    }

}
