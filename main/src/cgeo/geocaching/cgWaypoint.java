package cgeo.geocaching;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;

import android.content.res.Resources;
import android.widget.TextView;

import java.util.List;

public class cgWaypoint implements IWaypoint, Comparable<cgWaypoint> {

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

    /**
     * default constructor, no fields are set
     */
    public cgWaypoint() {
    }

    /**
     * copy constructor
     *
     * @param other
     */
    public cgWaypoint(final cgWaypoint other) {
        merge(other);
        id = 0;
    }

    public void setIcon(final Resources res, final TextView nameView) {
        nameView.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(waypointType.drawableId), null, null, null);
    }

    public void merge(final cgWaypoint old) {
        if (StringUtils.isBlank(getPrefix())) {
            setPrefix(old.getPrefix());
        }
        if (StringUtils.isBlank(lookup)) {
            lookup = old.lookup;
        }
        if (StringUtils.isBlank(name)) {
            setName(old.getName());
        }
        if (StringUtils.isBlank(latlon) || latlon.startsWith("?")) { // there are waypoints containing "???"
            latlon = old.latlon;
        }
        if (coords == null) {
            coords = old.getCoords();
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
            List<cgWaypoint> oldPoints) {
        // copy user modified details of the waypoints
        if (newPoints != null && oldPoints != null) {
            for (cgWaypoint old : oldPoints) {
                if (old != null) {
                    boolean merged = false;
                    if (old.getName() != null && old.getName().length() > 0) {
                        for (cgWaypoint waypoint : newPoints) {
                            if (waypoint != null && waypoint.getName() != null) {
                                if (old.getName().equalsIgnoreCase(waypoint.getName())) {
                                    waypoint.merge(old);
                                    merged = true;
                                    break;
                                }
                            }
                        }
                    }
                    // user added waypoints should also be in the new list
                    if (!merged && old.isUserDefined()) {
                        newPoints.add(old);
                    }
                }
            }
        }
    }

    public boolean isUserDefined() {
        return waypointType == WaypointType.OWN;
    }

    public void setUserDefined() {
        waypointType = WaypointType.OWN;
        setPrefix("OWN");
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

    public int getCachedOrder() {
        return cachedOrder;
    }

    public void setCachedOrder(int cachedOrder) {
        this.cachedOrder = cachedOrder;
    }

}