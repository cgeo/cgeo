package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.interfaces.MapViewImpl;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class DistanceDrawer {
    private final Geopoint destinationCoords;

    private Paint paintBox = null;
    private Paint paintBoxShadow = null;
    private Paint paintText = null;
    private BlurMaskFilter blurBoxShadow = null;

    private final boolean needsInvertedColors;
    private final float boxWidth, boxHeight, boxCornerRadius, boxShadowSize, boxPadding;
    private final float textHeight, maxTextWidth;
    private final float boxX, boxY;

    private String distanceText = null;

    public DistanceDrawer(final MapViewImpl mapView, final Geopoint destinationCoords) {
        this.destinationCoords = destinationCoords;

        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        final float pixelDensity = metrics.density;

        boxPadding = 2;
        boxWidth = 100 * pixelDensity + 3 * boxPadding;
        boxHeight = 30 * pixelDensity + 2 * boxPadding;
        boxCornerRadius = 5 * pixelDensity;
        boxShadowSize = 1 * pixelDensity;
        textHeight = 20 * pixelDensity;

        needsInvertedColors = mapView.needsInvertedColors();
        boxX = metrics.widthPixels - boxWidth;
        boxY = 0;

        maxTextWidth = boxWidth - 3 * boxPadding;
    }

    public void setCoordinates(final Location location) {
        final Geopoint currentCoords = new Geopoint(location);

        final float distance = currentCoords.distanceTo(destinationCoords);
        distanceText = Units.getDistanceFromKilometers(distance);
    }

    public Geopoint getDestinationCoords() {
        return destinationCoords;
    }

    public void drawDistance(final Canvas canvas) {
        if (distanceText == null) {
            return;
        }

        if (blurBoxShadow == null) {
            blurBoxShadow = new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL);

            paintBoxShadow = new Paint();
            paintBoxShadow.setAntiAlias(true);
            paintBoxShadow.setMaskFilter(blurBoxShadow);

            paintBox = new Paint();
            paintBox.setAntiAlias(true);

            paintText = new Paint();
            paintText.setAntiAlias(true);
            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setTypeface(Typeface.DEFAULT_BOLD);

            final int transparency = 0x80000000;
            if (needsInvertedColors) {
                paintBoxShadow.setColor(0x000000 | transparency);
                paintBox.setColor(0xFFFFFF | transparency);
                paintText.setColor(0xFF000000);
            } else {
                paintBoxShadow.setColor(0xFFFFFF | transparency);
                paintBox.setColor(0x000000 | transparency);
                paintText.setColor(0xFFFFFFFF);
            }
        }

        /* Calculate text size */
        final Rect textBounds = new Rect();
        paintText.setTextSize(textHeight);
        paintText.getTextBounds(distanceText, 0, distanceText.length(), textBounds);
        while (textBounds.height() > maxTextWidth) {
            paintText.setTextSize(paintText.getTextSize() - 1);
            paintText.getTextBounds(distanceText, 0, distanceText.length(), textBounds);
        }

        final float textX = (boxWidth - 3 * boxPadding - textBounds.width()) / 2 + boxX + 2 * boxPadding;
        final float textY = (boxHeight + textBounds.height()) / 2 + boxY;

        /* Paint background box */
        canvas.drawRoundRect(
                new RectF(
                        boxX - boxShadowSize, boxY - boxShadowSize - boxCornerRadius,
                        boxX + boxWidth + boxShadowSize + boxCornerRadius, boxY + boxHeight + boxShadowSize
                ),
                boxCornerRadius, boxCornerRadius,
                paintBoxShadow
                );
        canvas.drawRoundRect(
                new RectF(
                        boxX, boxY - boxCornerRadius,
                        boxX + boxWidth + boxCornerRadius, boxY + boxHeight
                ),
                boxCornerRadius, boxCornerRadius,
                paintBox
                );

        /* Paint distance */
        canvas.drawText(distanceText, textX, textY, paintText);
    }
}
