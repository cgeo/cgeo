package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum listing waypoint types
 *
 * @author koem
 */
public enum WaypointType {
    FINAL("flag", R.string.wp_final, R.drawable.waypoint_flag),
    OWN("own", R.string.wp_waypoint, R.drawable.waypoint_waypoint),
    PARKING("pkg", R.string.wp_pkg, R.drawable.waypoint_pkg),
    PUZZLE("puzzle", R.string.wp_puzzle, R.drawable.waypoint_puzzle),
    STAGE("stage", R.string.wp_stage, R.drawable.waypoint_stage),
    TRAILHEAD("trailhead", R.string.wp_trailhead, R.drawable.waypoint_trailhead),
    WAYPOINT("waypoint", R.string.wp_waypoint, R.drawable.waypoint_waypoint);

    public final String id;
    public final int stringId;
    private String l10n; // not final because the locale can be changed
    public final int markerId;

    private WaypointType(String id, int stringId, int markerId) {
        this.id = id;
        this.stringId = stringId;
        setL10n();
        this.markerId = markerId;
    }

    /**
     * inverse lookup of waypoint IDs<br/>
     * non public so that <code>null</code> handling can be handled centrally in the enum type itself
     */
    private static final Map<String, WaypointType> FIND_BY_ID;
    public static final Map<WaypointType, String> ALL_TYPES_EXCEPT_OWN = new HashMap<WaypointType, String>();
    static {
        final HashMap<String, WaypointType> mapping = new HashMap<String, WaypointType>();
        for (WaypointType wt : values()) {
            mapping.put(wt.id, wt);
            if (wt != WaypointType.OWN) {
                ALL_TYPES_EXCEPT_OWN.put(wt, wt.getL10n());
            }
        }
        FIND_BY_ID = Collections.unmodifiableMap(mapping);
    }

    /**
     * inverse lookup of waypoint IDs<br/>
     * here the <code>null</code> handling shall be done
     */
    public static WaypointType findById(final String id) {
        if (null == id) {
            return WAYPOINT;
        }
        WaypointType waypointType = FIND_BY_ID.get(id);
        if (null == waypointType) {
            return WAYPOINT;
        }
        return waypointType;
    }

    public final String getL10n() {
        return l10n;
    }

    public void setL10n() {
        this.l10n = cgeoapplication.getInstance().getBaseContext().getResources().getString(this.stringId);
        if (WaypointType.ALL_TYPES_EXCEPT_OWN != null && WaypointType.ALL_TYPES_EXCEPT_OWN.containsKey(this)) {
            WaypointType.ALL_TYPES_EXCEPT_OWN.put(this, this.getL10n());
        }
    }

}
