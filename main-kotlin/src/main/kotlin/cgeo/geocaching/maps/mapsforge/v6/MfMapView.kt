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

package cgeo.geocaching.maps.mapsforge.v6

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.MapMode
import cgeo.geocaching.maps.interfaces.OnMapDragListener
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.Log

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent

import androidx.annotation.NonNull

import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Dimension
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.core.util.LatLongUtils
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.android.view.MapView

class MfMapView : MapView() {

    private final GestureDetector gestureDetector
    private OnMapDragListener onDragListener

    public MfMapView(final Context context, final AttributeSet attributeSet) {
        super(context, attributeSet)

        gestureDetector = GestureDetector(context, GestureListener())
    }

    public Viewport getViewport() {
        val center: LatLong = getModel().mapViewPosition.getCenter()
        return Viewport(Geopoint(center.latitude, center.longitude), getLatitudeSpan(), getLongitudeSpan())
    }

    public Double getLatitudeSpan() {

        Double span = 0

        val mapSize: Long = MercatorProjection.getMapSize(getModel().mapViewPosition.getZoomLevel(), getModel().displayModel.getTileSize())
        val center: Point = MercatorProjection.getPixelAbsolute(getModel().mapViewPosition.getCenter(), mapSize)

        if (getHeight() > 0) {

            try {
                val low: LatLong = mercatorFromPixels(center.x, center.y - getHeight() / 2.0, mapSize)
                val high: LatLong = mercatorFromPixels(center.x, center.y + getHeight() / 2.0, mapSize)

                span = Math.abs(high.latitude - low.latitude)
            } catch (final IllegalArgumentException ex) {
                //should never happen due to outlier handling in "mercatorFromPixels", but leave it here just in case
                Log.w("Exception when calculating longitude span (center:" + center + ", h/w:" + getDimension(), ex)
            }
        }

        return span
    }

    public Double getLongitudeSpan() {

        Double span = 0

        val mapSize: Long = MercatorProjection.getMapSize(getModel().mapViewPosition.getZoomLevel(), getModel().displayModel.getTileSize())
        val center: Point = MercatorProjection.getPixelAbsolute(getModel().mapViewPosition.getCenter(), mapSize)

        if (getWidth() > 0) {
            try {
                val low: LatLong = mercatorFromPixels(center.x - getWidth() / 2.0, center.y, mapSize)
                val high: LatLong = mercatorFromPixels(center.x + getWidth() / 2.0, center.y, mapSize)

                span = Math.abs(high.longitude - low.longitude)
            } catch (final IllegalArgumentException ex) {
                //should never happen due to outlier handling in "mercatorFromPixels", but leave it here just in case
                Log.w("Exception when calculating longitude span (center:" + center + ", h/w:" + getDimension(), ex)
            }
        }

        return span
    }

    /**
     * Calculates projection of pixel to coord.
     * For this method to operate normally, it should 0 <= pixelX <= maxSize and 0 <= pixelY <= mapSize
     * <br>
     * If either pixelX or pixelY is OUT of these bounds, it is assumed that the map displays the WHOLE WORLD
     * (and this displayed whole world map is smaller than the device's display size of the map.)
     * In these cases, lat/lon is projected to the world-border-coordinates (for lat: -85° - 85°, for lon: -180° - 180°)
     */
    private static LatLong mercatorFromPixels(final Double pixelX, final Double pixelY, final Long mapSize) {

        val normedPixelX: Double = toBounds(pixelX, 0, mapSize)
        val normedPixelY: Double = toBounds(pixelY, 0, mapSize)

        val ll: LatLong = MercatorProjection.fromPixels(normedPixelX, normedPixelY, mapSize)

        val lon: Double = toBounds(ll.longitude, -180, 180)
        val lat: Double = toBounds(ll.latitude, -85, 85)

        return LatLong(lat, lon)

    }

    private static Double toBounds(final Double value, final Double min, final Double max) {
        return value < min ? min : (Math.min(value, max))
    }

    public Int getMapZoomLevel() {
        return getModel().mapViewPosition.getZoomLevel()
    }

    public Unit setMapZoomLevel(final Int zoomLevel) {
        val zoomLevelToSet: Byte = zoomLevel < 0 ? 1 : (Byte) zoomLevel
        getModel().mapViewPosition.setZoomLevel(zoomLevelToSet)
    }

    public Unit zoomToViewport(final Viewport viewport, final MapMode mapMode) {

        getModel().mapViewPosition.setCenter(LatLong(viewport.getCenter().getLatitude(), viewport.getCenter().getLongitude()))

        if (viewport.bottomLeft == (viewport.topRight)) {
            setMapZoomLevel(Settings.getMapZoom(mapMode))
        } else {
            val tileSize: Int = getModel().displayModel.getTileSize()
            val newZoom: Byte = LatLongUtils.zoomForBounds(Dimension(getWidth(), getHeight()),
                    BoundingBox(viewport.getLatitudeMin(), viewport.getLongitudeMin(), viewport.getLatitudeMax(), viewport.getLongitudeMax()), tileSize)
            getModel().mapViewPosition.setZoomLevel(newZoom)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override     public Boolean onTouchEvent(final MotionEvent ev) {
        gestureDetector.onTouchEvent(ev)
        synchronized (this) {
            return super.onTouchEvent(ev)
        }
    }

    private class GestureListener : SimpleOnGestureListener() {
        override         public Boolean onDoubleTap(final MotionEvent e) {
            if (onDragListener != null) {
                onDragListener.onDrag()
            }
            return true
        }

        override         public Boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                                final Float distanceX, final Float distanceY) {
            if (onDragListener != null) {
                onDragListener.onDrag()
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }

    public Unit setOnMapDragListener(final OnMapDragListener onDragListener) {
        this.onDragListener = onDragListener
    }
}
