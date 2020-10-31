package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Units;

import android.view.View;
import android.widget.TextView;

public class MapDistanceDrawerCommons {
    private static final String STRAIGHT_LINE_SYMBOL = Character.toString((char) 0x007C);
    private static final String WAVY_LINE_SYMBOL = Character.toString((char) 0x2307);

    private final TextView distance1InfoView;
    private final TextView distance1View;
    private final TextView distance2InfoView;
    private final TextView distance2View;
    private final TextView routeDistanceView;

    public MapDistanceDrawerCommons(final View root) {
        distance1InfoView = root.findViewById(R.id.distance1info);
        distance1View = root.findViewById(R.id.distance1);
        distance2InfoView = root.findViewById(R.id.distance2info);
        distance2View = root.findViewById(R.id.distance2);
        routeDistanceView = root.findViewById(R.id.routeDistance);
    }

    public void drawDistance(final boolean showBothDistances, final float distance, final float realDistance) {
        final boolean bothViewsNeeded = showBothDistances && realDistance != 0.0f && distance != realDistance;
        distance1InfoView.setVisibility(bothViewsNeeded ? View.VISIBLE : View.GONE);
        distance2InfoView.setVisibility(bothViewsNeeded ? View.VISIBLE : View.GONE);
        distance2View.setVisibility(bothViewsNeeded ? View.VISIBLE : View.GONE);
        if (bothViewsNeeded) {
            distance1InfoView.setText(STRAIGHT_LINE_SYMBOL);
            distance1View.setText(Units.getDistanceFromKilometers(distance));
            distance2InfoView.setText(WAVY_LINE_SYMBOL);
            distance2View.setText(Units.getDistanceFromKilometers(realDistance));
        } else {
            distance1View.setText(Units.getDistanceFromKilometers(realDistance != 0.0f && distance != realDistance ? realDistance : distance));
        }
        distance1View.setVisibility(realDistance != 0.0f ? View.VISIBLE : View.GONE);
    }

    public void drawRouteDistance(final float routeDistance) {
        routeDistanceView.setVisibility(routeDistance != 0.0f ? View.VISIBLE : View.GONE);
        if (routeDistance != 0.0f) {
            routeDistanceView.setText(Units.getDistanceFromKilometers(routeDistance));
        }
    }
}
