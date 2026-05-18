package cgeo.geocaching.files.unifiedgpxparser;

import androidx.annotation.Nullable;

/**
 * State that lives across multiple {@code <wpt>} elements within a single GPX file.
 * Owned by {@link UnifiedGPXParser} and threaded through each
 * {@link UnifiedGPXWaypointParser#parseWaypoint} call.
 */
final class ParseContext {

    /** URL of the script that generated the file, used to recognise quirks (e.g. extremcaching). */
    @Nullable String scriptUrl;

    /**
     * Set once we encounter a TerraCaching parent waypoint (identified by the
     * {@code GC_WayPoint1} sentinel in its short description). The next wpt is then
     * known to be a TerraCaching child.
     */
    boolean terraChildWaypoint;
}
