package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointParser;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.ClipboardUtils;

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
import org.apache.commons.lang3.tuple.ImmutablePair;

public class Waypoint implements IWaypoint {

    public static final String PREFIX_OWN = "OWN";
    public static final String CLIPBOARD_PREFIX = "c:geo:WAYPOINT:";

    private static final String WP_PREFIX_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int ORDER_UNDEFINED = -2;

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
     *
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
     *
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
     *
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
     * Detect coordinates in the given text and converts them to user defined waypoints.
     * Works by rule of thumb.
     *
     * @param text Text to parse for waypoints
     * @param namePrefix Prefix of the name of the waypoint
     * @return a collection of found waypoints
     */
    public static Collection<Waypoint> parseWaypoints(@NonNull final String text, @NonNull final String namePrefix) {
        final List<Waypoint> waypoints = new LinkedList<>();
        final Collection<ImmutablePair<Geopoint, Integer>> matches = GeopointParser.parseAll(text);

        int count = 1;
        for (final ImmutablePair<Geopoint, Integer> match : matches) {
            final Geopoint point = match.getLeft();
            final Integer start = match.getRight();
            final String name = namePrefix + " " + count;
            final String potentialWaypointType = text.substring(Math.max(0, start - 15));
            final Waypoint waypoint = new Waypoint(name, parseWaypointType(potentialWaypointType), true);
            waypoint.setCoords(point);
            waypoints.add(waypoint);
            count++;
        }

        return waypoints;
    }

    /**
     * Detect waypoint types in the personal note text. It works by rule of thumb only.
     */
    private static WaypointType parseWaypointType(final String input) {
        final String lowerInput = StringUtils.substring(input, 0, 20).toLowerCase(Locale.getDefault());
        for (final WaypointType wpType : WaypointType.values()) {
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
     * @param type
     *            type to create a new default name for
     *
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
