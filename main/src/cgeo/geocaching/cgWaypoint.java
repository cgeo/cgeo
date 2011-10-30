package cgeo.geocaching;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.Geopoint;

import org.apache.commons.lang3.StringUtils;

import android.content.res.Resources;
import android.widget.TextView;

import java.util.List;

public class cgWaypoint implements Comparable<cgWaypoint> {
    public Integer id = 0;
    public String geocode = "geocode";
    public WaypointType type = WaypointType.WAYPOINT;
    private String prefix = "";
    public String lookup = "";
    public String name = "";
    public String latlon = "";
    public Geopoint coords = null;
    public String note = "";
    private Integer cachedOrder = null;

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
        nameView.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(type.drawableId), null, null, null);
    }

    public void merge(final cgWaypoint old) {
        if (StringUtils.isBlank(getPrefix())) {
            setPrefix(old.getPrefix());
        }
        if (StringUtils.isBlank(lookup)) {
            lookup = old.lookup;
        }
        if (StringUtils.isBlank(name)) {
            this.name = old.name;
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
            List<cgWaypoint> oldPoints) {
        // copy user modified details of the waypoints
        if (newPoints != null && oldPoints != null) {
            for (cgWaypoint old : oldPoints) {
                boolean merged = false;
                if (old != null && old.name != null && old.name.length() > 0) {
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
                if (!merged) {
                    newPoints.add(old);
                }
            }
        }
    }

    public boolean isUserDefined() {
        return type == WaypointType.OWN;
    }

    public void setUserDefined() {
        type = WaypointType.OWN;
        setPrefix("OWN");
    }

    private int computeOrder() {
        switch (type) {
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
        if (cachedOrder == null) {
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
        cachedOrder = null;
    }

    public String getUrl() {
        return "http://coord.info/" + geocode.toUpperCase();
    }
}