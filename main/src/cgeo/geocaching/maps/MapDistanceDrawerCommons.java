package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.settings.Settings;

import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

public class MapDistanceDrawerCommons {
    private static final String STRAIGHT_LINE_SYMBOL = Character.toString((char) 0x007C);
    private static final String WAVY_LINE_SYMBOL = Character.toString((char) 0x2307);

    private final TextView distance1InfoView;
    private final TextView distance1View;
    private final TextView distance2InfoView;
    private final TextView distance2View;
    private final TextView routeDistanceView;
    private final TextView routeDistanceViewSupersize;
    private final TextView distanceSupersizeView;
    private boolean bothViewsNeeded = false;

    public MapDistanceDrawerCommons(final View root) {
        distance1InfoView = root.findViewById(R.id.distance1info);
        distance1View = root.findViewById(R.id.distance1);
        distance2InfoView = root.findViewById(R.id.distance2info);
        distance2View = root.findViewById(R.id.distance2);
        routeDistanceView = root.findViewById(R.id.routeDistance);
        routeDistanceViewSupersize = root.findViewById(R.id.routeDistanceSupersize);
        distanceSupersizeView = root.findViewById(R.id.distanceSupersize);

        distance1InfoView.setOnClickListener(v -> swap());
        distance1View.setOnClickListener(v -> swap());
        distance2InfoView.setOnClickListener(v -> swap());
        distance2View.setOnClickListener(v -> swap());
        routeDistanceView.setOnClickListener(v -> swap());
        distanceSupersizeView.setOnClickListener(v -> swap());
    }

    public void drawDistance(final boolean showBothDistances, final float distance, final float realDistance) {
        final boolean showRealDistance = realDistance != 0.0f && distance != realDistance;
        bothViewsNeeded = showBothDistances && showRealDistance;
        final int supersize = Settings.getSupersizeDistance() % (bothViewsNeeded ? 3 : 2);

        distanceSupersizeView.setVisibility(supersize > 0 ? View.VISIBLE : View.GONE);
        if (bothViewsNeeded) {
            updateDisplay(supersize == 1, distance1InfoView, STRAIGHT_LINE_SYMBOL, distance1View, distance);
            updateDisplay(supersize == 2, distance2InfoView, WAVY_LINE_SYMBOL, distance2View, realDistance);
        } else {
            updateDisplay(supersize > 0, distance1InfoView, "", distance1View, showRealDistance ? realDistance : distance);
        }
    }

    private void updateDisplay(final boolean showAsSupersize, final TextView infoView, final String info, final TextView distanceView, final float distance) {
        final String distanceString = Units.getDistanceFromKilometers(distance);
        final boolean zeroDistance = distance == 0.0f;

        infoView.setVisibility(showAsSupersize || zeroDistance || StringUtils.isEmpty(info) ? View.GONE : View.VISIBLE);
        distanceView.setVisibility(showAsSupersize || zeroDistance ? View.GONE : View.VISIBLE);
        if (showAsSupersize) {
            distanceSupersizeView.setText(StringUtils.isNotEmpty(info) ? info + " " + distanceString : distanceString);
        } else if (!zeroDistance) {
            infoView.setText(info);
            distanceView.setText(distanceString);
        }
    }

    public void drawRouteDistance(final float routeDistance) {
        routeDistanceView.setVisibility(View.GONE);
        routeDistanceViewSupersize.setVisibility(View.GONE);
        if (routeDistance != 0.0f) {
            final TextView view = bothViewsNeeded && Settings.getSupersizeDistance() > 0 ? routeDistanceViewSupersize : routeDistanceView;
            view.setVisibility(View.VISIBLE);
            view.setText(Units.getDistanceFromKilometers(routeDistance));
        }
    }

    private void swap() {
        final int supersize = (Settings.getSupersizeDistance() + 1) % (bothViewsNeeded ? 3 : 2);
        Settings.setSupersizeDistance(supersize);
    }
}
