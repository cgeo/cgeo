package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapMode;
import cgeo.geocaching.maps.interfaces.OnMapDragListener;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.view.MapView;

public class MfMapView extends MapView {

    private final GestureDetector gestureDetector;
    private OnMapDragListener onDragListener;

    public MfMapView(final Context context, final AttributeSet attributeSet) {
        super(context, attributeSet);

        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public Viewport getViewport() {
        final LatLong center = getModel().mapViewPosition.getCenter();
        return new Viewport(new Geopoint(center.latitude, center.longitude), getLatitudeSpan(), getLongitudeSpan());
    }

    public double getLatitudeSpan() {

        double span = 0;

        final long mapSize = MercatorProjection.getMapSize(getModel().mapViewPosition.getZoomLevel(), getModel().displayModel.getTileSize());
        final Point center = MercatorProjection.getPixelAbsolute(getModel().mapViewPosition.getCenter(), mapSize);

        if (getHeight() > 0) {

            try {
                final LatLong low = mercatorFromPixels(center.x, center.y - getHeight() / 2.0, mapSize);
                final LatLong high = mercatorFromPixels(center.x, center.y + getHeight() / 2.0, mapSize);

                span = Math.abs(high.latitude - low.latitude);
            } catch (final IllegalArgumentException ex) {
                //should never happen due to outlier handling in "mercatorFromPixels", but leave it here just in case
                Log.w("Exception when calculating longitude span (center:" + center + ", h/w:" + getDimension(), ex);
            }
        }

        return span;
    }

    public double getLongitudeSpan() {

        double span = 0;

        final long mapSize = MercatorProjection.getMapSize(getModel().mapViewPosition.getZoomLevel(), getModel().displayModel.getTileSize());
        final Point center = MercatorProjection.getPixelAbsolute(getModel().mapViewPosition.getCenter(), mapSize);

        if (getWidth() > 0) {
            try {
                final LatLong low = mercatorFromPixels(center.x - getWidth() / 2.0, center.y, mapSize);
                final LatLong high = mercatorFromPixels(center.x + getWidth() / 2.0, center.y, mapSize);

                span = Math.abs(high.longitude - low.longitude);
            } catch (final IllegalArgumentException ex) {
                //should never happen due to outlier handling in "mercatorFromPixels", but leave it here just in case
                Log.w("Exception when calculating longitude span (center:" + center + ", h/w:" + getDimension(), ex);
            }
        }

        return span;
    }

    /**
     * Calculates projection of pixel to coord.
     * For this method to operate normally, it should 0 <= pixelX <= maxSize and 0 <= pixelY <= mapSize
     *
     * If either pixelX or pixelY is OUT of these bounds, it is assumed that the map displays the WHOLE WORLD
     * (and this displayed whole world map is smaller than the device's display size of the map.)
     * In these cases, lat/lon is projected to the world-border-coordinates (for lat: -85° - 85°, for lon: -180° - 180°)
     */
    private static LatLong mercatorFromPixels(final double pixelX, final double pixelY, final long mapSize) {

        final double normedPixelX = toBounds(pixelX, 0, mapSize);
        final double normedPixelY = toBounds(pixelY, 0, mapSize);

        final LatLong ll = MercatorProjection.fromPixels(normedPixelX, normedPixelY, mapSize);

        final double lon = toBounds(ll.longitude, -180, 180);
        final double lat = toBounds(ll.latitude, -85, 85);

        return new LatLong(lat, lon);

    }

    private static double toBounds(final double value, final double min, final double max) {
        return value < min ? min : (value > max ? max : value);
    }

    public int getMapZoomLevel() {
        return getModel().mapViewPosition.getZoomLevel();
    }

    public void setMapZoomLevel(final int zoomLevel) {
        final byte zoomLevelToSet = zoomLevel < 0 ? 1 : (byte) zoomLevel;
        getModel().mapViewPosition.setZoomLevel(zoomLevelToSet);
    }

    public void zoomToViewport(final Viewport viewport, final MapMode mapMode) {

        getModel().mapViewPosition.setCenter(new LatLong(viewport.getCenter().getLatitude(), viewport.getCenter().getLongitude()));

        if (viewport.bottomLeft.equals(viewport.topRight)) {
            setMapZoomLevel(Settings.getMapZoom(mapMode));
        } else {
            final int tileSize = getModel().displayModel.getTileSize();
            final byte newZoom = LatLongUtils.zoomForBounds(new Dimension(getWidth(), getHeight()),
                    new BoundingBox(viewport.getLatitudeMin(), viewport.getLongitudeMin(), viewport.getLatitudeMax(), viewport.getLongitudeMax()), tileSize);
            getModel().mapViewPosition.setZoomLevel(newZoom);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        synchronized (this) {
            return super.onTouchEvent(ev);
        }
    }

    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(final MotionEvent e) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return true;
        }

        @Override
        public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                                final float distanceX, final float distanceY) {
            if (onDragListener != null) {
                onDragListener.onDrag();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    public void setOnMapDragListener(final OnMapDragListener onDragListener) {
        this.onDragListener = onDragListener;
    }
}
