package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.models.ManualRoute;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.utils.MapLineUtils;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;

public class RouteLayer extends AbstractRouteLayer implements ManualRoute.UpdateManualRoute {

    private float distance = 0.0f;
    private PostRealDistance postRealRouteDistance = null;

    public interface PostRealDistance {
        void postRealDistance (float realDistance);
    }

    public RouteLayer(final PostRealDistance postRealRouteDistance) {
        super();
        this.postRealRouteDistance = postRealRouteDistance;
        resetColor();
        width = MapLineUtils.getRouteLineWidth();
    }

    public void resetColor() {
        lineColor = MapLineUtils.getRouteColor();
        super.resetColor();
    }

    @Override
    public void updateManualRoute(final Route route) {
        super.updateRoute(route);
        this.distance = route.getDistance();

        if (postRealRouteDistance != null) {
            postRealRouteDistance.postRealDistance(distance);
        }
    }

    @Override
    public void draw(final BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint) {
        super.draw(boundingBox, zoomLevel, canvas, topLeftPoint);

        if (postRealRouteDistance != null) {
            postRealRouteDistance.postRealDistance(distance);
        }
    }
}
