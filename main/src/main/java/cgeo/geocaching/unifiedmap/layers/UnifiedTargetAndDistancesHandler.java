package cgeo.geocaching.unifiedmap.layers;

/*
 * Manages distance and target views
 *
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
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.settings.Settings;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public class UnifiedTargetAndDistancesHandler {

    // navigation target info
    protected String targetGeocode = null;
    protected Geopoint lastNavTarget = null;

    private static final String STRAIGHT_LINE_SYMBOL = Character.toString((char) 0x007C);
    private static final String WAVY_LINE_SYMBOL = Character.toString((char) 0x2307);
    private static final String ELEVATION_SYMBOL = Character.toString((char) 0x25e2);

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

    UnifiedTargetAndDistancesHandler(final View root) {
        distances1 = root.findViewById(R.id.distances1);
        distances2 = root.findViewById(R.id.distances2);
        targetView = root.findViewById(R.id.target);
        distanceSupersizeView = root.findViewById(R.id.distanceSupersize);

        distances1.setOnClickListener(v -> swap());
        distances2.setOnClickListener(v -> swap());
        distanceSupersizeView.setOnClickListener(v -> swap());
    }

    // distances handling -------------------------------------------------------------------------------------------

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
        updateDistanceViews(distanceInfo, realDistanceInfo, routingInfo, elevationInfo, distances1, distances2, distanceSupersizeView, targetView);
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public static void updateDistanceViews(
            final String distanceInfo, final String realDistanceInfo, final String routingInfo, final String elevationInfo,
            final LinearLayout distances1, final LinearLayout distances2,
            final TextView distanceSupersizeView, final TextView targetView
    ) {
        final int supersize = Settings.getSupersizeDistance();
        String supersizeInfo = "";
        final ArrayList<String>[] data = new ArrayList[2];
        data[0] = new ArrayList<>();
        data[1] = new ArrayList<>();

        if (!distanceInfo.isEmpty()) {
            if (supersize == 2) {
                supersizeInfo = distanceInfo;
            } else {
                data[0].add(distanceInfo);
            }
        }

        if (!realDistanceInfo.isEmpty()) {
            if (supersize == 1) {
                supersizeInfo = realDistanceInfo;
            } else {
                data[0].add(realDistanceInfo);
            }
        }

        if (!routingInfo.isEmpty()) {
            data[data[0].size() > 0 && !supersizeInfo.isEmpty() ? 1 : 0].add(routingInfo);
        }

        if (!elevationInfo.isEmpty()) {
            data[data[0].size() > 0 && !supersizeInfo.isEmpty() ? 1 : 0].add(elevationInfo);
        }

        syncViews(distances1, data[0]);
        distances1.setVisibility(data[0].size() > 0 ? View.VISIBLE : View.GONE);
        syncViews(distances2, data[1]);
        distances2.setVisibility(data[1].size() > 0 ? View.VISIBLE : View.GONE);

        if (supersizeInfo.isEmpty()) {
            distanceSupersizeView.setVisibility(View.GONE);
            targetView.setBackgroundResource(R.drawable.icon_bcg);
            distances1.setBackgroundResource(R.drawable.icon_bcg);
        } else {
            distanceSupersizeView.setVisibility(View.VISIBLE);
            targetView.setBackground(null);
            distances1.setBackgroundResource(0);
            distanceSupersizeView.setText(supersizeInfo);
        }
    }

    /** syncs actual children of LinearLayots with current data, removes/creates views "on the fly" */
    private static void syncViews(final LinearLayout ll, final ArrayList<String> data) {
        final int existing = ll.getChildCount();
        int count = 0;
        for (String info : data) {
            if (count < existing) {
                ((TextView) ll.getChildAt(count)).setText(info);
            } else {
                final TextView tv = new TextView(ll.getContext(), null, 0, R.style.map_distanceinfo_no_background);
                tv.setText(info);
                tv.setVisibility(View.VISIBLE);
                ll.addView(tv);
            }
            count++;
        }
        for (int i = count; i < existing; i++) {
            ll.removeView(ll.getChildAt(data.size()));
        }
    }

    // elevation handling -------------------------------------------------------------------------------------------

    public void drawElevation(final float elevation) {
        elevationInfo = buildElevationInfo(elevation, Float.NaN);
        updateDistanceViews();
    }

    public static String buildElevationInfo(final float elevationFromRouting, final float elevationFromGNSS) {
        // Float.isNaN() is equivalent to Routing.NO_ELEVATION_AVAILABLE
        final String temp = !Float.isNaN(elevationFromRouting) ? String.format(Locale.getDefault(), "%.1f", elevationFromRouting) : !Float.isNaN(elevationFromGNSS) ? String.format(Locale.getDefault(), "%.1f", elevationFromGNSS) : "";
        return StringUtils.isBlank(temp) ? "" : ELEVATION_SYMBOL + " " + temp + "m";
    }

    // target handling ----------------------------------------------------------------------------------------------

    public void setTarget(final String name) {
        if (StringUtils.isNotEmpty(targetGeocode)) {
            targetView.setText(String.format("%s: %s", targetGeocode, name));
            targetView.setVisibility(View.VISIBLE);
        } else {
            targetView.setText("");
            targetView.setVisibility(View.GONE);
        }
    }

    public void setLastNavTarget(final Geopoint geopoint) {
        lastNavTarget = geopoint;
    }

    public void setTargetGeocode(final String geocode) {
        targetGeocode = geocode;
    }

}
