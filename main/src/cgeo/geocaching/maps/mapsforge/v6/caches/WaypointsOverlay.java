package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mapsforge.map.layer.Layer;

public class WaypointsOverlay extends AbstractCachesOverlay {
    public WaypointsOverlay(final int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers) {
        super(overlayId, geoEntries, bundle, anchorLayer, mapHandlers);
    }

    void hideWaypoints() {
        final Collection<String> removeCodes = getGeocodes();
        final Collection<String> newCodes = new HashSet<>();

        syncLayers(removeCodes, newCodes);
    }

    private Set<Waypoint> filterWaypoints(final Collection<String> baseGeoCodes, final boolean showStored) {
        final Set<Waypoint> waypoints = new HashSet<>();

        final Set<Geocache> baseCaches = DataStore.loadCaches(baseGeoCodes, LoadFlags.LOAD_WAYPOINTS);

        for (final Geocache cache : baseCaches) {
            final Set<Waypoint> filteredWaypoints = new HashSet<>(cache.getWaypoints());
            MapUtils.filter(filteredWaypoints, true);
            waypoints.addAll(filteredWaypoints);
        }

        if (showStored) {
            final boolean excludeMine = Settings.isExcludeMyCaches();
            final boolean excludeDisabled = Settings.isExcludeDisabledCaches();
            final boolean excludeArchived = Settings.isExcludeArchivedCaches();
            final CacheType type = Settings.getCacheType();

            final Set<Waypoint> waypointsInViewport = DataStore.loadWaypoints(getViewport(), excludeMine, excludeDisabled, excludeArchived, type);
            MapUtils.filter(waypointsInViewport, true);
            waypoints.addAll(waypointsInViewport);
        }

        return waypoints;
    }

    void showWaypoints(final Collection<String> baseGeoCodes, final boolean showStored) {
        final Collection<String> removeCodes = getGeocodes();
        final Collection<String> newCodes = new HashSet<>();

        final Set<Waypoint> waypoints = filterWaypoints(baseGeoCodes, showStored);
        final boolean isDotMode = Settings.isDotMode();

        for (final Waypoint waypoint : waypoints) {
            if (waypoint == null || waypoint.getCoords() == null) {
                continue;
            }
            if (removeCodes.contains(waypoint.getGpxId())) {
                removeCodes.remove(waypoint.getGpxId());
            } else {
                if (addItem(waypoint, isDotMode)) {
                    newCodes.add(waypoint.getGpxId());
                }
            }
        }

        syncLayers(removeCodes, newCodes);
    }

    /**
     * get waypoint IDs for geocodes and invalidate them
     * @param geocodes
     */
    public void invalidateWaypoints(final Collection<String> geocodes) {
        final Set<Geocache> baseCaches = DataStore.loadCaches(geocodes, LoadFlags.LOAD_WAYPOINTS);
        final Collection<String> invalidWpCodes = new ArrayList<String>();
        for (final Geocache cache : baseCaches) {
            final List<Waypoint> wl = cache.getWaypoints();
            for (final Waypoint w : wl) {
                invalidWpCodes.add(w.getGpxId());
            }
        }
        invalidate(invalidWpCodes);
    }

}
