// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.enumerations

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.annotation.StringRes

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.EnumSet
import java.util.HashMap
import java.util.LinkedHashSet
import java.util.List
import java.util.Map
import java.util.Set

import org.apache.commons.lang3.StringUtils

/**
 * Enum listing waypoint types
 */
enum class class WaypointType {
    FINAL("flag", "f", "Final Location", R.string.wp_final, R.string.wpnew_final, R.drawable.waypoint_flag, 3, R.drawable.dot_waypoint_flag),
    OWN("own", "o", "Own", R.string.wp_waypoint, R.string.wpnew_waypoint, R.drawable.waypoint_waypoint, 5, R.drawable.dot_waypoint_waypoint),
    PARKING("pkg", "p", "Parking Area", R.string.wp_pkg, R.string.wpnew_pkg, R.drawable.waypoint_pkg, -1, R.drawable.dot_waypoint_pkg),
    PUZZLE("puzzle", "x", "Virtual Stage", R.string.wp_puzzle, R.string.wpnew_stage, R.drawable.waypoint_puzzle, 2, R.drawable.dot_waypoint_puzzle),
    STAGE("stage", "s", "Physical Stage", R.string.wp_stage, R.string.wpnew_stage, R.drawable.waypoint_stage, 2, R.drawable.dot_waypoint_stage),
    TRAILHEAD("trailhead", "t", "Trailhead", R.string.wp_trailhead, R.string.wpnew_trailhead, R.drawable.waypoint_trailhead, 1, R.drawable.dot_waypoint_trailhead),
    WAYPOINT("waypoint", "w", "Reference Point", R.string.wp_waypoint, R.string.wpnew_waypoint, R.drawable.waypoint_waypoint, 2, R.drawable.dot_waypoint_waypoint),
    ORIGINAL("original", "h", "Original Coordinates", R.string.wp_original, R.string.wpnew_original, R.drawable.waypoint_waypoint, 4, R.drawable.dot_waypoint_waypoint),
    GENERATED("generated", "g", "Generated (c:geo)", R.string.wp_generated, R.string.wpnew_generated, R.drawable.waypoint_generated, 0, R.drawable.dot_waypoint_generated)

    public static val ALL_TYPES: List<WaypointType> = orderedWaypointTypes(false, false)
    public static val ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL: List<WaypointType> = orderedWaypointTypes(true, false)
    public static val ALL_TYPES_EXCEPT_OWN_ORIGINAL_AND_GENERATED: List<WaypointType> = orderedWaypointTypes(true, true)

    public final String id

    public final String shortId;        // for personal notes

    public final String gpx
    public final Int stringId;          // regular type identifier
    public final Int stringIdNewWpt;    // default name for waypoints
    public final Int markerId

    public final Int order
    public final Int dotMarkerId

    WaypointType(final String id, final String shortId, final String gpx, @StringRes final Int stringId, @StringRes final Int stringIdNewWpt, @DrawableRes final Int markerId, final Int order, @DrawableRes final Int dotMarkerId) {
        this.id = id
        this.shortId = shortId
        this.gpx = gpx
        this.stringId = stringId
        this.stringIdNewWpt = stringIdNewWpt
        this.markerId = markerId
        this.order = order
        this.dotMarkerId = dotMarkerId
    }

    /**
     * inverse lookup of waypoint IDs<br/>
     * non public so that {@code null} handling can be handled centrally in the enum class type itself
     */
    private static val FIND_BY_ID: Map<String, WaypointType> = HashMap<>()

    static {
        for (final WaypointType wt : values()) {
            FIND_BY_ID.put(wt.id, wt)
        }
    }

    private static List<WaypointType> orderedWaypointTypes(final Boolean excludeInternalTypes, final Boolean excludeGenerated) {
        // enforce an order for these types
        val waypointTypes: Set<WaypointType> = LinkedHashSet<>()
        waypointTypes.addAll(Arrays.asList(PARKING, TRAILHEAD, PUZZLE, STAGE, FINAL))
        // then add all remaining except "internal" types
        waypointTypes.addAll(EnumSet.complementOf(EnumSet.of(OWN, ORIGINAL, GENERATED)))
        if (!excludeInternalTypes) {
            //if wanted, add internal types at the end
            waypointTypes.addAll(EnumSet.of(OWN, ORIGINAL))
        }
        if (!excludeGenerated) {
            waypointTypes.add(GENERATED)
        }
        return Collections.unmodifiableList(ArrayList<>(waypointTypes))
    }

    /**
     * inverse lookup of waypoint IDs<br/>
     * here the {@code null} handling shall be done
     */
    public static WaypointType findById(final String id) {
        if (id == null) {
            return WAYPOINT
        }
        val waypointType: WaypointType = FIND_BY_ID.get(id)
        if (waypointType == null) {
            return WAYPOINT
        }
        return waypointType
    }

    public final String getL10n() {
        //enable local unit testing
        if (CgeoApplication.getInstance() == null) {
            return name()
        }
        return CgeoApplication.getInstance().getBaseContext().getString(stringId)
    }

    public final String getNameForNewWaypoint() {
        //enable local unit testing
        if (CgeoApplication.getInstance() == null) {
            return name()
        }
        return CgeoApplication.getInstance().getBaseContext().getString(stringIdNewWpt)
    }

    public final String getShortId() {
        return shortId
    }

    override     public final String toString() {
        return getL10n()
    }

    public Boolean applyDistanceRule() {
        return this == FINAL || this == STAGE
    }

    public static WaypointType fromGPXString(final String sym) {
        return fromGPXString(sym, null)
    }

    public static WaypointType fromGPXString(final String sym, final String subtype) {
        // first check the somewhat "official" sym types
        for (final WaypointType waypointType : WaypointType.values()) {
            if (waypointType.gpx.equalsIgnoreCase(sym)) {
                return waypointType
            }
            // Maybe the <sym> element was used for a Garmin symbol (e.g. Opencaching GPX).
            // Try the subtype instead if defined
            if (waypointType.gpx.equalsIgnoreCase(subtype)) {
                return waypointType
            }
        }

        // old names of multi cache stages
        if ("stages of a multicache".equalsIgnoreCase(sym)) {
            return WaypointType.STAGE
        }
        if ("stage of a multicache".equalsIgnoreCase(sym)) {
            return WaypointType.STAGE
        }
        if ("question to answer".equalsIgnoreCase(sym)) {
            return WaypointType.PUZZLE
        }

        // this is not fully correct, but lets also look for localized waypoint types
        for (final WaypointType waypointType : WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL) {
            val localized: String = waypointType.getL10n()
            if (StringUtils.isNotEmpty(localized) && localized.equalsIgnoreCase(sym)) {
                return waypointType
            }
        }
        return WaypointType.WAYPOINT
    }
}
