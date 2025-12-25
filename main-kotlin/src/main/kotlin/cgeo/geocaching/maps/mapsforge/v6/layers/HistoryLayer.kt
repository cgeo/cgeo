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

import cgeo.geocaching.maps.PositionHistory
import cgeo.geocaching.models.TrailHistoryElement
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.MapLineUtils

import android.location.Location

import java.util.ArrayList

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

class HistoryLayer : Layer() {

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static val LINE_MAXIMUM_DISTANCE_METERS: Float = 10000

    private val positionHistory: PositionHistory = PositionHistory()
    private Location coordinates
    private Paint historyLine

    public HistoryLayer(final ArrayList<TrailHistoryElement> locationHistory) {
        super()
        if (locationHistory != null) {
            positionHistory.setHistory(locationHistory)
        }
        resetColor()
    }

    public Unit reset() {
        positionHistory.reset()
    }

    public Unit resetColor() {
        historyLine = AndroidGraphicFactory.INSTANCE.createPaint()
        historyLine.setStrokeWidth(MapLineUtils.getHistoryLineWidth(false))
        historyLine.setStyle(Style.STROKE)
        historyLine.setColor(MapLineUtils.getTrailColor())
    }

    override     public Unit draw(final BoundingBox boundingBox, final Byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
        if (coordinates == null) {
            return
        }

        positionHistory.rememberTrailPosition(coordinates)

        if (Settings.isMapTrail()) {
            val paintHistory: ArrayList<TrailHistoryElement> = ArrayList<>(getHistory())
            // always add current position to drawn history to have a closed connection, even if it's not yet recorded
            paintHistory.add(TrailHistoryElement(coordinates))
            val size: Int = paintHistory.size()
            if (size < 2) {
                return
            }

            val mapSize: Long = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize())

            Location prev = paintHistory.get(0).getLocation()
            val path: Path = AndroidGraphicFactory.INSTANCE.createPath()
            Int current = 1
            while (current < size) {
                path.moveTo((Float) (MercatorProjection.longitudeToPixelX(prev.getLongitude(), mapSize) - topLeftPoint.x), (Float) (MercatorProjection.latitudeToPixelY(prev.getLatitude(), mapSize) - topLeftPoint.y))

                Boolean paint = false
                while (!paint && current < size) {
                    val now: Location = paintHistory.get(current).getLocation()
                    current++
                    if (now.distanceTo(prev) < LINE_MAXIMUM_DISTANCE_METERS) {
                        path.lineTo((Float) (MercatorProjection.longitudeToPixelX(now.getLongitude(), mapSize) - topLeftPoint.x), (Float) (MercatorProjection.latitudeToPixelY(now.getLatitude(), mapSize) - topLeftPoint.y))
                    } else {
                        paint = true
                    }
                    prev = now
                }
                if (!path.isEmpty()) {
                    canvas.drawPath(path, historyLine)
                    path.clear()
                }
            }
        }
    }

    public ArrayList<TrailHistoryElement> getHistory() {
        return positionHistory.getHistory()
    }

    public Unit setCoordinates(final Location coordinatesIn) {
        coordinates = coordinatesIn
    }

    public Location getCoordinates() {
        return coordinates
    }

}
