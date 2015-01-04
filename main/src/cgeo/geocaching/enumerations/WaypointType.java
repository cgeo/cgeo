package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

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

    @NonNull
    public final String id;
    public final int stringId;
    public final int markerId;

    WaypointType(@NonNull final String id, final int stringId, final int markerId) {
        this.id = id;
        this.stringId = stringId;
        this.markerId = markerId;
    }

    /**
     * inverse lookup of waypoint IDs<br/>
     * non public so that <code>null</code> handling can be handled centrally in the enum type itself
     */
    private static final Map<String, WaypointType> FIND_BY_ID = new HashMap();
    private static final Set<WaypointType> ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL_TMP = new HashSet<>();
    static {
        for (final WaypointType wt : values()) {
            FIND_BY_ID.put(wt.id, wt);
            if (wt != WaypointType.OWN && wt != WaypointType.ORIGINAL) {
                ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL_TMP.add(wt);
            }
        }
    }
    public static final Set<WaypointType> ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL = Collections.unmodifiableSet(ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL_TMP);

    /**
     * inverse lookup of waypoint IDs<br/>
     * here the <code>null</code> handling shall be done
     */
    @NonNull
    public static WaypointType findById(final String id) {
        if (null == id) {
            return WAYPOINT;
        }
        final WaypointType waypointType = FIND_BY_ID.get(id);
        if (null == waypointType) {
            return WAYPOINT;
        }
        return waypointType;
    }

    public final String getL10n() {
        return CgeoApplication.getInstance().getBaseContext().getResources().getString(stringId);
    }

    @Override
    public final String toString() {
        return getL10n();
    }

    public boolean applyDistanceRule() {
        return (this == FINAL || this == STAGE);
    }
}
