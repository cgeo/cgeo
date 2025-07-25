package cgeo.geocaching.models;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.settings.Settings;

import java.util.ArrayList;
import java.util.Arrays;

public class NavigationTargetRoute extends Route {

    float straightDistance = 0.0f;

    public NavigationTargetRoute() {
        super(false);
        setName(CgeoApplication.getInstance().getString(R.string.map_manual_target_route_name));
    }

    /**
     * Update the start and target points of the route.
     * This method performs heavy UI work, don't call it from the main thread!
     *
     * @param start the start point of the route
     * @param target the target point of the route
     */
    public void update(final Geopoint start, final Geopoint target) {
        final ArrayList<Float> elevation = new ArrayList<>();
        final Geopoint[] routingPoints = Routing.getTrack(start, target, elevation);

        if (elevation.isEmpty() && routingPoints.length > 2 && getNumSegments() > 0
                && routingPoints.length == segments.get(0).getPoints().size()) {
            // If the elevation is empty, we assume that the routing points are
            // already in the segment thus keep them.
            return;
        }

        segments.clear();
        final RouteSegment segment = new RouteSegment(new ArrayList<>(Arrays.asList(routingPoints)), elevation);
        segments.add(segment);

        straightDistance = start.distanceTo(target);
        distance = 0.0f;
        if (routingPoints.length > 2 || Settings.isMapDirection()) {
            distance = segment.calculateDistance();
        }
    }

    public float getStraightDistance() {
        return straightDistance;
    }

    public Geopoint getTarget() {
        if (getNumSegments() == 0) {
            return null;
        }
        final RouteSegment segment = segments.get(0);
        if (segment.getPoints().isEmpty()) {
            return null;
        }
        return segment.getPoints().get(segment.getPoints().size() - 1);
    }
}
