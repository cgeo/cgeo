package cgeo.geocaching.files.unifiedgpxparser;

import cgeo.geocaching.models.Waypoint;

import androidx.annotation.NonNull;

/**
 * A child waypoint extracted from a {@code <wpt>} element together with the cache
 * geocode it belongs to. The parser does not have access to the parent cache during
 * the streaming pass; matching happens in {@link UnifiedGPXParser} after the full
 * gpx subtree has been read.
 */
final class ChildWaypoint {

    @NonNull final Waypoint waypoint;
    @NonNull final String parentGeocode;
    /** Raw wpt name; used by the post-parse step to compute the per-parent prefix. */
    @NonNull final String wptName;
    final boolean userDefined;
    /** If true the parent's {@code userModifiedCoords} flag is set on attach (ORIGINAL waypoint). */
    final boolean markParentUserModifiedCoords;

    ChildWaypoint(@NonNull final Waypoint waypoint, @NonNull final String parentGeocode,
                  @NonNull final String wptName, final boolean userDefined,
                  final boolean markParentUserModifiedCoords) {
        this.waypoint = waypoint;
        this.parentGeocode = parentGeocode;
        this.wptName = wptName;
        this.userDefined = userDefined;
        this.markParentUserModifiedCoords = markParentUserModifiedCoords;
    }
}
