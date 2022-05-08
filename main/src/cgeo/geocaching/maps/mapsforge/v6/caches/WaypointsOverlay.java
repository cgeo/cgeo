package cgeo.geocaching.maps.mapsforge.v6.caches;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.maps.MapUtils;
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mapsforge.map.layer.Layer;

public class WaypointsOverlay extends AbstractCachesOverlay {
    public WaypointsOverlay(final NewMap map, final int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers) {
        super(map, overlayId, geoEntries, bundle, anchorLayer, mapHandlers);
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
            MapUtils.filter(filteredWaypoints, getFilterContext());
            waypoints.addAll(filteredWaypoints);
        }

        if (showStored) {
            final Set<Waypoint> waypointsInViewport = DataStore.loadWaypoints(getViewport());
            MapUtils.filter(waypointsInViewport, getFilterContext());
            waypoints.addAll(waypointsInViewport);
        }

        return waypoints;
    }

    protected void showWaypoints(final Collection<String> baseGeoCodes, final boolean showStored, final boolean checkOwnership, final boolean forceCompactIconMode) {
        final Collection<String> removeCodes = getGeocodes();
        final Collection<String> newCodes = new HashSet<>();
        final Set<Waypoint> waypoints = filterWaypoints(baseGeoCodes, showStored);

        for (final Waypoint waypoint : waypoints) {
            if (waypoint == null || waypoint.getCoords() == null || !waypoint.getCoords().isValid()) {
                continue;
            }
            if (removeCodes.contains(waypoint.getGpxId())) {
                removeCodes.remove(waypoint.getGpxId());
            } else {
                if (addItem(waypoint, forceCompactIconMode)) {
                    newCodes.add(waypoint.getGpxId());
                }
            }
        }

        syncLayers(removeCodes, newCodes);
    }

    /**
     * get waypoint IDs for geocodes and invalidate them
     *
     * @param geocodes the codes
     */
    public void invalidateWaypoints(final Collection<String> geocodes) {
        final Set<Geocache> baseCaches = DataStore.loadCaches(geocodes, LoadFlags.LOAD_WAYPOINTS);
        final Collection<String> invalidWpCodes = new ArrayList<>();
        for (final Geocache cache : baseCaches) {
            final List<Waypoint> wl = cache.getWaypoints();
            for (final Waypoint w : wl) {
                invalidWpCodes.add(w.getGpxId());
            }
        }
        invalidate(invalidWpCodes);
    }

}
