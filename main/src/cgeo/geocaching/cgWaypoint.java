package cgeo.geocaching;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;

import android.content.res.Resources;
import android.widget.TextView;

import java.util.List;

public class cgWaypoint implements IWaypoint, Comparable<cgWaypoint> {

    static final String PREFIX_OWN = "OWN";
    private static final int ORDER_UNDEFINED = -2;
    private int id = 0;
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

    /**
     * require name and type for every waypoint
     *
     * @param name
     * @param type
     */
    public cgWaypoint(final String name, final WaypointType type, final boolean own) {
        this.name = name;
        this.waypointType = type;
        this.own = own;
    }

    /**
     * copy constructor
     *
     * @param other
     */
    public cgWaypoint(final cgWaypoint other) {
        merge(other);
        this.waypointType = other.waypointType;
        id = 0;
    }

    public void setIcon(final Resources res, final TextView nameView) {
        nameView.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(waypointType.markerId), null, null, null);
    }

    public void merge(final cgWaypoint old) {
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
    }

    public static void mergeWayPoints(List<cgWaypoint> newPoints,
            List<cgWaypoint> oldPoints, boolean forceMerge) {
        // copy user modified details of the waypoints
        if (newPoints != null && oldPoints != null) {
            for (cgWaypoint old : oldPoints) {
                if (old != null) {
                    boolean merged = false;
                    if (old.name != null && old.name.length() > 0) {
                        for (cgWaypoint waypoint : newPoints) {
                            if (waypoint != null && waypoint.name != null) {
                                if (old.name.equalsIgnoreCase(waypoint.name)) {
                                    waypoint.merge(old);
                                    merged = true;
                                    break;
                                }
                            }
                        }
                    }
                    // user added waypoints should also be in the new list
                    if (!merged && (old.isUserDefined() || forceMerge)) {
                        newPoints.add(old);
                    }
                }
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
    public int compareTo(cgWaypoint other) {
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
        return "http://coord.info/" + geocode.toUpperCase();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGeocode() {
        return geocode;
    }

    public void setGeocode(String geocode) {
        this.geocode = geocode;
    }

    public WaypointType getWaypointType() {
        return waypointType;
    }

    public void setWaypointType(WaypointType type) {
        this.waypointType = type;
    }

    public String getLookup() {
        return lookup;
    }

    public void setLookup(String lookup) {
        this.lookup = lookup;
    }

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
}