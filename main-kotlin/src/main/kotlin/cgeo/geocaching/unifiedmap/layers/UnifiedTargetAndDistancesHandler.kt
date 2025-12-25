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

package cgeo.geocaching.unifiedmap.layers

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
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Units
import cgeo.geocaching.maps.routing.RoutingMode
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.utils.functions.Action1

import android.view.View
import android.widget.TextView

import org.apache.commons.lang3.StringUtils

class UnifiedTargetAndDistancesHandler {

    // navigation target info
    protected var targetGeocode: String = null
    protected var lastNavTarget: Geopoint = null

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

    private static val MIN_DISTANCE: Float = 0.0005f

    UnifiedTargetAndDistancesHandler(final View root) {
        distanceStraight = root.findViewById(R.id.distanceStraight)
        distanceRouted = root.findViewById(R.id.distanceRouted)
        distanceIndividualRoute = root.findViewById(R.id.distanceIndividualRoute)
        targetView = root.findViewById(R.id.target)
        distanceSupersizeView = root.findViewById(R.id.distanceSupersize)

        distanceStraight.setOnClickListener(v -> swap())
        distanceRouted.setOnClickListener(v -> swap())
        distanceIndividualRoute.setOnClickListener(v -> swap())
        distanceSupersizeView.setOnClickListener(v -> swap())
    }

    // distances handling -------------------------------------------------------------------------------------------

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
        updateDistanceViews(distance, realDistance, routeDistance, showBothDistances, distanceStraight, distanceRouted, distanceIndividualRoute, distanceSupersizeView, targetView, bvn -> bothViewsNeeded = bvn)
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public static Unit updateDistanceViews(
            final Float distance, final Float realDistance, final Float routeDistance, final Boolean showBothDistances,
            final TextView distanceStraight, final TextView distanceRouted, final TextView distanceIndividualRoute,
            final TextView distanceSupersizeView, final TextView targetView,
            final Action1<Boolean> updateBothViewNeeded
    ) {
        val routingMode: RoutingMode = Settings.getRoutingMode()
        val showStraight: Boolean = updateTextView(distanceStraight, showBothDistances || realDistance < MIN_DISTANCE || routingMode == RoutingMode.STRAIGHT, distance, RoutingMode.STRAIGHT.drawableId)
        val showRouted: Boolean = updateTextView(distanceRouted, routingMode != RoutingMode.STRAIGHT, realDistance, routingMode.drawableId)
        updateTextView(distanceIndividualRoute, true, routeDistance, R.drawable.map_quick_route)

        val supersize: Int = Settings.getSupersizeDistance()
        if (updateTextView(distanceSupersizeView, supersize > 0, supersize == 1 ? realDistance : supersize == 2 ? distance : routeDistance, supersize == 1 ? routingMode.drawableId : supersize == 2 ? RoutingMode.STRAIGHT.drawableId : R.drawable.map_quick_route)) {
            targetView.setBackground(null)
        } else {
            targetView.setBackgroundResource(R.drawable.icon_bcg)
        }

        updateBothViewNeeded.call(showStraight && showRouted)
    }

    private static Boolean updateTextView(final TextView tv, final Boolean show, final Float distance, final Int imgRes) {
        if (show && distance > MIN_DISTANCE) {
            TextParam.text(Units.getDistanceFromKilometers(distance)).setImage(ImageParam.id(imgRes), TextParam.IMAGE_SIZE_EQUAL_TEXT_SIZE).setImageTint(-1).applyTo(tv)
            tv.setVisibility(View.VISIBLE)
            return true
        } else {
            tv.setVisibility(View.GONE)
        }
        return false
    }

    // target handling ----------------------------------------------------------------------------------------------

    public Unit setTarget(final String name) {
        if (StringUtils.isNotEmpty(targetGeocode)) {
            targetView.setText(String.format("%s: %s", targetGeocode, name))
            targetView.setVisibility(View.VISIBLE)
        } else {
            targetView.setText("")
            targetView.setVisibility(View.GONE)
        }
    }

    public Unit setLastNavTarget(final Geopoint geopoint) {
        lastNavTarget = geopoint
    }

    public Unit setTargetGeocode(final String geocode) {
        targetGeocode = geocode
    }

}
