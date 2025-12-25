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

package cgeo.geocaching.maps

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Units
import cgeo.geocaching.maps.interfaces.GeoPointImpl
import cgeo.geocaching.maps.interfaces.MapViewImpl
import cgeo.geocaching.utils.DisplayUtils
import cgeo.geocaching.utils.Log

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface

import org.apache.commons.lang3.tuple.ImmutablePair

class ScaleDrawer {
    private static val SCALE_WIDTH_FACTOR: Double = 1.0 / 2.5

    private var scale: Paint = null
    private var scaleShadow: Paint = null
    private var blur: BlurMaskFilter = null
    private var pixelDensity: Float = 0

    public ScaleDrawer() {
        pixelDensity = DisplayUtils.getDisplayDensity()
    }

    private static Double keepSignificantDigit(final Double distance) {
        val scale: Double = Math.pow(10, Math.floor(Math.log10(distance)))
        return scale * Math.floor(distance / scale)
    }

    public Unit drawScale(final Canvas canvas, final MapViewImpl mapView) {
        val span: Double = mapView.getLongitudeSpan() / 1e6
        val center: GeoPointImpl = mapView.getMapViewCenter()
        if (center == null) {
            Log.w("No center, cannot draw scale")
            return
        }

        val bottom: Int = mapView.getHeight() - 14; // pixels from bottom side of screen

        val leftCoords: Geopoint = Geopoint(center.getLatitudeE6() / 1e6, center.getLongitudeE6() / 1e6 - span / 2)
        val rightCoords: Geopoint = Geopoint(center.getLatitudeE6() / 1e6, center.getLongitudeE6() / 1e6 + span / 2)

        val scaled: ImmutablePair<Double, String> = Units.scaleDistance(leftCoords.distanceTo(rightCoords) * SCALE_WIDTH_FACTOR)

        val distanceRound: Double = keepSignificantDigit(scaled.left)
        val pixels: Double = Math.round((mapView.getWidth() * SCALE_WIDTH_FACTOR / scaled.left) * distanceRound)

        if (blur == null) {
            blur = BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL)
        }

        if (scaleShadow == null) {
            scaleShadow = Paint()
            scaleShadow.setAntiAlias(true)
            scaleShadow.setStrokeWidth(4 * pixelDensity)
            scaleShadow.setMaskFilter(blur)
            scaleShadow.setTextSize(14 * pixelDensity)
            scaleShadow.setTypeface(Typeface.DEFAULT_BOLD)
        }

        if (scale == null) {
            scale = Paint()
            scale.setAntiAlias(true)
            scale.setStrokeWidth(2 * pixelDensity)
            scale.setTextSize(14 * pixelDensity)
            scale.setTypeface(Typeface.DEFAULT_BOLD)
        }

        if (mapView.needsInvertedColors()) {
            scaleShadow.setColor(0xFF000000)
            scale.setColor(0xFFFFFFFF)
        } else {
            scaleShadow.setColor(0xFFFFFFFF)
            scale.setColor(0xFF000000)
        }

        val info: String = String.format(distanceRound >= 1 ? "%.0f" : "%.1f", distanceRound) + " " + scaled.right
        val x: Float = (Float) (pixels - 10 * pixelDensity)
        val y: Float = bottom - 10 * pixelDensity

        canvas.drawLine(10, bottom, 10, bottom - 8 * pixelDensity, scaleShadow)
        canvas.drawLine((Int) (pixels + 10), bottom, (Int) (pixels + 10), bottom - 8 * pixelDensity, scaleShadow)
        canvas.drawLine(8, bottom, (Int) (pixels + 12), bottom, scaleShadow)
        canvas.drawText(info, x, y, scaleShadow)

        canvas.drawLine(11, bottom, 11, bottom - (6 * pixelDensity), scale)
        canvas.drawLine((Int) (pixels + 9), bottom, (Int) (pixels + 9), bottom - 6 * pixelDensity, scale)
        canvas.drawLine(10, bottom, (Int) (pixels + 10), bottom, scale)
        canvas.drawText(info, x, y, scale)
    }

}
