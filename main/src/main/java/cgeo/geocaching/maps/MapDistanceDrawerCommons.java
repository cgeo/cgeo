package cgeo.geocaching.maps;

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

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.layers.UnifiedTargetAndDistancesHandler;

import android.view.View;
import android.widget.TextView;

public class MapDistanceDrawerCommons {
    private final TextView distanceStraight;
    private final TextView distanceRouted;
    private final TextView distanceIndividualRoute;
    private final TextView distanceSupersizeView;
    private final TextView targetView;
    private boolean bothViewsNeeded = false;

    private boolean showBothDistances = false;
    private float distance = 0.0f;
    private float realDistance = 0.0f;
    private float routeDistance = 0.0f;
    private final Runnable handleSwapNotification;

    public MapDistanceDrawerCommons(final View root, final Runnable handleSwapNotification) {
        distanceStraight = root.findViewById(R.id.distanceStraight);
        distanceRouted = root.findViewById(R.id.distanceRouted);
        distanceIndividualRoute = root.findViewById(R.id.distanceIndividualRoute);
        targetView = root.findViewById(R.id.target);
        distanceSupersizeView = root.findViewById(R.id.distanceSupersize);

        distanceStraight.setOnClickListener(v -> swap());
        distanceRouted.setOnClickListener(v -> swap());
        distanceIndividualRoute.setOnClickListener(v -> swap());
        distanceSupersizeView.setOnClickListener(v -> swap());

        this.handleSwapNotification = handleSwapNotification;
    }

    public void drawDistance(final boolean showBothDistances, final float distance, final float realDistance) {
        this.showBothDistances = showBothDistances;
        this.distance = distance;
        this.realDistance = realDistance;
        updateDistanceViews();
    }

    public void drawRouteDistance(final float routeDistance) {
        this.routeDistance = routeDistance;
        drawDistance(showBothDistances, distance, realDistance);
    }

    private void swap() {
        final int supersize = (Settings.getSupersizeDistance() + 1) % (bothViewsNeeded ? 3 : 2);
        Settings.setSupersizeDistance(supersize);
        updateDistanceViews();
    }

    private void updateDistanceViews() {
        // glue code to UnifiedMap
        UnifiedTargetAndDistancesHandler.updateDistanceViews(distance, realDistance, routeDistance, showBothDistances, distanceStraight, distanceRouted, distanceIndividualRoute, distanceSupersizeView, targetView, bvn -> bothViewsNeeded = bvn);
        if (handleSwapNotification != null) {
            handleSwapNotification.run();
        }
    }

}
