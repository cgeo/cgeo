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
    private final TextView distanceSupersizeView;
    private boolean bothViewsNeeded = false;

    public MapDistanceDrawerCommons(final View root) {
        distance1InfoView = root.findViewById(R.id.distance1info);
        distance1View = root.findViewById(R.id.distance1);
        distance2InfoView = root.findViewById(R.id.distance2info);
        distance2View = root.findViewById(R.id.distance2);
        routeDistanceView = root.findViewById(R.id.routeDistance);
        distanceSupersizeView = root.findViewById(R.id.distanceSupersize);

        distance1InfoView.setOnClickListener(v -> swap());
        distance1View.setOnClickListener(v -> swap());
        distance2InfoView.setOnClickListener(v -> swap());
        distance2View.setOnClickListener(v -> swap());
        routeDistanceView.setOnClickListener(v -> swap());
        distanceSupersizeView.setOnClickListener(v -> swap());
    }

    public void drawDistance(final boolean showBothDistances, final float distance, final float realDistance) {
        bothViewsNeeded = showBothDistances && realDistance != 0.0f && distance != realDistance;
        final int supersize = Settings.getSupersizeDistance() % (bothViewsNeeded ? 3 : 2);

        distanceSupersizeView.setVisibility(supersize > 0 ? View.VISIBLE : View.GONE);
        if (bothViewsNeeded) {
            updateDisplay(supersize == 1, distance1InfoView, STRAIGHT_LINE_SYMBOL, distance1View, Units.getDistanceFromKilometers(distance));
            updateDisplay(supersize == 2, distance2InfoView, WAVY_LINE_SYMBOL, distance2View, Units.getDistanceFromKilometers(realDistance));
        } else {
            updateDisplay(supersize > 0, distance1InfoView, "", distance1View, Units.getDistanceFromKilometers(realDistance != 0.0f && distance != realDistance ? realDistance : distance));
        }
    }

    private void updateDisplay(final boolean showAsSupersize, final TextView infoView, final String info, final TextView distanceView, final String distance) {
        infoView.setVisibility(showAsSupersize || StringUtils.isEmpty(info) ? View.GONE : View.VISIBLE);
        distanceView.setVisibility(showAsSupersize ? View.GONE : View.VISIBLE);
        if (showAsSupersize) {
            distanceSupersizeView.setText(StringUtils.isNotEmpty(info) ? info + " " + distance : distance);
        } else {
            infoView.setText(info);
            distanceView.setText(distance);
        }
    }

    public void drawRouteDistance(final float routeDistance) {
        routeDistanceView.setVisibility(routeDistance != 0.0f ? View.VISIBLE : View.GONE);
        if (routeDistance != 0.0f) {
            routeDistanceView.setText(Units.getDistanceFromKilometers(routeDistance));
        }
    }

    private void swap() {
        final int supersize = (Settings.getSupersizeDistance() + 1) % (bothViewsNeeded ? 3 : 2);
        Settings.setSupersizeDistance(supersize);
    }
}
