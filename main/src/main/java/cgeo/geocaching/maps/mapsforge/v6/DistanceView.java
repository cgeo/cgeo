package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.MapDistanceDrawerCommons;

import android.location.Location;
import android.view.View;

public class DistanceView {

    private Geopoint destinationCoords;
    private float realDistance = 0.0f;
    private boolean showBothDistances = false;
    private float routeDistance = 0.0f;

    private final MapDistanceDrawerCommons mapDistanceDrawer;

    public DistanceView(final View root, final Geopoint destinationCoords, final boolean showBothDistances) {
        mapDistanceDrawer = new MapDistanceDrawerCommons(root);
        this.showBothDistances = showBothDistances;
        setDestination(destinationCoords);
    }

    public void setDestination(final Geopoint coords) {
        destinationCoords = coords;
        realDistance = 0.0f;
        if (destinationCoords == null) {
            mapDistanceDrawer.drawDistance(showBothDistances, 0.0f, 0.0f);
        }
    }

    public void setRealDistance(final float realDistance) {
        this.realDistance = realDistance;
    }

    public void setCoordinates(final Location coordinatesIn) {
        if (destinationCoords == null || coordinatesIn == null) {
            return;
        }

        final Geopoint currentCoords = new Geopoint(coordinatesIn);
        final float distance = null != destinationCoords ? currentCoords.distanceTo(destinationCoords) : 0.0f;
        mapDistanceDrawer.drawDistance(showBothDistances, distance, realDistance);
    }

    public void setRouteDistance(final float routeDistance) {
        this.routeDistance = routeDistance;
    }

    public void showRouteDistance() {
        mapDistanceDrawer.drawRouteDistance(routeDistance);
    }
}
