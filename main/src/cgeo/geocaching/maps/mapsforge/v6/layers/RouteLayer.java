package cgeo.geocaching.maps.mapsforge.v6.layers;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.maps.mapsforge.v6.TapHandler;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemLayer;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.utils.MapLineUtils;

import android.graphics.drawable.Drawable;

import androidx.core.content.res.ResourcesCompat;

import java.util.HashSet;
import java.util.Set;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
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
        resetColor();
        width = MapLineUtils.getRouteLineWidth();
    }

    public void resetColor() {
        lineColor = MapLineUtils.getRouteColor();
        super.resetColor();
    }

    @Override
    public void updateIndividualRoute(final IndividualRoute route) {
        super.updateRoute(KEY, route);

        layerManager.getLayers().removeAll(overlayLayers);
        overlayLayers.clear();

        for (RouteItem item : route.getRouteItems()) {
            if (item.getType() == RouteItem.RouteItemType.COORDS) {
                final Drawable drawable = ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.marker_routepoint, null);
                final Bitmap marker = AndroidGraphicFactory.convertToBitmap(drawable);
                final Layer layer = new GeoitemLayer(new GeoitemRef(item.getIdentifier(), CoordinatesType.COORDS, null, 0, null, 0), false, tapHandler, new LatLong(item.getPoint().getLatitude(), item.getPoint().getLongitude()), marker, 0, 0);

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
