package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func2;

import android.location.Location;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.oscim.core.GeoPoint;

/**
 * layer for positioning the following elements on the map
 * - position and heading arrow
 * - position history
 * - direction line
 * - target and distance infos (target cache, distance to target, length of individual route)
 * - individual route
 * - route(s)/track(s)
 *
 * T is the type the map expects its coordinates in (LatLng for Google Maps, GeoPoint for Mapsforge)
 */
public abstract class AbstractPositionLayer<T> {

    protected Location currentLocation = null;
    protected float currentHeading = 0.0f;
    protected Func2<Double, Double, T> createNewPoint;

    // distance view
    protected GeoPoint destination;
    protected float routedDistance = 0.0f;
    protected float directDistance = 0.0f;
    protected float individualRouteDistance = 0.0f;
    private final boolean showBothDistances = Settings.isBrouterShowBothDistances();
    public final UnifiedTargetAndDistancesHandler mapDistanceDrawer;

    protected AbstractPositionLayer(final View root, final Func2<Double, Double, T> createNewPoint) {
        mapDistanceDrawer = new UnifiedTargetAndDistancesHandler(root);
        this.createNewPoint = createNewPoint;
    }

    public float getCurrentHeading() {
        return currentHeading;
    }

    // ========================================================================
    // distance view handling

    public final void setDestination(final GeoPoint destination) {
        this.destination = destination;
        repaintDestinationHelper(null);
    }


    // ========================================================================
    // repaint methods

    protected void repaintPosition() { //TODO re-enable MapDistanceDrawer
        mapDistanceDrawer.drawDistance(showBothDistances, directDistance, routedDistance);
        mapDistanceDrawer.drawRouteDistance(individualRouteDistance);
    }

    private void repaintDestinationHelper(final @Nullable Action1<List<T>> drawSegment) {
        if (currentLocation != null && destination != null) {
            final Geopoint[] routingPoints = Routing.getTrack(new Geopoint(currentLocation.getLatitude(), currentLocation.getLongitude()), new Geopoint(destination.getLatitude(), destination.getLongitude()));
            final ArrayList<T> points = new ArrayList<>();
            points.add(createNewPoint.call(routingPoints[0].getLatitude(), routingPoints[0].getLongitude()));

            routedDistance = 0.0f;
            if (routingPoints.length > 2 || Settings.isMapDirection()) {
                for (int i = 1; i < routingPoints.length; i++) {
                    points.add(createNewPoint.call(routingPoints[i].getLatitude(), routingPoints[i].getLongitude()));
                    routedDistance += routingPoints[i - 1].distanceTo(routingPoints[i]);
                }
                if (drawSegment != null) {
                    drawSegment.call(points);
                }
            }
            mapDistanceDrawer.drawDistance(showBothDistances, directDistance, routedDistance);
        }
    }
}
