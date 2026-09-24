package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.DisplayUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.widget.ImageView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import org.apache.commons.lang3.tuple.ImmutableTriple;

public class ScaleDrawer {
    private static final double SCALE_WIDTH_FACTOR = 0.3d;
    private static final double FONT_SIZE = 14.0;

    private Paint paint = null;
    private Paint paintShadow = null;
    private float pixelDensity = 0;

    private LatLngBounds lastBounds = null;
    private boolean needsInvertedColors = false;
    private ImageView scaleView = null;

    public ScaleDrawer() {
        pixelDensity = DisplayUtils.getDisplayDensity();
    }

    public void drawScale(final GoogleMap mMap, final LatLngBounds lastBounds) {
        if (scaleView == null || this.lastBounds == lastBounds) {
            return;
        }
        if (scaleView.getWidth() <= 0 || scaleView.getHeight() <= 0) {
            return;
        }
        this.lastBounds = lastBounds;

        // measure the distance covered by the maximum scale length horizontally on screen, starting at the map center
        // (using screen coordinates keeps this independent of map rotation, tilt and antimeridian crossing)
        final Projection projection = mMap.getProjection();
        final int maxPixels = (int) (scaleView.getWidth() * SCALE_WIDTH_FACTOR);
        final LatLng center = mMap.getCameraPosition().target;
        final Point centerOnScreen = projection.toScreenLocation(center);
        final LatLng right = projection.fromScreenLocation(new Point(centerOnScreen.x + maxPixels, centerOnScreen.y));
        final double maxDistance = new Geopoint(center.latitude, center.longitude).distanceTo(new Geopoint(right.latitude, right.longitude));
        if (maxDistance <= 0 || Double.isNaN(maxDistance)) {
            return;
        }

        final ImmutableTriple<Double, String, Float> scaled = Units.scaleDistanceWithFactor(maxDistance);
        final double distanceRound = roundToNiceValue(scaled.left);
        final int pixels = (int) Math.round(maxPixels * distanceRound / scaled.left);


        if (paint == null) {
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStrokeWidth(2 * pixelDensity);
            paint.setTextSize((int) (FONT_SIZE * pixelDensity));
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setColor(needsInvertedColors ? Color.WHITE : Color.BLACK);
            paint.setShadowLayer(5.0f, 2.0f, 3.0f, needsInvertedColors ? Color.BLACK : Color.WHITE);
        }

        if (paintShadow == null) {
            paintShadow = new Paint(paint);
            paintShadow.setStrokeWidth(4 * pixelDensity);
            paintShadow.setColor(needsInvertedColors ? Color.BLACK : Color.WHITE);
            paintShadow.clearShadowLayer();
        }

        final String info = String.format(distanceRound >= 1 ? "%.0f" : "%.1f", distanceRound);
        final int bottom = scaleView.getHeight() - 10;
        final Bitmap bitmap = Bitmap.createBitmap(scaleView.getWidth(), scaleView.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);

        canvas.drawLine(10, bottom + 1, 10, bottom - 8 * pixelDensity, paintShadow);
        canvas.drawLine(pixels + 10, bottom + 1, pixels + 10, bottom - 8 * pixelDensity, paintShadow);
        canvas.drawLine(8, bottom, pixels + 12, bottom, paintShadow);

        canvas.drawLine(11, bottom, 11, bottom - (6 * pixelDensity), paint);
        canvas.drawLine(pixels + 9, bottom, pixels + 9, bottom - 6 * pixelDensity, paint);
        canvas.drawLine(10, bottom, pixels + 10, bottom, paint);

        final int space = ViewUtils.dpToPixel((int) (FONT_SIZE / 4));
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(info, pixels + 10 - space, bottom - 10 * pixelDensity, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(scaled.middle, pixels + 10 + space, bottom - 10 * pixelDensity, paint);

        scaleView.setImageBitmap(bitmap);
    }

    /**
     * rounds the given value down to a "nice" value:
     * 1..9 in steps of 1, 10..95 in steps of 5, 100..950 in steps of 50 and so on
     * (values below 1 in steps of 0.1, 0.01, ...)
     */
    static double roundToNiceValue(final double value) {
        if (value <= 0) {
            return 0;
        }
        final double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        final double step = magnitude < 10 ? magnitude : magnitude / 2;
        return step * Math.floor(value / step + 1e-9); // epsilon compensates floating point errors (e.g. 0.3 / 0.1 = 2.999...)
    }

    public void setNeedsInvertedColors(final boolean needsInvertedColors) {
        this.needsInvertedColors = needsInvertedColors;
        paint = null;
        paintShadow = null;
    }

    public void setImageView(final ImageView scaleView) {
        this.scaleView = scaleView;
    }
}
