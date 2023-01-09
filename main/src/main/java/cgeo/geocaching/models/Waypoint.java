package cgeo.geocaching.models;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.formulas.Value;
import cgeo.geocaching.utils.functions.Func1;
import static cgeo.geocaching.utils.Formatter.generateShortGeocode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class Waypoint implements IWaypoint {

    public static final String PREFIX_OWN = "OWN";
    public static final String CLIPBOARD_PREFIX = "c:geo:WAYPOINT:";

    private static final String WP_PREFIX_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int ORDER_UNDEFINED = -2;

    private int id = -1;
    private String geocode = "geocode";
    private Geocache parentCache = null;
    private WaypointType waypointType = WaypointType.WAYPOINT;
    @NonNull
    private String prefix = "";
    private String lookup = "";
    private String name = "";
    @Nullable
    private Geopoint coords = null;
    @NonNull
    private String note = "";
    private String userNote = "";
    private int cachedOrder = ORDER_UNDEFINED;
    private boolean own = false;

    private boolean visited = false;
    private boolean originalCoordsEmpty = false;

    private String calcStateConfig = null;

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

    public Waypoint(@NonNull final String lookup, @Nullable final Geopoint coords, @NonNull final String name, @NonNull final String prefix, @NonNull final String note, @NonNull final WaypointType type) {
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

        // keep note only for user-defined waypoints
        if (StringUtils.isBlank(note) && isUserDefined()) {
            note = old.note;
        }

        if (StringUtils.isBlank(userNote)) {
            userNote = old.userNote;
        }
        if (StringUtils.equals(note, userNote)) {
            userNote = "";
        }

        if (calcStateConfig == null) {
            calcStateConfig = old.calcStateConfig;
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

    /**
     * returns true if either this is a user defined waypoint or some data of
     * the waypoint was modified by the user
     */
    public boolean isUserModified() {
        return
                isUserDefined() ||
                        (isOriginalCoordsEmpty() && (getCoords() != null || getCalcStateConfig() != null)) ||
                        StringUtils.isNotBlank(getUserNote());
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

    @Nullable
    public Geocache getParentGeocache() {
        if (StringUtils.isNotBlank(geocode) && parentCache == null) {
            // lazy load - only load parent cache if needed to improve performance
            parentCache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        }

        return parentCache;
    }

    @NonNull
    public String getShortGeocode() {
        return generateShortGeocode(geocode);
    }

    public void setGeocode(final String geocode) {
        this.geocode = StringUtils.upperCase(geocode);
        this.parentCache = null;
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

    @NonNull
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

    public boolean isCalculated() {
        return CalculatedCoordinate.isValidConfig(calcStateConfig);
    }

    public void setCalculated(final CalculatedCoordinate cc, final Func1<String, Value> varMap) {
        this.setCalcStateConfig(cc.toConfig());
        this.setCoords(cc.calculateGeopoint(varMap));
    }

    public CalculatedCoordinate getCalculated() {
        return CalculatedCoordinate.createFromConfig(calcStateConfig);
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

        //calcState, only if coords are empty, otherwise mismatch between coords and formula can occur
        if (getCoords() == null && getCalcStateConfig() == null && parsedWaypoint.getCalcStateConfig() != null) {
            setCalcStateConfig(parsedWaypoint.getCalcStateConfig());
            changed = true;
        }

        //coordinates
        if (getCoords() == null && parsedWaypoint.getCoords() != null) {
            setCoords(parsedWaypoint.getCoords());
            changed = true;
        }

        //user note
        if (StringUtils.isBlank(this.getUserNote()) && StringUtils.isNotBlank(parsedWaypoint.getUserNote())) {
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

    public String getCalcStateConfig() {
        return calcStateConfig;
    }

    public void setCalcStateConfig(final String calcStateConfig) {
        this.calcStateConfig = calcStateConfig;
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
        final ArrayList<String> wpNamesLocalNumbering = new ArrayList<>();
        final ArrayList<String> wpNamesSharedNumbering = new ArrayList<>();
        for (final Waypoint waypoint : cache.getWaypoints()) {
            if (waypoint.getWaypointType() == WaypointType.FINAL || waypoint.getWaypointType() == WaypointType.PARKING) {
                wpNamesLocalNumbering.add(waypoint.getName());
            } else {
                wpNamesSharedNumbering.add(waypoint.getName());
            }
        }

        // try final and parking without index
        if (type == WaypointType.FINAL || type == WaypointType.PARKING) {
            // if it's the only one: don't add a number
            if (!wpNamesLocalNumbering.contains(type.getL10n()) && !wpNamesLocalNumbering.contains(type.getNameForNewWaypoint())) {
                return type.getNameForNewWaypoint();
            }
            // otherwise count up within its own wp type
            int max = 1; // start with one, as there as at least one other wp of this type when we are here
            for (int i = 0; i < 30; i++) {
                if (wpNamesLocalNumbering.contains(type.getL10n() + " " + i) || wpNamesLocalNumbering.contains(type.getNameForNewWaypoint() + " " + i)) {
                    max = i;
                }
            }
            return type.getNameForNewWaypoint() + " " + (max + 1);
        }

        // for other types add an index by default, which is highest found index (across all "shared numbering" types) + 1
        int max = 0;
        final Pattern p = Pattern.compile("(^|[-_ ])([0-9]+)([-_ ]|$)");
        for (String wpName : wpNamesSharedNumbering) {
            final MatcherWrapper match = new MatcherWrapper(p, wpName);
            while (match.find()) {
                try {
                    final int i = Integer.parseInt(match.group(2));
                    if (Math.abs(i) <= 10100 && i > max) {
                        max = i;
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return type.getNameForNewWaypoint() + " " + (max + 1);
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

    public boolean belongsToUserDefinedCache() {
        return InternalConnector.getInstance().canHandle(geocode);
    }

    public int getMapMarkerId() {
        return ConnectorFactory.getConnector(geocode).getCacheMapMarkerId();
    }

    public int getMapDotMarkerId() {
        return ConnectorFactory.getConnector(geocode).getCacheMapDotMarkerId();
    }

    public int getMapDotMarkerBackgroundId() {
        return ConnectorFactory.getConnector(geocode).getCacheMapDotMarkerBackgroundId();
    }

    public boolean applyDistanceRule() {
        boolean applyDistanceRule = getWaypointType().applyDistanceRule();
        if (applyDistanceRule) {
            final Geocache parentCache = getParentGeocache();
            if (parentCache != null) {
                applyDistanceRule = parentCache.mayApplyDistanceRule();
            }
        }
        return applyDistanceRule;
    }
}
