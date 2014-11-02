package cgeo.geocaching;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.MatcherWrapper;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class Waypoint implements IWaypoint {

    public static final String PREFIX_OWN = "OWN";
    private static final int ORDER_UNDEFINED = -2;
    private static final Pattern PATTERN_COORDS = Pattern.compile("\\b[nNsS]\\s*\\d");
    private int id = -1;
    private String geocode = "geocode";
    private WaypointType waypointType = WaypointType.WAYPOINT;
    private String prefix = "";
    private String lookup = "";
    private String name = "";
    private Geopoint coords = null;
    private String note = "";
    private int cachedOrder = ORDER_UNDEFINED;
    private boolean own = false;
    private boolean visited = false;

    /**
     * require name and type for every waypoint
     *
     * @param name
     * @param type
     */
    public Waypoint(final String name, final WaypointType type, final boolean own) {
        this.name = name;
        this.waypointType = type;
        this.own = own;
    }

    /**
     * copy constructor
     *
     * @param other
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
            setName(old.name);
        }
        if (coords == null) {
            coords = old.coords;
        }
        if (StringUtils.isBlank(note)) {
            note = old.note;
        }
        if (note != null && old.note != null) {
            if (old.note.length() > note.length()) {
                note = old.note;
            }
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
                newPrefixes.get(prefix).merge(oldWaypoint);
            } else if (oldWaypoint.isUserDefined() || forceMerge) {
                // personal note waypoints should always be taken from the new list only
                if (!isPersonalNoteWaypoint(oldWaypoint)) {
                    newPoints.add(oldWaypoint);
                }
            }
        }
    }

    private static boolean isPersonalNoteWaypoint(final @NonNull Waypoint waypoint) {
        return StringUtils.startsWith(waypoint.getName(), CgeoApplication.getInstance().getString(R.string.cache_personal_note) + " ");
    }

    public boolean isUserDefined() {
        return own || WaypointType.OWN == waypointType;
    }

    public void setUserDefined() {
        own = true;
        setPrefix(PREFIX_OWN);
    }

    private int computeOrder() {
        // first parking, then trailhead (as start of the journey)
        // puzzles, stages, waypoints can all be mixed
        // at last the final and the original coordinates of the final
        switch (waypointType) {
            case PARKING:
                return -1;
            case TRAILHEAD:
                return 1;
            case STAGE:
            case PUZZLE:
            case WAYPOINT:
                return 2;
            case FINAL:
                return 3;
            case ORIGINAL:
                return 4;
            case OWN:
                return 5;
        }
        return 0;
    }

    private int order() {
        if (cachedOrder == ORDER_UNDEFINED) {
            cachedOrder = computeOrder();
        }
        return cachedOrder;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
        cachedOrder = ORDER_UNDEFINED;
    }

    public String getUrl() {
        return "http://www.geocaching.com/seek/cache_details.aspx?wp=" + geocode;
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
        return WaypointType.FINAL == waypointType && null != coords;
    }

    @Override
    public String getCoordType() {
        return "waypoint";
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
     * Sort waypoints by their probable order (e.g. parking first, final last).
     */
    public static final Comparator<? super Waypoint> WAYPOINT_COMPARATOR = new Comparator<Waypoint>() {

        @Override
        public int compare(final Waypoint left, final Waypoint right) {
            return left.order() - right.order();
        }
    };

    /**
     * Delegates the creation of the waypoint-id for gpx-export to the waypoint
     *
     * @return
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
     * Detect coordinates in the personal note and convert them to user defined waypoints. Works by rule of thumb.
     *
     * @param initialNote Note content
     * @return a collection of found waypoints
     */
    public static Collection<Waypoint> parseWaypointsFromNote(@NonNull final String initialNote) {
        final List<Waypoint> waypoints = new LinkedList<>();

        String note = initialNote;
        MatcherWrapper matcher = new MatcherWrapper(PATTERN_COORDS, note);
        int count = 1;
        while (matcher.find()) {
            try {
                final Geopoint point = new Geopoint(note.substring(matcher.start()));
                // Coords must have non zero latitude and longitude and at least one part shall have fractional degrees.
                if (point.getLatitudeE6() != 0 && point.getLongitudeE6() != 0 &&
                        ((point.getLatitudeE6() % 1000) != 0 || (point.getLongitudeE6() % 1000) != 0)) {
                    final String name = CgeoApplication.getInstance().getString(R.string.cache_personal_note) + " " + count;
                    final String potentialWaypointType = note.substring(Math.max(0, matcher.start() - 15));
                    final Waypoint waypoint = new Waypoint(name, parseWaypointType(potentialWaypointType), false);
                    waypoint.setCoords(point);
                    waypoints.add(waypoint);
                    count++;
                }
            } catch (final Geopoint.ParseException ignored) {
            }

            note = note.substring(matcher.start() + 1);
            matcher = new MatcherWrapper(PATTERN_COORDS, note);
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


}
