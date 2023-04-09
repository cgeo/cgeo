package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.maps.mapsforge.v6.TapHandler;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.utils.MapLineUtils;

import java.util.HashSet;
import java.util.Set;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.LayerManager;

public class RouteLayer extends AbstractRouteLayer implements IndividualRoute.UpdateIndividualRoute {

    private static final String KEY = "INDIVIDUALROUTE";

    private float distance = 0.0f;
    private final PostRealDistance postRealRouteDistance;
    private final TapHandler tapHandler;
    private final LayerManager layerManager;
    private final Set<Layer> overlayLayers  = new HashSet<>();



    public interface PostRealDistance {
        void postRealDistance(float realDistance);
    }

    public RouteLayer(final PostRealDistance postRealRouteDistance, final TapHandler tapHandler, final LayerManager layerManager) {
        super();
        this.postRealRouteDistance = postRealRouteDistance;
        this.tapHandler = tapHandler;
        this.layerManager = layerManager;
        resetPaint(MapLineUtils.getRouteColor(), MapLineUtils.getRouteLineWidth(false));
    }

    @Override
    public void updateIndividualRoute(final IndividualRoute route) {
        super.updateRoute(KEY, route, MapLineUtils.getRouteColor(), MapLineUtils.getRawRouteLineWidth());

        layerManager.getLayers().removeAll(overlayLayers);
        overlayLayers.clear();

        for (RouteItem item : route.getRouteItems()) {
            if (item.getType() == RouteItem.RouteItemType.COORDS) {
                final Layer layer = new IndividualRoutePointLayer(new LatLong(item.getPoint().getLatitude(), item.getPoint().getLongitude()), tapHandler);
                layerManager.getLayers().add(layer);
                overlayLayers.add(layer);
            }
        }

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
