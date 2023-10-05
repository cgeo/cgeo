package cgeo.geocaching.maps;

/*
 * Standard view is: (at the top of the map view)
 * target      distances1
 * supersize
 *             distances2
 *
 * - target is only used when in target navigation (opened via cache/waypoint popup)
 * - distances1/distances2 placeholder will be filled with straight distance, routed distance,
 *   individual route length and elevation info (depending on settings and current data)
 * - By tapping on any of the distance fields either straight or routed distance can be supersized.
 *   It gets removed from the distance view containers and displayed in a larger font
 * - Tapping on the supersized window toggles between real distance supersized, straight distance
 *   supersized (depending on availability) and no supersize
 */

import cgeo.geocaching.R;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.unifiedmap.layers.UnifiedTargetAndDistancesHandler;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MapDistanceDrawerCommons {
    private static final String STRAIGHT_LINE_SYMBOL = Character.toString((char) 0x007C);
    private static final String WAVY_LINE_SYMBOL = Character.toString((char) 0x2307);

    private final LinearLayout distances1;
    private final LinearLayout distances2;
    private final TextView distanceSupersizeView;
    private final TextView targetView;
    private boolean bothViewsNeeded = false;

    private boolean showBothDistances = false;
    private float distance = 0.0f;
    private float realDistance = 0.0f;
    private float routeDistance = 0.0f;

    private String realDistanceInfo = "";
    private String distanceInfo = "";
    private String routingInfo = "";
    private String elevationInfo = "";

    public MapDistanceDrawerCommons(final View root) {
        distances1 = root.findViewById(R.id.distances1);
        distances2 = root.findViewById(R.id.distances2);
        targetView = root.findViewById(R.id.target);
        distanceSupersizeView = root.findViewById(R.id.distanceSupersize);

        distances1.setOnClickListener(v -> swap());
        distances2.setOnClickListener(v -> swap());
        distanceSupersizeView.setOnClickListener(v -> swap());
    }

    public void drawDistance(final boolean showBothDistances, final float distance, final float realDistance) {
        final boolean routingModeStraight = Settings.getRoutingMode() == RoutingMode.STRAIGHT;
        this.showBothDistances = showBothDistances;
        this.distance = distance;
        this.realDistance = realDistance;
        final boolean showRealDistance = realDistance > 0.0f && distance != realDistance && !routingModeStraight;
        bothViewsNeeded = showBothDistances && showRealDistance;

        realDistanceInfo = showRealDistance ? (showBothDistances ? WAVY_LINE_SYMBOL + " " : "") + Units.getDistanceFromKilometers(realDistance) : "";
        distanceInfo = (showBothDistances || routingModeStraight) && distance > 0.0f ? (showBothDistances ? STRAIGHT_LINE_SYMBOL + " " : "") + Units.getDistanceFromKilometers(distance) : "";
        routingInfo = routeDistance > 0.0f ? Units.getDistanceFromKilometers(routeDistance) : "";
        updateDistanceViews();
    }

    public void drawElevation(final float elevationFromRouting, final float elevationFromGNSS) {
        elevationInfo = UnifiedTargetAndDistancesHandler.buildElevationInfo(elevationFromRouting, elevationFromGNSS);
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
        UnifiedTargetAndDistancesHandler.updateDistanceViews(distanceInfo, realDistanceInfo, routingInfo, elevationInfo, distances1, distances2, distanceSupersizeView, targetView);
    }

}
