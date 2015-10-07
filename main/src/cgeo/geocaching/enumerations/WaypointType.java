package cgeo.geocaching.enumerations;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import org.apache.commons.lang3.StringUtils;
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
    FINAL("flag", "Final Location", R.string.wp_final, R.drawable.waypoint_flag),
    OWN("own", "Own", R.string.wp_waypoint, R.drawable.waypoint_waypoint),
    PARKING("pkg", "Parking Area", R.string.wp_pkg, R.drawable.waypoint_pkg),
    PUZZLE("puzzle", "Virtual Stage", R.string.wp_puzzle, R.drawable.waypoint_puzzle),
    STAGE("stage", "Physical Stage", R.string.wp_stage, R.drawable.waypoint_stage),
    TRAILHEAD("trailhead", "Trailhead", R.string.wp_trailhead, R.drawable.waypoint_trailhead),
    WAYPOINT("waypoint", "Reference Point", R.string.wp_waypoint, R.drawable.waypoint_waypoint),
    ORIGINAL("original", "Original Coordinates", R.string.wp_original, R.drawable.waypoint_waypoint);

    @NonNull
    public final String id;

    @NonNull public final String gpx;
    public final int stringId;
    public final int markerId;

    WaypointType(@NonNull final String id, @NonNull final String gpx, final int stringId, final int markerId) {
        this.id = id;
        this.gpx = gpx;
        this.stringId = stringId;
        this.markerId = markerId;
    }

    /**
     * inverse lookup of waypoint IDs<br/>
     * non public so that <code>null</code> handling can be handled centrally in the enum type itself
     */
    private static final Map<String, WaypointType> FIND_BY_ID = new HashMap<>();
    private static final Set<WaypointType> ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL_TMP = new HashSet<>();
    static {
        for (final WaypointType wt : values()) {
            FIND_BY_ID.put(wt.id, wt);
            if (wt != OWN && wt != ORIGINAL) {
                ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL_TMP.add(wt);
            }
        }
    }
    @NonNull
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

    @NonNull
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

    public static WaypointType fromGPXString(@NonNull final String sym) {
        // first check the somewhat "official" sym types
        for (final WaypointType waypointType : WaypointType.values()) {
            if (waypointType.gpx.equalsIgnoreCase(sym)) {
                return waypointType;
            }
        }

        // old names of multi cache stages
        if ("stages of a multicache".equalsIgnoreCase(sym)) {
            return WaypointType.STAGE;
        }
        if ("stage of a multicache".equalsIgnoreCase(sym)) {
            return WaypointType.STAGE;
        }
        if ("question to answer".equalsIgnoreCase(sym)) {
            return WaypointType.PUZZLE;
        }

        // this is not fully correct, but lets also look for localized waypoint types
        for (final WaypointType waypointType : WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL) {
            final String localized = waypointType.getL10n();
            if (StringUtils.isNotEmpty(localized)) {
                if (localized.equalsIgnoreCase(sym)) {
                    return waypointType;
                }
            }
        }
        return WaypointType.WAYPOINT;
    }
}
