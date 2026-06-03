package cgeo.geocaching.files.unifiedgpxparser;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Route;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of a single {@link UnifiedGPXParser#parse} call. One collection per
 * GPX content type, exposed as separate fields.
 */
public final class Result {

    /** Fully-parsed geocaches collected from {@code <wpt>} elements. */
    public final Collection<Geocache> waypoints = new ArrayList<>();

    /**
     * Logs collected from Groundspeak / TerraCaching extensions, keyed by the geocode
     * of the cache they belong to. Logs are not attached to the {@link Geocache} itself
     * because that's how today's parser hands them to {@code DataStore.saveLogs} at
     * persistence time.
     */
    public final Map<String, List<LogEntry>> logsByGeocode = new HashMap<>();

    /**
     * Child waypoints whose parent cache was not present in the same file.
     * Integration code is expected to resolve the parent (e.g. via DataStore) and
     * attach each waypoint there.
     */
    public final List<OrphanWaypoint> orphanWaypoints = new ArrayList<>();

    /** One {@link Route} (routable=true) per {@code <rte>} element. */
    public final List<Route> routes = new ArrayList<>();

    /** One {@link Route} (routable=false) per {@code <trk>} element. */
    public final List<Route> tracks = new ArrayList<>();

    public boolean isEmpty() {
        return waypoints.isEmpty() && routes.isEmpty() && tracks.isEmpty()
                && orphanWaypoints.isEmpty();
    }
}
