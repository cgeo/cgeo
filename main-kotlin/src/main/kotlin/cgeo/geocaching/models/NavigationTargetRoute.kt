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

package cgeo.geocaching.models

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.routing.Routing
import cgeo.geocaching.settings.Settings

import java.util.ArrayList
import java.util.Arrays

class NavigationTargetRoute : Route() {

    Float straightDistance = 0.0f

    public NavigationTargetRoute() {
        super(false)
        setName(CgeoApplication.getInstance().getString(R.string.map_manual_target_route_name))
    }

    /**
     * Update the start and target points of the route.
     * This method performs heavy UI work, don't call it from the main thread!
     *
     * @param start the start point of the route
     * @param target the target point of the route
     */
    public Unit update(final Geopoint start, final Geopoint target) {
        val elevation: ArrayList<Float> = ArrayList<>()
        final Geopoint[] routingPoints = Routing.getTrack(start, target, elevation)

        if (elevation.isEmpty() && routingPoints.length > 2 && getNumSegments() > 0
                && routingPoints.length == segments.get(0).getPoints().size()) {
            // If the elevation is empty, we assume that the routing points are
            // already in the segment thus keep them.
            return
        }

        segments.clear()
        val segment: RouteSegment = RouteSegment(ArrayList<>(Arrays.asList(routingPoints)), elevation)
        segments.add(segment)

        straightDistance = start.distanceTo(target)
        distance = 0.0f
        if (routingPoints.length > 2 || Settings.isMapDirection()) {
            distance = segment.calculateDistance()
        }
    }

    public Float getStraightDistance() {
        return straightDistance
    }

    public Geopoint getTarget() {
        if (getNumSegments() == 0) {
            return null
        }
        val segment: RouteSegment = segments.get(0)
        if (segment.getPoints().isEmpty()) {
            return null
        }
        return segment.getPoints().get(segment.getPoints().size() - 1)
    }
}
