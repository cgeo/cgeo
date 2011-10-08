package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum listing waypoint types
 *
 * @author koem
 */
public enum WaypointType {
    FLAG("flag", R.string.wp_final, R.drawable.waypoint_flag, R.drawable.marker_waypoint_flag),
    OWN("own", R.string.wp_waypoint, R.drawable.waypoint_waypoint, R.drawable.marker_waypoint_waypoint),
    PKG("pkg", R.string.wp_pkg, R.drawable.waypoint_pkg, R.drawable.marker_waypoint_pkg),
    PUZZLE("puzzle", R.string.wp_puzzle, R.drawable.waypoint_puzzle, R.drawable.marker_waypoint_puzzle),
    STAGE("stage", R.string.wp_stage, R.drawable.waypoint_stage, R.drawable.marker_waypoint_stage),
    TRAILHEAD("trailhead", R.string.wp_trailhead, R.drawable.waypoint_trailhead, R.drawable.marker_waypoint_trailhead),
    WAYPOINT("waypoint", R.string.wp_waypoint, R.drawable.waypoint_waypoint, R.drawable.marker_waypoint_waypoint);

    public final String id;
    public final int stringId;
    public final int drawableId;
    public final int markerId;

    private WaypointType(String id, int stringId, int drawableId, int markerId) {
        this.id = id;
        this.stringId = stringId;
        this.drawableId = drawableId;
        this.markerId = markerId;
    }

    public static final Map<String, WaypointType> FIND_BY_ID;
    static {
        final HashMap<String, WaypointType> mapping = new HashMap<String, WaypointType>();
        for (WaypointType wt : values()) {
            mapping.put(wt.id, wt);
        }
        FIND_BY_ID = Collections.unmodifiableMap(mapping);
    }

}
