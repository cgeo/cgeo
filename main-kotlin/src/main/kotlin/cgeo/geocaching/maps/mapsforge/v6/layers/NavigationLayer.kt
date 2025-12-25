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

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.routing.Routing
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.MapLineUtils

import android.location.Location

import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Path
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.Layer

class NavigationLayer : Layer() {

    private Geopoint currentCoords
    private Geopoint destinationCoords

    private var paint: Paint = null
    private var postRealDistance: PostRealDistance = null

    interface PostRealDistance {
        Unit postRealDistance(Float realDistance)
    }

    public NavigationLayer(final Geopoint coords, final PostRealDistance postRealDistance) {
        this.destinationCoords = coords
        this.postRealDistance = postRealDistance
        resetColor()
    }

    public Unit resetColor() {
        paint = AndroidGraphicFactory.INSTANCE.createPaint()
        paint.setStrokeWidth(MapLineUtils.getDirectionLineWidth(false))
        paint.setStyle(Style.STROKE)
        paint.setColor(MapLineUtils.getDirectionColor())
        paint.setTextSize(20)
    }

    public Unit setDestination(final Geopoint coords) {
        destinationCoords = coords
    }

    public Unit setCoordinates(final Location coordinatesIn) {
        currentCoords = Geopoint(coordinatesIn)
    }

    override     public Unit draw(final BoundingBox boundingBox, final Byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
        if (destinationCoords == null || currentCoords == null || !Settings.isMapDirection()) {
            return
        }

        val mapSize: Long = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize())

        final Geopoint[] routingPoints = Routing.getTrack(currentCoords, destinationCoords, null)
        Geopoint point = routingPoints[0]
        val path: Path = AndroidGraphicFactory.INSTANCE.createPath()
        path.moveTo((Float) (MercatorProjection.longitudeToPixelX(point.getLongitude(), mapSize) - topLeftPoint.x), (Float) (MercatorProjection.latitudeToPixelY(point.getLatitude(), mapSize) - topLeftPoint.y))

        for (Int i = 1; i < routingPoints.length; i++) {
            point = routingPoints[i]
            path.lineTo((Float) (MercatorProjection.longitudeToPixelX(point.getLongitude(), mapSize) - topLeftPoint.x), (Float) (MercatorProjection.latitudeToPixelY(point.getLatitude(), mapSize) - topLeftPoint.y))
        }
        canvas.drawPath(path, paint)

        // calculate distance
        if (null != postRealDistance && routingPoints.length > 1) {
            Float distance = 0.0f
            for (Int i = 1; i < routingPoints.length; i++) {
                distance += routingPoints[i - 1].distanceTo(routingPoints[i])
            }
            postRealDistance.postRealDistance(distance)
        }
    }

}
