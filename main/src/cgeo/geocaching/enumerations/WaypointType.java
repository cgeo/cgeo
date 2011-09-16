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
    FLAG("flag", R.string.wp_final),
    OWN("own", R.string.wp_stage),
    PKG("pkg", R.string.wp_pkg),
    PUZZLE("puzzle", R.string.wp_puzzle),
    STAGE("stage", R.string.wp_stage),
    TRAILHEAD("trailhead", R.string.wp_trailhead),
    WAYPOINT("waypoint", R.string.wp_waypoint);

    public final String id;
    public final int stringId;

    private WaypointType(String id, int stringId) {
        this.id = id;
        this.stringId = stringId;
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
