package cgeo.geocaching.files.unifiedgpxparser;

import cgeo.geocaching.models.Waypoint;

import androidx.annotation.NonNull;

/**
 * A child waypoint whose parent cache was not present in the same GPX file.
 * <p>
 * Returned via {@link Result#orphanWaypoints}. Integration code is expected to
 * resolve the parent (e.g. via {@code DataStore.loadCache}) and attach the
 * waypoint there.
 */
public final class OrphanWaypoint {

    @NonNull public final Waypoint waypoint;
    @NonNull public final String parentGeocode;

    OrphanWaypoint(@NonNull final Waypoint waypoint, @NonNull final String parentGeocode) {
        this.waypoint = waypoint;
        this.parentGeocode = parentGeocode;
    }
}
