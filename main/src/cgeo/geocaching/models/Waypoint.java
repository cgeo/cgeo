package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.GeopointParser;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.jetbrains.annotations.NotNull;

public class Waypoint implements IWaypoint {

    public static final String PREFIX_OWN = "OWN";
    public static final String CLIPBOARD_PREFIX = "c:geo:WAYPOINT:";

    private static final String WP_PREFIX_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int ORDER_UNDEFINED = -2;

    //Constants for waypoint parsing
    private static final String PARSING_NAME_PRAEFIX = "@";
    private static final char PARSING_USERNOTE_DELIM = '"';
    private static final char PARSING_USERNOTE_ESCAPE = '\\';
    private static final String PARSING_USERNOTE_CONTINUED = "...";
    private static final String PARSING_TYPE_OPEN = "(";
    private static final String PARSING_TYPE_CLOSE = ")";
    private static final String BACKUP_TAG_OPEN = "<----->";
    private static final String BACKUP_TAG_CLOSE = "</----->";

    private int id = -1;
    private String geocode = "geocode";
    private WaypointType waypointType = WaypointType.WAYPOINT;
    private String prefix = "";
    private String lookup = "";
    private String name = "";
    private Geopoint coords = null;
    private String note = "";
    private String userNote = "";
    private int cachedOrder = ORDER_UNDEFINED;
    private boolean own = false;

    private boolean visited = false;
    private boolean originalCoordsEmpty = false;

    private String calcStateJson = null;

    /**
     * Sort waypoints by their probable order (e.g. parking first, final last).
     * use Geocache::getWaypointComparator() to retrieve the adequate comparator for your cache
     */
    public static final Comparator<? super Waypoint> WAYPOINT_COMPARATOR = (Comparator<Waypoint>) (left, right) -> left.order() - right.order();

    /**
     * Sort waypoints by internal id descending (results in newest on top)
     * used only for "goto history" UDC
     * use Geocache::getWaypointComparator() to retrieve the adequate comparator for your cache
     */
    public static final Comparator<? super Waypoint> WAYPOINT_ID_COMPARATOR = (Comparator<Waypoint>) (left, right) -> right.id - left.id;

    /**
     * require name and type for every waypoint
     */
    public Waypoint(final String name, final WaypointType type, final boolean own) {
        this.name = name;
        this.waypointType = type;
        this.own = own;
    }

    public Waypoint(final String lookup, final Geopoint coords, final String name, final String prefix, final String note, final WaypointType type) {
        this(name, type, false);
        this.prefix = prefix;
        this.lookup = lookup;
        this.coords = coords;
        this.note = note;
    }

    /**
     * copy constructor
     */
    public Waypoint(final Waypoint other) {
        merge(other);
        this.waypointType = other.waypointType;
        id = -1;
    }

    public void merge(final Waypoint old) {
        if (StringUtils.isBlank(prefix)) {
            setPrefix(old.prefix);
        }
        if (StringUtils.isBlank(lookup)) {
            lookup = old.lookup;
        }
        if (StringUtils.isBlank(name)) {
            name = old.name;
        }
        if (coords == null) {
            coords = old.coords;
        }
        if (StringUtils.isBlank(note)) {
            note = old.note;
        }
        if (StringUtils.isBlank(userNote)) {
            userNote = old.userNote;
        }
        if (StringUtils.equals(note, userNote)) {
            userNote = "";
        }
        if (calcStateJson == null) {
            calcStateJson = old.calcStateJson;
        }
        if (id < 0) {
            id = old.id;
        }
        visited = old.visited;
    }

