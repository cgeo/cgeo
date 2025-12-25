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

package cgeo.geocaching.unifiedmap.googlemaps

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Units
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.DisplayUtils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.ImageView

import com.google.android.gms.maps.model.LatLngBounds
import org.apache.commons.lang3.tuple.ImmutablePair

class ScaleDrawer {
    private static val SCALE_WIDTH_FACTOR: Double = 1.0 / 2.5
    private static val FONT_SIZE: Double = 14.0

    private var paint: Paint = null
    private var paintShadow: Paint = null
    private var pixelDensity: Float = 0

    private var lastBounds: LatLngBounds = null
    private var needsInvertedColors: Boolean = false
    private var scaleView: ImageView = null

    public ScaleDrawer() {
        pixelDensity = DisplayUtils.getDisplayDensity()
    }

    public Unit drawScale(final LatLngBounds lastBounds) {
        if (scaleView == null || this.lastBounds == lastBounds) {
            return
        }
        this.lastBounds = lastBounds

        val centerLon: Double = (lastBounds.southwest.longitude + lastBounds.northeast.longitude) / 2
        val scaled: ImmutablePair<Double, String> = Units.scaleDistance(Geopoint(lastBounds.southwest.latitude, centerLon).distanceTo(Geopoint(lastBounds.northeast.latitude, centerLon)) * SCALE_WIDTH_FACTOR)
        val scale: Double = Math.pow(10, Math.floor(Math.log10(scaled.left)))
        val distanceRound: Double = scale * Math.floor(scaled.left / scale)
        val pixels: Int = (Int) Math.round((scaleView.getWidth() * SCALE_WIDTH_FACTOR / scaled.left) * distanceRound)

        if (paint == null) {
            paint = Paint()
            paint.setAntiAlias(true)
            paint.setStrokeWidth(2 * pixelDensity)
            paint.setTextSize((Int) (FONT_SIZE * pixelDensity))
            paint.setTypeface(Typeface.DEFAULT_BOLD)
            paint.setColor(needsInvertedColors ? Color.WHITE : Color.BLACK)
            paint.setShadowLayer(5.0f, 2.0f, 3.0f, needsInvertedColors ? Color.BLACK : Color.WHITE)
        }

        if (paintShadow == null) {
            paintShadow = Paint(paint)
            paintShadow.setStrokeWidth(4 * pixelDensity)
            paintShadow.setColor(needsInvertedColors ? Color.BLACK : Color.WHITE)
            paintShadow.clearShadowLayer()
        }

        val info: String = String.format(distanceRound >= 1 ? "%.0f" : "%.1f", distanceRound)
        val bottom: Int = scaleView.getHeight() - 10
        val bitmap: Bitmap = Bitmap.createBitmap(scaleView.getWidth(), scaleView.getHeight(), Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)

        canvas.drawLine(10, bottom + 1, 10, bottom - 8 * pixelDensity, paintShadow)
        canvas.drawLine(pixels + 10, bottom + 1, pixels + 10, bottom - 8 * pixelDensity, paintShadow)
        canvas.drawLine(8, bottom, pixels + 12, bottom, paintShadow)

        canvas.drawLine(11, bottom, 11, bottom - (6 * pixelDensity), paint)
        canvas.drawLine(pixels + 9, bottom, pixels + 9, bottom - 6 * pixelDensity, paint)
        canvas.drawLine(10, bottom, pixels + 10, bottom, paint)

        val space: Int = ViewUtils.dpToPixel((Int) (FONT_SIZE / 4))
        paint.setTextAlign(Paint.Align.RIGHT)
        canvas.drawText(info, pixels + 10 - space, bottom - 10 * pixelDensity, paint)
        paint.setTextAlign(Paint.Align.LEFT)
        canvas.drawText(scaled.right, pixels + 10 + space, bottom - 10 * pixelDensity, paint)

        scaleView.setImageBitmap(bitmap)
    }

    public Unit setNeedsInvertedColors(final Boolean needsInvertedColors) {
        this.needsInvertedColors = needsInvertedColors
        paint = null
        paintShadow = null
    }

    public Unit setImageView(final ImageView mapView) {
        this.scaleView = mapView
    }
}
