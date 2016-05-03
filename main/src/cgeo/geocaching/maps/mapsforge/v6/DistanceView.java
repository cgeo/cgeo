package cgeo.geocaching.maps.mapsforge.v6;

import android.location.Location;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;

public class DistanceView {

    private Geopoint destinationCoords;

    @BindView(R.id.distance) protected TextView distanceView;

    public DistanceView(final Geopoint destinationCoords, final TextView distanceView) {

        this.distanceView = distanceView;

        setDestination(destinationCoords);
    }

    public void setDestination(final Geopoint coords) {
        destinationCoords = coords;
        this.distanceView.setVisibility(destinationCoords != null ? View.VISIBLE : View.GONE);
    }

    public void setCoordinates(final Location coordinatesIn) {
        if (destinationCoords == null || coordinatesIn == null) {
            return;
        }

        final Geopoint currentCoords = new Geopoint(coordinatesIn);

        final float distance = currentCoords.distanceTo(destinationCoords);
        distanceView.setText(Units.getDistanceFromKilometers(distance));
    }
}