    public static void mergeWayPoints(final List<Waypoint> newPoints, final List<Waypoint> oldPoints, final boolean forceMerge) {
        // Build a map of new waypoints for faster subsequent lookups
        final Map<String, Waypoint> newPrefixes = new HashMap<>(newPoints.size());
        for (final Waypoint waypoint : newPoints) {
            newPrefixes.put(waypoint.getPrefix(), waypoint);
        }

        // Copy user modified details of the old waypoints over the new ones
        for (final Waypoint oldWaypoint : oldPoints) {
            final String prefix = oldWaypoint.getPrefix();
            if (newPrefixes.containsKey(prefix)) {
                final Waypoint newWaypoint = newPrefixes.get(prefix);
                if (oldWaypoint.isUserDefined() && !newWaypoint.isUserDefined()) {
                    assignUniquePrefix(oldWaypoint, newPoints);
                    newPoints.add(oldWaypoint);
                } else {
                    newWaypoint.merge(oldWaypoint);
                }
            } else if (oldWaypoint.isUserDefined() || forceMerge) {
                newPoints.add(oldWaypoint);
            }
        }
    }

    public boolean isUserDefined() {
        return own || waypointType == WaypointType.OWN;
    }

    public void setUserDefined() {
        own = true;
        setPrefix(PREFIX_OWN);
    }

    private int order() {
        if (cachedOrder == ORDER_UNDEFINED) {
            cachedOrder = waypointType.order;
        }
        return cachedOrder;
    }

    @NonNull
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(@NonNull final String prefix) {
        this.prefix = prefix;
        cachedOrder = ORDER_UNDEFINED;
    }

