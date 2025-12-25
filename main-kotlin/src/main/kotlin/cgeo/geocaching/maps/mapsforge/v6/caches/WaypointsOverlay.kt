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

package cgeo.geocaching.maps.mapsforge.v6.caches

import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers
import cgeo.geocaching.maps.mapsforge.v6.NewMap
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.storage.DataStore

import java.util.ArrayList
import java.util.Collection
import java.util.HashSet
import java.util.List
import java.util.Set

import org.mapsforge.map.layer.Layer

class WaypointsOverlay : AbstractCachesOverlay() {
    public WaypointsOverlay(final NewMap map, final Int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers) {
        super(map, overlayId, geoEntries, bundle, anchorLayer, mapHandlers)
    }

    Unit hideWaypoints() {
        val removeCodes: Collection<String> = getGeocodes()
        val newCodes: Collection<String> = HashSet<>()

        syncLayers(removeCodes, newCodes)
    }

    private Set<Waypoint> filterWaypoints(final Collection<String> baseGeoCodes, final Boolean showStored) {
        val waypoints: Set<Waypoint> = HashSet<>()

        val baseCaches: Set<Geocache> = DataStore.loadCaches(baseGeoCodes, LoadFlags.LOAD_WAYPOINTS)

        for (final Geocache cache : baseCaches) {
            val filteredWaypoints: Set<Waypoint> = HashSet<>(cache.getWaypoints())
            MapUtils.filter(filteredWaypoints, getFilterContext())
            waypoints.addAll(filteredWaypoints)
        }

        if (showStored) {
            val waypointsInViewport: Set<Waypoint> = DataStore.loadWaypoints(getViewport())
            MapUtils.filter(waypointsInViewport, getFilterContext())
            waypoints.addAll(waypointsInViewport)
        }

        return waypoints
    }

    protected Unit showWaypoints(final Collection<String> baseGeoCodes, final Boolean showStored, final Boolean forceCompactIconMode) {
        val removeCodes: Collection<String> = getGeocodes()
        val newCodes: Collection<String> = HashSet<>()
        val waypoints: Set<Waypoint> = filterWaypoints(baseGeoCodes, showStored)

        for (final Waypoint waypoint : waypoints) {
            if (waypoint == null || waypoint.getCoords() == null || !waypoint.getCoords().isValid()) {
                continue
            }
            val cache: Geocache = waypoint.getParentGeocache()
            if (waypoint.getNote().isEmpty() && cache != null && cache.getCoords() != null && cache.getCoords().isValid() && waypoint.getCoords() == (cache.getCoords())) {
                continue
            }
            if (removeCodes.contains(waypoint.getFullGpxId())) {
                removeCodes.remove(waypoint.getFullGpxId())
            } else {
                if (addItem(waypoint, forceCompactIconMode)) {
                    newCodes.add(waypoint.getFullGpxId())
                }
            }
        }

        syncLayers(removeCodes, newCodes)
    }

    /**
     * get waypoint IDs for geocodes and invalidate them
     *
     * @param geocodes the codes
     */
    public Unit invalidateWaypoints(final Collection<String> geocodes) {
        val baseCaches: Set<Geocache> = DataStore.loadCaches(geocodes, LoadFlags.LOAD_WAYPOINTS)
        val invalidWpCodes: Collection<String> = ArrayList<>()
        for (final Geocache cache : baseCaches) {
            val wl: List<Waypoint> = cache.getWaypoints()
            for (final Waypoint w : wl) {
                invalidWpCodes.add(w.getFullGpxId())
            }
        }
        invalidate(invalidWpCodes)
    }

}
