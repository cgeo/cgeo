package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.DisplayUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.widget.ImageView;

import com.google.android.gms.maps.model.LatLngBounds;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class ScaleDrawer {
    private static final double SCALE_WIDTH_FACTOR = 1.0 / 2.5;
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

    public void drawScale(final LatLngBounds lastBounds) {
        if (scaleView == null || this.lastBounds == lastBounds) {
            return;
        }
        this.lastBounds = lastBounds;

        final double centerLon = (lastBounds.southwest.longitude + lastBounds.northeast.longitude) / 2;
        final ImmutablePair<Double, String> scaled = Units.scaleDistance(new Geopoint(lastBounds.southwest.latitude, centerLon).distanceTo(new Geopoint(lastBounds.northeast.latitude, centerLon)) * SCALE_WIDTH_FACTOR);
        final double scale = Math.pow(10, Math.floor(Math.log10(scaled.left)));
        final double distanceRound = scale * Math.floor(scaled.left / scale);
        final int pixels = (int) Math.round((scaleView.getWidth() * SCALE_WIDTH_FACTOR / scaled.left) * distanceRound);

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
        canvas.drawText(scaled.right, pixels + 10 + space, bottom - 10 * pixelDensity, paint);

        scaleView.setImageBitmap(bitmap);
    }

    public void setNeedsInvertedColors(final boolean needsInvertedColors) {
        this.needsInvertedColors = needsInvertedColors;
        paint = null;
        paintShadow = null;
    }

    public void setImageView(final ImageView mapView) {
        this.scaleView = mapView;
    }
}
