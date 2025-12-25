// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.maps.mapsforge.v6.layers

import cgeo.geocaching.maps.mapsforge.v6.TapHandler
import cgeo.geocaching.models.IndividualRoute
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.utils.MapLineUtils

import java.util.HashSet
import java.util.Set

import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.LayerManager

class RouteLayer : AbstractRouteLayer() : IndividualRoute.UpdateIndividualRoute {

    private static val KEY: String = "INDIVIDUALROUTE"

    private var distance: Float = 0.0f
    private final PostRealDistance postRealRouteDistance
    private final TapHandler tapHandler
    private final LayerManager layerManager
    private val overlayLayers: Set<Layer> = HashSet<>()



    interface PostRealDistance {
        Unit postRealDistance(Float realDistance)
    }

    public RouteLayer(final PostRealDistance postRealRouteDistance, final TapHandler tapHandler, final LayerManager layerManager) {
        super()
        this.postRealRouteDistance = postRealRouteDistance
        this.tapHandler = tapHandler
        this.layerManager = layerManager
        resetPaint(MapLineUtils.getRouteColor(), MapLineUtils.getRouteLineWidth(false))
    }

    override     public Unit updateIndividualRoute(final IndividualRoute route) {
        super.updateRoute(KEY, route, MapLineUtils.getRouteColor(), MapLineUtils.getRawRouteLineWidth())

        layerManager.getLayers().removeAll(overlayLayers)
        overlayLayers.clear()

        for (RouteItem item : route.getRouteItems()) {
            if (item.getType() == RouteItem.RouteItemType.COORDS) {
                val layer: Layer = IndividualRoutePointLayer(LatLong(item.getPoint().getLatitude(), item.getPoint().getLongitude()), tapHandler)
                layerManager.getLayers().add(layer)
                overlayLayers.add(layer)
            }
        }

        this.distance = route.getDistance()

        if (postRealRouteDistance != null) {
            postRealRouteDistance.postRealDistance(distance)
        }
    }

    override     public Unit draw(final BoundingBox boundingBox, final Byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
        super.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation)

        if (postRealRouteDistance != null) {
            postRealRouteDistance.postRealDistance(distance)
        }
    }
}
