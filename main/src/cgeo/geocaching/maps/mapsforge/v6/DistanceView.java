package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;

import android.location.Location;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;

public class DistanceView {

    private Geopoint destinationCoords;
    private float realDistance = 0.0f;
    private boolean showBothDistances = false;
    private float routeDistance = 0.0f;

    private static final String STRAIGHT_LINE_SYMBOL = Character.toString((char) 0x007C);
    private static final String WAVY_LINE_SYMBOL = Character.toString((char) 0x2307);

    @BindView(R.id.distance1info) protected TextView distance1InfoView;
    @BindView(R.id.distance1) protected TextView distance1View;
    @BindView(R.id.distance2info) protected TextView distance2InfoView;
    @BindView(R.id.distance2) protected TextView distance2View;
    @BindView(R.id.routeDistance) protected TextView routeDistanceView;

    public DistanceView(final Geopoint destinationCoords, final TextView distance1InfoView, final TextView distance1View, final TextView distance2InfoView, final TextView distance2View, final boolean showBothDistances, final TextView routeDistanceView) {
        this.distance1InfoView = distance1InfoView;
        this.distance1View = distance1View;
        this.distance2InfoView = distance2InfoView;
        this.distance2View = distance2View;
        this.showBothDistances = showBothDistances;
        this.routeDistanceView = routeDistanceView;

        setDestination(destinationCoords);
    }

    public void setDestination(final Geopoint coords) {
        destinationCoords = coords;
        realDistance = 0.0f;
        this.distance1View.setVisibility(destinationCoords != null ? View.VISIBLE : View.GONE);
    }

    public void setRealDistance(final float realDistance) {
        this.realDistance = realDistance;
    }

    public void setCoordinates(final Location coordinatesIn) {
        if (destinationCoords == null || coordinatesIn == null) {
            return;
        }

        final Geopoint currentCoords = new Geopoint(coordinatesIn);
        final float distance = currentCoords.distanceTo(destinationCoords);

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
    }


    public void setRouteDistance(final float routeDistance) {
        this.routeDistance = routeDistance;
    }

    public void showRouteDistance() {
        routeDistanceView.setVisibility(routeDistance != 0.0f ? View.VISIBLE : View.GONE);
        if (routeDistance != 0.0f) {
            routeDistanceView.setText(Units.getDistanceFromKilometers(routeDistance));
        }
    }
}
