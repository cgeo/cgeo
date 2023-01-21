package cgeo.geocaching.unifiedmap;

/*
 * Manages distance and target views
 *
 * Standard view is: (at the top of the map view)
 * target           distance1
 *                  distance2
 *                  distance3
 *
 * - target is only used when in target navigation (opened via cache/waypoint popup)
 * - distance1, distance2, distance will be filled with straight distance, routed distance,
 *   individual route length (depending on settings and current data)
 * - By tapping on any of the distance fields either straight or routed distance can be supersized.
 *   It gets removed from the three distance fields and displayed in a larger font:
 *
 * target           distance1
 *     supersize    distance2
 *
 * - Tapping on the supersized window toggles between distance1 supersized, distance 2 supersized
 *   (depending on availability) and no supersize
 */


import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.settings.Settings;

import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

class UnifiedTargetAndDistancesHandler {

    // navigation target info
    protected String targetGeocode = null;
    protected Geopoint lastNavTarget = null;

    private static final String STRAIGHT_LINE_SYMBOL = Character.toString((char) 0x007C);
    private static final String WAVY_LINE_SYMBOL = Character.toString((char) 0x2307);

    private final TextView distance1View;
    private final TextView distance1ViewSupersize;
    private final TextView distance2View;
    private final TextView distance2ViewSupersize;
    private final TextView distance3View;
    private final TextView targetView;
    private final TextView targetViewSupersize;
    private final TextView distanceSupersizeView;
    private boolean bothViewsNeeded = false;

    private boolean showBothDistances = false;
    private float distance = 0.0f;
    private float realDistance = 0.0f;
    private float routeDistance = 0.0f;

    UnifiedTargetAndDistancesHandler(final View root) {
        distance1View = root.findViewById(R.id.distance1);
        distance1ViewSupersize = root.findViewById(R.id.distance1Supersize);
        distance2View = root.findViewById(R.id.distance2);
        distance2ViewSupersize = root.findViewById(R.id.distance2Supersize);
        distance3View = root.findViewById(R.id.distance3);
        targetView = root.findViewById(R.id.target);
        targetViewSupersize = root.findViewById(R.id.targetSupersize);
        distanceSupersizeView = root.findViewById(R.id.distanceSupersize);

        distance1View.setOnClickListener(v -> swap());
        distance2View.setOnClickListener(v -> swap());
        distance3View.setOnClickListener(v -> swap());
        distanceSupersizeView.setOnClickListener(v -> swap());
    }

    // distances handling -------------------------------------------------------------------------------------------

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public void drawDistance(final boolean showBothDistances, final float distance, final float realDistance) {
        this.showBothDistances = showBothDistances;
        this.distance = distance;
        this.realDistance = realDistance;

        final boolean showRealDistance = realDistance != 0.0f && distance != realDistance && Settings.getRoutingMode() != RoutingMode.STRAIGHT;
        bothViewsNeeded = showBothDistances && showRealDistance;
        final int supersize = distance == 0 ? 0 : Settings.getSupersizeDistance() % (bothViewsNeeded ? 3 : 2);
        final String[] values = {"", "", ""};
        String superValue = "";
        int current = 0;

        // collect data to be displayed
        if (bothViewsNeeded) {
            if (supersize == 1) {
                superValue = STRAIGHT_LINE_SYMBOL + " " + Units.getDistanceFromKilometers(distance);
                values[current++] = WAVY_LINE_SYMBOL + " " + Units.getDistanceFromKilometers(realDistance);
            } else if (supersize == 2) {
                values[current++] = STRAIGHT_LINE_SYMBOL + " " + Units.getDistanceFromKilometers(distance);
                superValue = WAVY_LINE_SYMBOL + " " + Units.getDistanceFromKilometers(realDistance);
            } else {
                values[current++] = STRAIGHT_LINE_SYMBOL + " " + Units.getDistanceFromKilometers(distance);
                values[current++] = WAVY_LINE_SYMBOL + " " + Units.getDistanceFromKilometers(realDistance);
            }
        } else if (distance != 0.0f) {
            if (supersize > 0) {
                superValue = Units.getDistanceFromKilometers(showRealDistance ? realDistance : distance);
            } else {
                values[current++] = Units.getDistanceFromKilometers(showRealDistance ? realDistance : distance);
            }
        }
        if (routeDistance != 0.0f) {
            values[current] = Units.getDistanceFromKilometers(routeDistance);
        }

        // adjust visibility and fill views
        if (targetView != null) {
            final boolean targetIsSet = StringUtils.isNotBlank(targetView.getText());
            targetView.setVisibility(supersize > 0 || !targetIsSet ? View.GONE : View.VISIBLE);
            targetViewSupersize.setVisibility(supersize > 0 && targetIsSet ? View.VISIBLE : View.GONE);
            targetViewSupersize.setText(targetView.getText());
        }
        if (supersize > 0) {
            distanceSupersizeView.setVisibility(View.VISIBLE);
            distanceSupersizeView.setText(superValue);
            distance1View.setVisibility(View.GONE);
            distance1ViewSupersize.setVisibility(StringUtils.isNotBlank(values[0]) ? View.VISIBLE : View.GONE);
            distance1ViewSupersize.setText(values[0]);
            distance2View.setVisibility(View.GONE);
            distance2ViewSupersize.setVisibility(StringUtils.isNotBlank(values[1]) ? View.VISIBLE : View.GONE);
            distance2ViewSupersize.setText(values[1]);
            distance3View.setVisibility(View.GONE);
        } else {
            distanceSupersizeView.setVisibility(View.GONE);
            distance1View.setVisibility(StringUtils.isNotBlank(values[0]) ? View.VISIBLE : View.GONE);
            distance1View.setText(values[0]);
            distance1ViewSupersize.setVisibility(View.GONE);
            distance2View.setVisibility(StringUtils.isNotBlank(values[1]) ? View.VISIBLE : View.GONE);
            distance2View.setText(values[1]);
            distance2ViewSupersize.setVisibility(View.GONE);
            distance3View.setVisibility(StringUtils.isNotBlank(values[2]) ? View.VISIBLE : View.GONE);
            distance3View.setText(values[2]);
        }
    }

    public void drawRouteDistance(final float routeDistance) {
        this.routeDistance = routeDistance;
        drawDistance(showBothDistances, distance, realDistance);
    }

    private void swap() {
        final int supersize = (Settings.getSupersizeDistance() + 1) % (bothViewsNeeded ? 3 : 2);
        Settings.setSupersizeDistance(supersize);
    }

    // target handling ----------------------------------------------------------------------------------------------

    public void setTarget(final String name) {
        if (StringUtils.isNotEmpty(targetGeocode)) {
            targetView.setText(String.format("%s: %s", targetGeocode, name));
            targetView.setVisibility(View.VISIBLE);
        } else {
            targetView.setText("");
            targetView.setVisibility(View.GONE);
            targetViewSupersize.setVisibility(View.GONE);
        }
    }

    public Geopoint getLastNavTarget() {
        return lastNavTarget;
    }

    public void setLastNavTarget(final Geopoint geopoint) {
        lastNavTarget = geopoint;
    }

    public String getTargetGeocode() {
        return targetGeocode;
    }

    public void setTargetGeocode(final String geocode) {
        targetGeocode = geocode;
    }

}
