package cgeo.geocaching.maps;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.utils.DisplayUtils;
import cgeo.geocaching.utils.Log;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class ScaleDrawer {
    private static final double SCALE_WIDTH_FACTOR = 1.0 / 2.5;

    private Paint scale = null;
    private Paint scaleShadow = null;
    private BlurMaskFilter blur = null;
    private float pixelDensity = 0;

    public ScaleDrawer() {
        pixelDensity = DisplayUtils.getDisplayDensity();
    }

    private static double keepSignificantDigit(final double distance) {
        final double scale = Math.pow(10, Math.floor(Math.log10(distance)));
        return scale * Math.floor(distance / scale);
    }

    public void drawScale(final Canvas canvas, final MapViewImpl mapView) {
        final double span = mapView.getLongitudeSpan() / 1e6;
        final GeoPointImpl center = mapView.getMapViewCenter();
        if (center == null) {
            Log.w("No center, cannot draw scale");
            return;
        }

        final int bottom = mapView.getHeight() - 14; // pixels from bottom side of screen

        final Geopoint leftCoords = new Geopoint(center.getLatitudeE6() / 1e6, center.getLongitudeE6() / 1e6 - span / 2);
        final Geopoint rightCoords = new Geopoint(center.getLatitudeE6() / 1e6, center.getLongitudeE6() / 1e6 + span / 2);

        final ImmutablePair<Double, String> scaled = Units.scaleDistance(leftCoords.distanceTo(rightCoords) * SCALE_WIDTH_FACTOR);

        final double distanceRound = keepSignificantDigit(scaled.left);
        final double pixels = Math.round((mapView.getWidth() * SCALE_WIDTH_FACTOR / scaled.left) * distanceRound);

        if (blur == null) {
            blur = new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL);
        }

        if (scaleShadow == null) {
            scaleShadow = new Paint();
            scaleShadow.setAntiAlias(true);
            scaleShadow.setStrokeWidth(4 * pixelDensity);
            scaleShadow.setMaskFilter(blur);
            scaleShadow.setTextSize(14 * pixelDensity);
            scaleShadow.setTypeface(Typeface.DEFAULT_BOLD);
        }

        if (scale == null) {
            scale = new Paint();
            scale.setAntiAlias(true);
            scale.setStrokeWidth(2 * pixelDensity);
            scale.setTextSize(14 * pixelDensity);
            scale.setTypeface(Typeface.DEFAULT_BOLD);
        }

        if (mapView.needsInvertedColors()) {
            scaleShadow.setColor(0xFF000000);
            scale.setColor(0xFFFFFFFF);
        } else {
            scaleShadow.setColor(0xFFFFFFFF);
            scale.setColor(0xFF000000);
        }

        final String info = String.format(distanceRound >= 1 ? "%.0f" : "%.1f", distanceRound) + " " + scaled.right;
        final float x = (float) (pixels - 10 * pixelDensity);
        final float y = bottom - 10 * pixelDensity;

        canvas.drawLine(10, bottom, 10, bottom - 8 * pixelDensity, scaleShadow);
        canvas.drawLine((int) (pixels + 10), bottom, (int) (pixels + 10), bottom - 8 * pixelDensity, scaleShadow);
        canvas.drawLine(8, bottom, (int) (pixels + 12), bottom, scaleShadow);
        canvas.drawText(info, x, y, scaleShadow);

        canvas.drawLine(11, bottom, 11, bottom - (6 * pixelDensity), scale);
        canvas.drawLine((int) (pixels + 9), bottom, (int) (pixels + 9), bottom - 6 * pixelDensity, scale);
        canvas.drawLine(10, bottom, (int) (pixels + 10), bottom, scale);
        canvas.drawText(info, x, y, scale);
    }

}
