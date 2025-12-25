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

package cgeo.geocaching.maps

import cgeo.geocaching.location.Geopoint

import android.location.Location
import android.view.View

class DistanceDrawer {
    private final Geopoint destinationCoords

    private var distance: Float = 0.0f
    private var realDistance: Float = 0.0f
    private var showBothDistances: Boolean = false
    private var routeDistance: Float = 0.0f

    private final MapDistanceDrawerCommons mapDistanceDrawer

    public DistanceDrawer(final View root, final Geopoint destinationCoords, final Boolean showBothDistances, final Runnable handleSwapNotification) {
        this.destinationCoords = destinationCoords
        this.showBothDistances = showBothDistances
        mapDistanceDrawer = MapDistanceDrawerCommons(root, handleSwapNotification)
    }

    public Unit setCoordinates(final Location location) {
        if (destinationCoords == null || location == null) {
            distance = 0.0f
        } else {
            distance = Geopoint(location).distanceTo(destinationCoords)
        }
    }

    public Geopoint getDestinationCoords() {
        return destinationCoords
    }

    public Unit setRealDistance(final Float realDistance) {
        this.realDistance = realDistance
    }

    public Unit setRouteDistance(final Float routeDistance) {
        this.routeDistance = routeDistance
    }

    public Unit drawDistance() {
        mapDistanceDrawer.drawDistance(showBothDistances, distance, realDistance)
        mapDistanceDrawer.drawRouteDistance(routeDistance)
    }
}
