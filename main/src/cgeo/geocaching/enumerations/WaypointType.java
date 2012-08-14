package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Enum listing waypoint types
 */
public enum WaypointType {
    FINAL("flag", R.string.wp_final, R.drawable.waypoint_flag),
    OWN("own", R.string.wp_waypoint, R.drawable.waypoint_waypoint),
    PARKING("pkg", R.string.wp_pkg, R.drawable.waypoint_pkg),
    PUZZLE("puzzle", R.string.wp_puzzle, R.drawable.waypoint_puzzle),
    STAGE("stage", R.string.wp_stage, R.drawable.waypoint_stage),
    TRAILHEAD("trailhead", R.string.wp_trailhead, R.drawable.waypoint_trailhead),
    WAYPOINT("waypoint", R.string.wp_waypoint, R.drawable.waypoint_waypoint),
    ORIGINAL("original", R.string.wp_original, R.drawable.waypoint_waypoint);

    public final String id;
    public final int stringId;
    public final int markerId;

    private WaypointType(String id, int stringId, int markerId) {
        this.id = id;
        this.stringId = stringId;
        this.markerId = markerId;
    }

    /**
     * inverse lookup of waypoint IDs<br/>
     * non public so that <code>null</code> handling can be handled centrally in the enum type itself
     */
    private static final Map<String, WaypointType> FIND_BY_ID;
    public static final Set<WaypointType> ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL = new HashSet<WaypointType>();
    static {
        final HashMap<String, WaypointType> mapping = new HashMap<String, WaypointType>();
        for (WaypointType wt : values()) {
            mapping.put(wt.id, wt);
            if (wt != WaypointType.OWN && wt != WaypointType.ORIGINAL) {
                ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL.add(wt);
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
        return cgeoapplication.getInstance().getBaseContext().getResources().getString(stringId);
    }

    @Override
    public final String toString() {
        return getL10n();
    }
}
