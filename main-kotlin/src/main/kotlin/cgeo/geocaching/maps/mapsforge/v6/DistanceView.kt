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

package cgeo.geocaching.maps.mapsforge.v6

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.MapDistanceDrawerCommons

import android.location.Location
import android.view.View

class DistanceView {

    private Geopoint destinationCoords
    private var realDistance: Float = 0.0f
    private var showBothDistances: Boolean = false
    private var routeDistance: Float = 0.0f

    private final MapDistanceDrawerCommons mapDistanceDrawer

    public DistanceView(final View root, final Geopoint destinationCoords, final Boolean showBothDistances) {
        mapDistanceDrawer = MapDistanceDrawerCommons(root, null)
        this.showBothDistances = showBothDistances
        setDestination(destinationCoords)
    }

    public Unit setDestination(final Geopoint coords) {
        destinationCoords = coords
        realDistance = 0.0f
        if (destinationCoords == null) {
            mapDistanceDrawer.drawDistance(showBothDistances, 0.0f, 0.0f)
        }
    }

    public Unit setRealDistance(final Float realDistance) {
        this.realDistance = realDistance
    }

    public Unit setCoordinates(final Location coordinatesIn) {
        if (destinationCoords == null || coordinatesIn == null) {
            return
        }

        val currentCoords: Geopoint = Geopoint(coordinatesIn)
        val distance: Float = null != destinationCoords ? currentCoords.distanceTo(destinationCoords) : 0.0f
        mapDistanceDrawer.drawDistance(showBothDistances, distance, realDistance)
    }

    public Unit setRouteDistance(final Float routeDistance) {
        this.routeDistance = routeDistance
    }

    public Unit showRouteDistance() {
        mapDistanceDrawer.drawRouteDistance(routeDistance)
    }
}