    @NonNull
    public String getUrl() {
        return "https://www.geocaching.com/seek/cache_details.aspx?wp=" + geocode;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Override
    public String getGeocode() {
        return geocode;
    }

    public void setGeocode(final String geocode) {
        this.geocode = StringUtils.upperCase(geocode);
    }

    @Override
    public WaypointType getWaypointType() {
        return waypointType;
    }

    public String getLookup() {
        return lookup;
    }

    public void setLookup(final String lookup) {
        this.lookup = lookup;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Nullable
    @Override
    public Geopoint getCoords() {
        return coords;
    }

    public void setCoords(final Geopoint coords) {
        this.coords = coords;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    @Override
    @NonNull
    public String toString() {
        return name + " " + waypointType.getL10n();
    }

    /**
     * Checks whether a given waypoint is a final and has coordinates
     *
     * @return True - waypoint is final and has coordinates, False - otherwise
     */
    public boolean isFinalWithCoords() {
        return waypointType == WaypointType.FINAL && coords != null;
    }

    @Override
    public CoordinatesType getCoordType() {
        return CoordinatesType.WAYPOINT;
    }

    public void setVisited(final boolean visited) {
        this.visited = visited;
    }

    public boolean isVisited() {
        return visited;
    }

    public int getStaticMapsHashcode() {
        long hash = 0;
        if (coords != null) {
            hash = coords.hashCode();
        }
        hash ^= waypointType.markerId;
        return (int) hash;
    }

    /**
     * Delegates the creation of the waypoint-id for gpx-export to the waypoint
     */
    public String getGpxId() {

        String gpxId = prefix;

        if (StringUtils.isNotBlank(geocode)) {
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            if (cache != null) {
                gpxId = cache.getWaypointGpxId(prefix);
            }
        }

        return gpxId;
    }

    /**
     * Detect coordinates in the given text and converts them to user-defined waypoints.
     * Works by rule of thumb.
     *
     * @param text       Text to parse for waypoints
     * @param namePrefix Prefix of the name of the waypoint
     * @return a collection of found waypoints
     */
    public static Collection<Waypoint> parseWaypoints(@NonNull final String text, @NonNull final String namePrefix) {
        final List<Waypoint> waypoints = new LinkedList<>();

        //if a backup is found, we parse it first
        for (final String backup : TextUtils.getAll(text, BACKUP_TAG_OPEN, BACKUP_TAG_CLOSE)) {
            parseWaypoints(waypoints, backup, namePrefix);
        }
        parseWaypoints(waypoints, TextUtils.replaceAll(text, BACKUP_TAG_OPEN, BACKUP_TAG_CLOSE, ""), namePrefix);

        return waypoints;
    }

    private static void parseWaypoints(final Collection<Waypoint> waypoints, final String text, final String namePrefix) {
        final Collection<ImmutableTriple<Geopoint, Integer, Integer>> matches = GeopointParser.parseAll(text);

        int count = 1;
        for (final ImmutableTriple<Geopoint, Integer, Integer> match : matches) {
            final Geopoint point = match.getLeft();
            final Integer start = match.getMiddle();
            final Integer end = match.getRight();

            final String[] wordsBefore = TextUtils.getWords(TextUtils.getTextBeforeIndexUntil(text, start, "\n"));
            final String lastWordBefore = wordsBefore.length == 0 ? "" : wordsBefore[wordsBefore.length - 1];

            //try to get a waypointType
            final WaypointType wpType = parseWaypointType(text.substring(Math.max(0, start - 20), start), lastWordBefore);

            //try to get a name
            String name = parseName(wordsBefore, wpType);
            if (name == null) {
                name = namePrefix + " " + count;
            }

            //create the waypoint
            final Waypoint waypoint = new Waypoint(name, wpType, true);
            waypoint.setCoords(point);

            //try to get a user note
            final String userNote = parseUserNote(text, end);
            if (!StringUtils.isBlank(userNote)) {
                waypoint.setUserNote(userNote.trim());
            }

            waypoints.add(waypoint);
            count++;
        }
    }

    private static String parseUserNote(final String text, final int end) {
        final String after = TextUtils.getTextAfterIndexUntil(text, end - 1, null).trim();
        if (after.startsWith("" + PARSING_USERNOTE_DELIM)) {
            return TextUtils.parseNextDelimitedValue(after, PARSING_USERNOTE_DELIM, PARSING_USERNOTE_ESCAPE);
        }
        return TextUtils.getTextAfterIndexUntil(text, end - 1, "\n");
    }

    /**
     * try to parse a name out of given words. If not possible, null is returned
     */
    @NotNull
    private static String parseName(final String[] words, final WaypointType wpType) {
        String name = "";
        if (words.length > 0 && words[0].startsWith(PARSING_NAME_PRAEFIX)) {
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                //remove parsing_name_praefix in first word
                if (i == 0) {
                    word = word.substring(1);
                }
                //remove words which are in parenthesis
                if (word.startsWith(PARSING_TYPE_OPEN) && word.endsWith(PARSING_TYPE_CLOSE)) {
                    continue;
                }
                //remove last word if it is just the waypoint type id
                if (i == words.length - 1 && word.toLowerCase(Locale.getDefault())
                        .equals(wpType.getShortId().toLowerCase(Locale.getDefault()))) {
                    continue;
                }
                if (!StringUtils.isBlank(word)) {
                    if (name.length() > 0) {
                        name += " ";
                    }
                    name += word;
                }
            }
        }
        return StringUtils.isBlank(name) ? null : name.trim();
    }

    /**
     * Detect waypoint types in the personal note text. Tries to find various ways that
     * the waypoints name or id is written in given text.
     */
    private static WaypointType parseWaypointType(final String input, final String lastWord) {
        final String lowerInput = input.toLowerCase(Locale.getDefault());
        final String lowerLastWord = lastWord.toLowerCase(Locale.getDefault());
        for (final WaypointType wpType : WaypointType.values()) {
            final String lowerShortId = wpType.getShortId().toLowerCase(Locale.getDefault());
            if (lowerLastWord.equals(lowerShortId)) {
                return wpType;
            }
            if (lowerInput.contains(PARSING_TYPE_OPEN + lowerShortId + PARSING_TYPE_CLOSE)) {
                return wpType;
            }
            if (lowerInput.contains(wpType.getL10n().toLowerCase(Locale.getDefault()))) {
                return wpType;
            }
            if (lowerInput.contains(wpType.id)) {
                return wpType;
            }
            if (lowerInput.contains(wpType.name().toLowerCase(Locale.US))) {
                return wpType;
            }
        }
        return WaypointType.WAYPOINT;
    }

    /**
     * Replaces waypoints stored in text with the ones passed as parameter.
     *
     * @param text      text to search and replace waypoints in
     * @param waypoints new waypoints to store
     * @param maxSize   if >0 then total size of returned text may not exceed this parameter
     * @return new text, or null if waypoints could not be placed due to size restrictions
     */
    public static String putParseableWaypointTextstore(final String text, final Collection<Waypoint> waypoints, final int maxSize) {
        final String cleanText = TextUtils.replaceAll(text, BACKUP_TAG_OPEN, BACKUP_TAG_CLOSE, "").trim() + "\n\n";
        if (maxSize > -1 && cleanText.length() > maxSize) {
            return null;
        }
        final String newWaypoints = getParseableText(waypoints, maxSize - cleanText.length(), true);
        if (newWaypoints == null) {
            return null;
        }
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
    public static String getParseableText(final Collection<Waypoint> waypoints, final int maxSize, final boolean includeBackupTags) {
        String text = getParseableText(waypoints, true, -1, includeBackupTags);
        if (maxSize < 0 || text.length() <= maxSize) {
            return text;
        }

        //try to shrink size using maximum user note length
        for (int maxUserNoteLength = 50; maxUserNoteLength >= 0; maxUserNoteLength -= 10) {
            text = getParseableText(waypoints, true, maxUserNoteLength, includeBackupTags);
            if (text.length() <= maxSize) {
                return text;
            }
        }

        //try to shrink size by creating without user notes and name
        text = getParseableText(waypoints, false, 0, includeBackupTags);
        if (text.length() <= maxSize) {
            return text;
        }
        //not possible to meet size requirements
        return null;
    }

    public static String getParseableText(final Collection<Waypoint> waypoints, final boolean includeName, final int maxUserNoteSize, final boolean includeBackupTags) {
        //no streaming allowed
        final List<String> waypointsAsStrings = new ArrayList<>();
        for (final Waypoint w : waypoints) {
            waypointsAsStrings.add(w.getParseableText(includeName, maxUserNoteSize));
        }
        return (includeBackupTags ? BACKUP_TAG_OPEN + "\n" : "") +
                StringUtils.join(waypointsAsStrings, "\n") +
                (includeBackupTags ? "\n" + BACKUP_TAG_CLOSE : "");
    }

    /**
     * creates parseable waypoint text
     *
     * @param includeName     if true, name will be included
     * @param maxUserNoteSize if -1, user notes size is not limited. if 0, user note is omitted.
     *                        if >0 user note size is limited to given size
     * @return
     */
    public String getParseableText(final boolean includeName, final int maxUserNoteSize) {
        final StringBuilder sb = new StringBuilder();
        //name
        if (includeName) {
            sb.append(PARSING_NAME_PRAEFIX).append(this.getName()).append(" ");
        }
        //type
        sb.append(PARSING_TYPE_OPEN).append(this.getWaypointType().getShortId().toUpperCase(Locale.US))
                .append(PARSING_TYPE_CLOSE).append(" ");
        //coordinate
        sb.append(getCoords().format(GeopointFormatter.Format.LAT_LON_DECMINUTE_SHORT));
        //user note
        if (maxUserNoteSize != 0 && !StringUtils.isBlank(getUserNote())) {
            String userNote = getUserNote();
            if (maxUserNoteSize > 0 && userNote.length() > maxUserNoteSize) {
                userNote = userNote.substring(0, maxUserNoteSize) + PARSING_USERNOTE_CONTINUED;
            }
            sb.append("\n").append(TextUtils.createDelimitedValue(userNote, PARSING_USERNOTE_DELIM, PARSING_USERNOTE_ESCAPE));
        }
        return sb.toString();
    }

    public boolean mergeFromParsedText(final Waypoint parsedWaypoint, final String parsePraefix) {
        boolean changed = false;

        final String waypointTypeName = getWaypointType().getL10n().toLowerCase(Locale.getDefault());

        if (this.isUserDefined()) {
            //type
            if (this.getWaypointType() == WaypointType.WAYPOINT &&
                    parsedWaypoint.getWaypointType() != WaypointType.WAYPOINT) {
                changed = true;
                waypointType = parsedWaypoint.getWaypointType();
            }
            //name: change when existing waypoint has a "default name"
            if (startsWithAnyLower(getName(), parsePraefix, waypointTypeName) &
                    !startsWithAnyLower(parsedWaypoint.getName(), parsePraefix, waypointTypeName)) {
                this.setName(parsedWaypoint.getName());
                changed = true;
            }
        }
        //user note
        if (StringUtils.isBlank(this.getUserNote()) && !StringUtils.isBlank(parsedWaypoint.getUserNote())) {
            this.setUserNote(parsedWaypoint.getUserNote());
            changed = true;
        }
        return changed;
    }

    private static boolean startsWithAnyLower(final String text, final String... compare) {
        final String textLower = text.toLowerCase(Locale.getDefault());
        for (String s : compare) {
            if (textLower.startsWith(s.toLowerCase(Locale.getDefault()))) {
                return true;
            }
        }
        return false;
    }

    public GeoitemRef getGeoitemRef() {
        return new GeoitemRef(getGpxId(), getCoordType(), getGeocode(), getId(), getName(), getWaypointType().markerId);
    }

    public String getUserNote() {
        return userNote;
    }

    public void setUserNote(final String userNote) {
        this.userNote = userNote;
    }

    public boolean isOriginalCoordsEmpty() {
        return originalCoordsEmpty;
    }

    public void setOriginalCoordsEmpty(final boolean originalCoordsEmpty) {
        this.originalCoordsEmpty = originalCoordsEmpty;
    }

    public String getCalcStateJson() {
        return calcStateJson;
    }

    public void setCalcStateJson(final String calcStateJson) {
        this.calcStateJson = calcStateJson;
    }

    /*
     * Assigns a unique two-character (compatibility with gc.com)
     * prefix within the scope of this cache.
     */
    public static void assignUniquePrefix(final Waypoint waypoint, final Collection<Waypoint> existingWaypoints) {
        // gather existing prefixes
        final Set<String> assignedPrefixes = new HashSet<>();
        for (final Waypoint wp : existingWaypoints) {
            assignedPrefixes.add(wp.getPrefix());
        }

        final int length = WP_PREFIX_CHARS.length();
        for (int i = 0; i < length * length; i++) {
            final String prefixCandidate = Character.toString(WP_PREFIX_CHARS.charAt(i / length)) + WP_PREFIX_CHARS.charAt(i % length);
            if (!assignedPrefixes.contains(prefixCandidate)) {
                waypoint.setPrefix(prefixCandidate);
                return;
            }
        }

        throw new IllegalStateException("too many waypoints, unable to assign unique prefix");
    }

    /**
     * Suffix the waypoint type with a running number to get a default name.
     *
     * @param type type to create a new default name for
     */
    public static String getDefaultWaypointName(final Geocache cache, final WaypointType type) {
        final ArrayList<String> wpNames = new ArrayList<>();
        for (final Waypoint waypoint : cache.getWaypoints()) {
            wpNames.add(waypoint.getName());
        }
        // try final and trailhead without index
        if ((type == WaypointType.FINAL || type == WaypointType.TRAILHEAD) && !wpNames.contains(type.getL10n())) {
            return type.getL10n();
        }
        // for other types add an index by default, which is highest found index + 1
        int max = 0;
        for (int i = 0; i < 30; i++) {
            if (wpNames.contains(type.getL10n() + " " + i)) {
                max = i;
            }
        }
        return type.getL10n() + " " + (max + 1);
    }

    /**
     * returns a string with current waypoint's id to clipboard, preceeded by internal prefix
     * this is to be pushed to clipboard
     */
    public CharSequence reformatForClipboard() {
        return CLIPBOARD_PREFIX + getId();
    }

    /**
     * tries to retrieve waypoint id from clipboard
     * returns the id, or -1 if no waypoint (with internal prefix) found
     */
    public static int hasClipboardWaypoint() {
        final int length = CLIPBOARD_PREFIX.length();
        final String clip = StringUtils.defaultString(ClipboardUtils.getText());
        if (clip.length() > length && clip.substring(0, length).equals(CLIPBOARD_PREFIX)) {
            try {
                return Integer.parseInt(clip.substring(CLIPBOARD_PREFIX.length()));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
