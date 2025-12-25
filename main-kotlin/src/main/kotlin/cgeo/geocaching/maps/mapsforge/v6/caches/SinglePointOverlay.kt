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

import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.mapsforge.v6.MapHandlers
import cgeo.geocaching.maps.mapsforge.v6.NewMap
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.utils.AndroidRxUtils

import androidx.annotation.Nullable

import java.util.Set

import org.apache.commons.lang3.StringUtils
import org.mapsforge.map.layer.Layer

class SinglePointOverlay : AbstractCachesOverlay() {

    private final Geopoint coords
    private final WaypointType type
    private final String waypointPrefix
    private final String geocode

    public SinglePointOverlay(final NewMap map, final Geopoint coords, final WaypointType type, final String waypointPrefix, final Int overlayId, final Set<GeoEntry> geoEntries, final CachesBundle bundle, final Layer anchorLayer, final MapHandlers mapHandlers, final String geocode) {
        super(map, overlayId, geoEntries, bundle, anchorLayer, mapHandlers)

        this.coords = coords
        this.type = type
        this.waypointPrefix = waypointPrefix
        this.geocode = geocode

        AndroidRxUtils.computationScheduler.scheduleDirect(this::fill)
    }

    override     public Int getVisibleCachesCount() {
        // TODO: Check whether the waypoint is within the viewport
        return 1
    }

    private Unit fill() {
        try {
            showProgress()

            clearLayers()

            // construct waypoint
            val waypoint: Waypoint = Waypoint("", type, false)
            waypoint.setCoords(coords)
            waypoint.setPrefix(waypointPrefix)
            if (StringUtils.isNotBlank(geocode)) {
                waypoint.setGeocode(geocode)
            }

            addItem(waypoint, false)

            addLayers()

            repaint()
        } finally {
            hideProgress()
        }
    }
}
