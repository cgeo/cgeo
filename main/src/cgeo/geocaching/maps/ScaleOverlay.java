package cgeo.geocaching.maps;

import cgeo.geocaching.Settings;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.IConversion;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.GeoPointImpl;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;

import android.app.Activity;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.DisplayMetrics;

public class ScaleOverlay implements GeneralOverlay {

    private static final double SCALE_WIDTH_FACTOR = 1.0 / 2.5;

    private Paint scale = null;
    private Paint scaleShadow = null;
    private BlurMaskFilter blur = null;
    private float pixelDensity = 0;
    private OverlayImpl ovlImpl = null;

    public ScaleOverlay(Activity activity, OverlayImpl overlayImpl) {
        this.ovlImpl = overlayImpl;

        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        pixelDensity = metrics.density;
    }

    @Override
    public void drawOverlayBitmap(Canvas canvas, Point drawPosition,
            MapProjectionImpl projection, byte drawZoomLevel) {
        drawInternal(canvas, getOverlayImpl().getMapViewImpl());
    }

    @Override
    public void draw(Canvas canvas, MapViewImpl mapView, boolean shadow) {
        drawInternal(canvas, mapView);
    }

    static private double keepSignificantDigit(final double distance) {
        final double scale = Math.pow(10, Math.floor(Math.log10(distance)));
        return scale * Math.floor(distance / scale);
    }

    private void drawInternal(Canvas canvas, MapViewImpl mapView) {

        final double span = mapView.getLongitudeSpan() / 1e6;
        final GeoPointImpl center = mapView.getMapViewCenter();

        final int bottom = mapView.getHeight() - 14; // pixels from bottom side of screen

        final Geopoint leftCoords = new Geopoint(center.getLatitudeE6() / 1e6, center.getLongitudeE6() / 1e6 - span / 2);
        final Geopoint rightCoords = new Geopoint(center.getLatitudeE6() / 1e6, center.getLongitudeE6() / 1e6 + span / 2);

        String units;
        double distance = leftCoords.distanceTo(rightCoords) * SCALE_WIDTH_FACTOR;
        if (Settings.isUseMetricUnits()) {
            if (distance >= 1) {
                units = "km";
            } else {
                distance *= 1000;
                units = "m";
            }
        } else {
            distance /= IConversion.MILES_TO_KILOMETER;
            if (distance >= 1) {
                units = "mi";
            } else {
                distance *= 5280;
                units = "ft";
            }
        }

        final double distanceRound = keepSignificantDigit(distance);
        final double pixels = Math.round((mapView.getWidth() * SCALE_WIDTH_FACTOR / distance) * distanceRound);

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

        canvas.drawLine(10, bottom, 10, (bottom - (8 * pixelDensity)), scaleShadow);
        canvas.drawLine((int) (pixels + 10), bottom, (int) (pixels + 10), (bottom - (8 * pixelDensity)), scaleShadow);
        canvas.drawLine(8, bottom, (int) (pixels + 12), bottom, scaleShadow);
        canvas.drawText(String.format("%.0f", distanceRound) + " " + units, (float) (pixels - (10 * pixelDensity)), (bottom - (10 * pixelDensity)), scaleShadow);

        canvas.drawLine(11, bottom, 11, (bottom - (6 * pixelDensity)), scale);
        canvas.drawLine((int) (pixels + 9), bottom, (int) (pixels + 9), (bottom - (6 * pixelDensity)), scale);
        canvas.drawLine(10, bottom, (int) (pixels + 10), bottom, scale);
        canvas.drawText(String.format("%.0f", distanceRound) + " " + units, (float) (pixels - (10 * pixelDensity)), (bottom - (10 * pixelDensity)), scale);
    }

    @Override
    public OverlayImpl getOverlayImpl() {
        return ovlImpl;
    }
}
