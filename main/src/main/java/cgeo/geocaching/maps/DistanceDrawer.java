package cgeo.geocaching.maps;

import cgeo.geocaching.location.Geopoint;

import android.graphics.Canvas;
import android.location.Location;
import android.view.View;

public class DistanceDrawer {
    private final Geopoint destinationCoords;

    private float distance = 0.0f;
    private float realDistance = 0.0f;
    private boolean showBothDistances = false;
    private float routeDistance = 0.0f;

    private final MapDistanceDrawerCommons mapDistanceDrawer;

    public DistanceDrawer(final View root, final Geopoint destinationCoords, final boolean showBothDistances) {
        this.destinationCoords = destinationCoords;
        this.showBothDistances = showBothDistances;
        mapDistanceDrawer = new MapDistanceDrawerCommons(root);
    }

    public void setCoordinates(final Location location) {
        if (destinationCoords == null || location == null) {
            distance = 0.0f;
        } else {
            distance = new Geopoint(location).distanceTo(destinationCoords);
        }
    }

    public Geopoint getDestinationCoords() {
        return destinationCoords;
    }

    public void setRealDistance(final float realDistance) {
        this.realDistance = realDistance;
    }

    public void setRouteDistance(final float routeDistance) {
        this.routeDistance = routeDistance;
    }

    public void drawDistance(final Canvas canvas) {
        mapDistanceDrawer.drawDistance(showBothDistances, distance, realDistance);
        mapDistanceDrawer.drawRouteDistance(routeDistance);
    }
}
