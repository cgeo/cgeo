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

/*
 * Manages distance and target views
 *
 * Standard view is: (at the top of the map view)
 * target
 * supersize
 *             distances
 *
 * - target is only used when in target navigation (opened via cache/waypoint popup)
 * - distances placeholder will be filled with straight distance, routed distance
 *   and individual route length (depending on settings and current data)
 * - Tapping on any of the distance fields toggles between real distance supersized, straight distance
 *   supersized (depending on availability) and no supersize
 */

import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.unifiedmap.layers.UnifiedTargetAndDistancesHandler

import android.view.View
import android.widget.TextView

class MapDistanceDrawerCommons {
    private final TextView distanceStraight
    private final TextView distanceRouted
    private final TextView distanceIndividualRoute
    private final TextView distanceSupersizeView
    private final TextView targetView
    private var bothViewsNeeded: Boolean = false

    private var showBothDistances: Boolean = false
    private var distance: Float = 0.0f
    private var realDistance: Float = 0.0f
    private var routeDistance: Float = 0.0f
    private final Runnable handleSwapNotification

    public MapDistanceDrawerCommons(final View root, final Runnable handleSwapNotification) {
        distanceStraight = root.findViewById(R.id.distanceStraight)
        distanceRouted = root.findViewById(R.id.distanceRouted)
        distanceIndividualRoute = root.findViewById(R.id.distanceIndividualRoute)
        targetView = root.findViewById(R.id.target)
        distanceSupersizeView = root.findViewById(R.id.distanceSupersize)

        distanceStraight.setOnClickListener(v -> swap())
        distanceRouted.setOnClickListener(v -> swap())
        distanceIndividualRoute.setOnClickListener(v -> swap())
        distanceSupersizeView.setOnClickListener(v -> swap())

        this.handleSwapNotification = handleSwapNotification
    }

    public Unit drawDistance(final Boolean showBothDistances, final Float distance, final Float realDistance) {
        this.showBothDistances = showBothDistances
        this.distance = distance
        this.realDistance = realDistance
        updateDistanceViews()
    }

    public Unit drawRouteDistance(final Float routeDistance) {
        this.routeDistance = routeDistance
        drawDistance(showBothDistances, distance, realDistance)
    }

    private Unit swap() {
        val supersize: Int = (Settings.getSupersizeDistance() + 1) % (bothViewsNeeded ? 3 : 2)
        Settings.setSupersizeDistance(supersize)
        updateDistanceViews()
    }

    private Unit updateDistanceViews() {
        // glue code to UnifiedMap
        UnifiedTargetAndDistancesHandler.updateDistanceViews(distance, realDistance, routeDistance, showBothDistances, distanceStraight, distanceRouted, distanceIndividualRoute, distanceSupersizeView, targetView, bvn -> bothViewsNeeded = bvn)
        if (handleSwapNotification != null) {
            handleSwapNotification.run()
        }
    }

}
