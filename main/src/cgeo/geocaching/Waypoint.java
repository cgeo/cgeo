package cgeo.geocaching;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Waypoint implements IWaypoint, Comparable<Waypoint> {

    public static final String PREFIX_OWN = "OWN";
    private static final int ORDER_UNDEFINED = -2;
    private int id = -1;
    private String geocode = "geocode";
    private WaypointType waypointType = WaypointType.WAYPOINT;
    private String prefix = "";
    private String lookup = "";
    private String name = "";
    private String latlon = "";
    private Geopoint coords = null;
    private String note = "";
    private int cachedOrder = ORDER_UNDEFINED;
    private boolean own = false;
    private boolean visited = false;
    // preliminary default for mdpi screens
    private static int VISITED_INSET = 7;

    public static void initializeScale() {
        // Calculate visited inset based on screen density
        VISITED_INSET = (int) (6.6f * cgeoapplication.getInstance().getResources().getDisplayMetrics().density + 0.5f);
    }

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

    public void setIcon(final Resources res, final TextView nameView) {
        Drawable icon;
        if (visited) {
            LayerDrawable ld = new LayerDrawable(new Drawable[] {
                    res.getDrawable(waypointType.markerId),
                    res.getDrawable(R.drawable.tick) });
            ld.setLayerInset(0, 0, 0, VISITED_INSET, VISITED_INSET);
            ld.setLayerInset(1, VISITED_INSET, VISITED_INSET, 0, 0);
            icon = ld;
        } else {
            icon = res.getDrawable(waypointType.markerId);
        }
        final Drawable fIcon = icon;
        nameView.setCompoundDrawablesWithIntrinsicBounds(fIcon, null, null, null);
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
        if (StringUtils.isBlank(latlon) || latlon.startsWith("?")) { // there are waypoints containing "???"
            latlon = old.latlon;
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
        final Map<String, Waypoint> newPrefixes = new HashMap<String, Waypoint>(newPoints.size());
        for (final Waypoint waypoint : newPoints) {
            newPrefixes.put(waypoint.getPrefix(), waypoint);
        }

        // Copy user modified details of the old waypoints over the new ones
        for (final Waypoint oldWaypoint : oldPoints) {
            final String prefix = oldWaypoint.getPrefix();
            if (newPrefixes.containsKey(prefix)) {
                newPrefixes.get(prefix).merge(oldWaypoint);
            } else if (oldWaypoint.isUserDefined() || forceMerge) {
                newPoints.add(oldWaypoint);
            }
        }
    }

    public boolean isUserDefined() {
        return own || WaypointType.OWN == waypointType;
    }

    public void setUserDefined() {
        own = true;
        setPrefix(PREFIX_OWN);
    }

    private int computeOrder() {
        switch (waypointType) {
            case PARKING:
                return -1;
            case TRAILHEAD:
                return 1;
            case STAGE: // puzzles and stages with same value
                return 2;
            case PUZZLE:
                return 2;
            case FINAL:
                return 3;
            case OWN:
                return 4;
            default:
                return 0;
        }
    }

    private int order() {
        if (cachedOrder == ORDER_UNDEFINED) {
            cachedOrder = computeOrder();
        }
        return cachedOrder;
    }

    @Override
    public int compareTo(Waypoint other) {
        return order() - other.order();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        cachedOrder = ORDER_UNDEFINED;
    }

    public String getUrl() {
        return "http://www.geocaching.com//seek/cache_details.aspx?wp=" + geocode;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getGeocode() {
        return geocode;
    }

    public void setGeocode(String geocode) {
        this.geocode = StringUtils.upperCase(geocode);
    }

    @Override
    public WaypointType getWaypointType() {
        return waypointType;
    }

    public String getLookup() {
        return lookup;
    }

    public void setLookup(String lookup) {
        this.lookup = lookup;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLatlon() {
        return latlon;
    }

    public void setLatlon(String latlon) {
        this.latlon = latlon;
    }

    @Override
    public Geopoint getCoords() {
        return coords;
    }

    public void setCoords(Geopoint coords) {
        this.coords = coords;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
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

    public void setVisited(boolean visited) {
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
        hash = hash ^ waypointType.markerId;
        return (int) hash;
    }
}
