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
 * - distances1/distances2 placeholder will be filled with straight distance, routed distance
 *   and individual route length (depending on settings and current data)
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
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.utils.functions.Action1;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.util.Pair;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

public class UnifiedTargetAndDistancesHandler {

    // navigation target info
    protected String targetGeocode = null;
    protected Geopoint lastNavTarget = null;

    private final LinearLayout distances1;
    private final LinearLayout distances2;
    private final TextView distanceSupersizeView;
    private final TextView targetView;
    private boolean bothViewsNeeded = false;

    private boolean showBothDistances = false;
    private float distance = 0.0f;
    private float realDistance = 0.0f;
    private float routeDistance = 0.0f;
    private final Runnable handleSwapNotification;

    private static final float MIN_DISTANCE = 0.0005f;
    private static final float MIN_DIFF = 0.015f;

    UnifiedTargetAndDistancesHandler(final View root, final Runnable handleSwapNotification) {
        distances1 = root.findViewById(R.id.distances1);
        distances2 = root.findViewById(R.id.distances2);
        targetView = root.findViewById(R.id.target);
        distanceSupersizeView = root.findViewById(R.id.distanceSupersize);

        distances1.setOnClickListener(v -> swap());
        distances2.setOnClickListener(v -> swap());
        distanceSupersizeView.setOnClickListener(v -> swap());

        this.handleSwapNotification = handleSwapNotification;
    }

    // distances handling -------------------------------------------------------------------------------------------

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
        updateDistanceViews(distance, realDistance, routeDistance, showBothDistances, distances1, distances2, distanceSupersizeView, targetView, bvn -> bothViewsNeeded = bvn);
        if (handleSwapNotification != null) {
            handleSwapNotification.run();
        }
    }

    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public static void updateDistanceViews(
            final float distance, final float realDistance, final float routeDistance, final boolean showBothDistances,
            final LinearLayout distances1, final LinearLayout distances2,
            final TextView distanceSupersizeView, final TextView targetView,
            final Action1<Boolean> updateBothViewNeeded
    ) {
        Pair<Integer, String> supersizeInfo = new Pair<>(0, "");
        final ArrayList<Pair<Integer, String>>[] data = new ArrayList[2];
        data[0] = new ArrayList<>();
        data[1] = new ArrayList<>();

        final boolean showRealDistance = realDistance > MIN_DISTANCE && Math.abs(distance - realDistance) > MIN_DIFF && Settings.getRoutingMode() != RoutingMode.STRAIGHT;
        final boolean bothViewsNeeded = showBothDistances && showRealDistance;
        updateBothViewNeeded.call(bothViewsNeeded);
        final int supersize = distance < MIN_DISTANCE ? 0 : Settings.getSupersizeDistance() % (bothViewsNeeded ? 3 : 2);

        final Pair<Integer, String> distanceInfo = new Pair<>(RoutingMode.STRAIGHT.drawableId, Units.getDistanceFromKilometers(distance));
        final Pair<Integer, String> realDistanceInfo = new Pair<>(Settings.getRoutingMode().drawableId, Units.getDistanceFromKilometers(realDistance));

        // collect data to be displayed
        if (bothViewsNeeded) {
            if (supersize == 1) {
                supersizeInfo = distanceInfo;
                data[0].add(realDistanceInfo);
            } else if (supersize == 2) {
                supersizeInfo = realDistanceInfo;
                data[0].add(distanceInfo);
            } else {
                data[0].add(distanceInfo);
                data[0].add(realDistanceInfo);
            }
        } else if (distance > MIN_DISTANCE) {
            if (supersize > 0) {
                supersizeInfo = showRealDistance ? realDistanceInfo : distanceInfo;
            } else {
                data[0].add(showRealDistance ? realDistanceInfo : distanceInfo);
            }
        }
        if (routeDistance > MIN_DISTANCE) {
            data[data[0].size() > 0 && !supersizeInfo.second.isEmpty() ? 1 : 0].add(new Pair<>(R.drawable.map_quick_route, Units.getDistanceFromKilometers(routeDistance)));
        }

        // update views
        syncViews(distances1, data[0]);
        distances1.setVisibility(data[0].size() > 0 ? View.VISIBLE : View.GONE);
        syncViews(distances2, data[1]);
        distances2.setVisibility(data[1].size() > 0 ? View.VISIBLE : View.GONE);

        if (supersizeInfo.second.isEmpty()) {
            distanceSupersizeView.setVisibility(View.GONE);
            targetView.setBackgroundResource(R.drawable.icon_bcg);
            distances1.setBackgroundResource(R.drawable.icon_bcg);
        } else {
            distanceSupersizeView.setVisibility(View.VISIBLE);
            targetView.setBackground(null);
            distances1.setBackgroundResource(0);
            TextParam.text(supersizeInfo.second).setImage(ImageParam.id(supersizeInfo.first), TextParam.IMAGE_SIZE_EQUAL_TEXT_SIZE).setImageTint(-1).applyTo(distanceSupersizeView);
        }
    }

    /**
     * syncs actual children of LinearLayouts with current data, removes/creates views "on the fly"
     * updates views only if changes need to be applied
     */
    private static void syncViews(final LinearLayout ll, final ArrayList<Pair<Integer, String>> data) {
        final int existing = ll.getChildCount();
        int count = 0;
        for (Pair<Integer, String> info : data) {
            final TextView tv;
            if (count < existing) {
                tv = (TextView) ll.getChildAt(count);
            } else {
                tv = new TextView(ll.getContext(), null, 0, R.style.map_distanceinfo_no_background);
                ll.addView(tv);
            }
            tv.setVisibility(View.VISIBLE);
            if (!StringUtils.equals(tv.getHint(), String.valueOf(info.first)) || !StringUtils.equals(tv.getText(), info.second)) {
                TextParam.text(info.second).setImage(ImageParam.id(info.first)).setImageTint(-1).applyTo(tv);
            }
            tv.setHint(String.valueOf(info.first));
            count++;
        }
        for (int i = count; i < existing; i++) {
            ll.removeView(ll.getChildAt(data.size()));
        }
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
