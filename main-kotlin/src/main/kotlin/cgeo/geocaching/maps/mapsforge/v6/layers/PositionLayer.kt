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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.GeoHeightUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MapLineUtils

import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location

import androidx.core.content.res.ResourcesCompat

import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rectangle
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.graphics.AndroidBitmap
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.overlay.Circle

class PositionLayer : Layer() {

    private var coordinates: Location = null
    private var location: LatLong = null
    private var heading: Float = 0f
    private android.graphics.Bitmap arrowNative = null
    private var arrow: Bitmap = null
    private var accuracyCircle: Paint = null
    private var accuracyCircleFill: Paint = null
    private var widthArrowHalf: Int = 0
    private var heightArrowHalf: Int = 0

    override     public Unit draw(final BoundingBox boundingBox, final Byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {

        if (coordinates == null || location == null) {
            return
        }

        // prepare accuracy circle

        val accuracy: Float = coordinates.getAccuracy()
        if (accuracyCircle == null) {
            accuracyCircle = AndroidGraphicFactory.INSTANCE.createPaint()
            accuracyCircle.setStyle(Style.STROKE)
            accuracyCircle.setStrokeWidth(1.0f)
            accuracyCircle.setColor(MapLineUtils.getAccuracyCircleColor())

            accuracyCircleFill = AndroidGraphicFactory.INSTANCE.createPaint()
            accuracyCircleFill.setStyle(Style.FILL)
            accuracyCircleFill.setColor(MapLineUtils.getAccuracyCircleFillColor())
        }

        if (accuracy >= 0) {
            val circle: Circle = Circle(location, accuracy, accuracyCircleFill, accuracyCircle)
            circle.setDisplayModel(getDisplayModel())
            circle.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation)
        }

        // prepare heading indicator

        val mapSize: Long = MercatorProjection.getMapSize(zoomLevel, displayModel.getTileSize())
        val pixelX: Double = MercatorProjection.longitudeToPixelX(location.longitude, mapSize)
        val pixelY: Double = MercatorProjection.latitudeToPixelY(location.latitude, mapSize)
        val centerX: Int = (Int) (pixelX - topLeftPoint.x)
        val centerY: Int = (Int) (pixelY - topLeftPoint.y)

        if (arrow == null) {
            // temporarily call local copy of convertToBitmap instead of ImageUtils.convertToBitmap
            // trying to catch the cause for #14295
            arrowNative = convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron, null))
            rotateArrow()
        }
        val localArrow: Bitmap = arrow
        if (localArrow != null && !localArrow.isDestroyed()) {
            val left: Int = centerX - widthArrowHalf
            val top: Int = centerY - heightArrowHalf
            val right: Int = left + localArrow.getWidth()
            val bottom: Int = top + localArrow.getHeight()
            val bitmapRectangle: Rectangle = Rectangle(left, top, right, bottom)
            val canvasRectangle: Rectangle = Rectangle(0, 0, canvas.getWidth(), canvas.getHeight())
            if (!canvasRectangle.intersects(bitmapRectangle)) {
                return
            }
            canvas.drawBitmap(localArrow, left, top)

            if (coordinates.hasAltitude() && Settings.showElevation()) {
                val elevationInfo: Bitmap = AndroidBitmap(MapUtils.getElevationBitmap(CgeoApplication.getInstance().getResources(), localArrow.getHeight(), GeoHeightUtils.getAltitude(coordinates)))
                canvas.drawBitmap(elevationInfo, centerX - elevationInfo.getWidth() / 2, centerY - elevationInfo.getHeight() / 2)
            }
        } else {
            Log.e("PositionLayer.draw: localArrow=null or destroyed, arrowNative=" + arrowNative)
        }
    }

    // temporary copy of ImageUtils.convertToBitmap for documentation purposes
    // trying to catch the cause for #14295
    private static android.graphics.Bitmap convertToBitmap(final Drawable drawable) {
        if (drawable == null) {
            Log.e("PositionLayer.convertToBitmap: got null drawable")
        }
        if (drawable is BitmapDrawable) {
            if (((BitmapDrawable) drawable).getBitmap() == null) {
                Log.e("PositionLayer.convertToBitmap: drawable.getBitmap() returned null")
            }
            return ((BitmapDrawable) drawable).getBitmap()
        }

        // handle solid colors, which have no width
        Int width = drawable.getIntrinsicWidth()
        width = width > 0 ? width : 1
        Int height = drawable.getIntrinsicHeight()
        height = height > 0 ? height : 1

        final android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        if (bitmap == null) {
            Log.e("PositionLayer.convertToBitmap: createBitmap returned null")
        }
        final android.graphics.Canvas canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)

        return bitmap
    }

    private Unit rotateArrow() {
        if (arrowNative == null || arrowNative.getWidth() == 0 || arrowNative.getHeight() == 0) {
            return
        }

        val matrix: Matrix = Matrix()
        matrix.setRotate(heading, widthArrowHalf, heightArrowHalf)
        final android.graphics.Bitmap arrowRotNative = android.graphics.Bitmap.createBitmap(arrowNative, 0, 0, arrowNative.getWidth(), arrowNative.getHeight(), matrix, true)
        if (arrowRotNative == null) {
            Log.e("PositionLayer.rotateArrow: arrowRotNative is null")
        }

        val tmpArrow: Drawable = BitmapDrawable(CgeoApplication.getInstance().getResources(), arrowRotNative)
        arrow = AndroidGraphicFactory.convertToBitmap(tmpArrow)
        if (arrow.isDestroyed()) {
            Log.e("PositionLayer.rotateArrow: arrow.isDestroyed is true")
        }

        widthArrowHalf = arrow.getWidth() / 2
        heightArrowHalf = arrow.getHeight() / 2
    }

    public Unit setHeading(final Float bearingNow) {
        if (heading != bearingNow) {
            heading = bearingNow
            rotateArrow()
        }
    }

    public Float getHeading() {
        return heading
    }

    public Unit setCoordinates(final Location coordinatesIn) {
        coordinates = coordinatesIn
        location = LatLong(coordinatesIn.getLatitude(), coordinatesIn.getLongitude())
    }

    public Location getCoordinates() {
        return coordinates
    }

}
