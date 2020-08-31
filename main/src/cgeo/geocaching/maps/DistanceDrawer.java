package cgeo.geocaching.maps;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.utils.DisplayUtils;

import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.util.DisplayMetrics;

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
    private float distance = 0.0f;
    private float realDistance = 0.0f;
    private boolean showBothDistances = false;
    private float routeDistance = 0.0f;

    private int nextLine = 0;

    private static final char STRAIGHT_LINE_SYMBOL = (char) 0x007C;
    private static final char WAVY_LINE_SYMBOL = (char) 0x2307;

    public DistanceDrawer(final MapViewImpl mapView, final Geopoint destinationCoords, final boolean showBothDistances) {
        this.destinationCoords = destinationCoords;
        this.showBothDistances = showBothDistances;

        final DisplayMetrics metrics = DisplayUtils.getDisplayMetrics();
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

        distance = null != destinationCoords ? currentCoords.distanceTo(destinationCoords) : 0.0f;
        distanceText = null != destinationCoords ? Units.getDistanceFromKilometers(distance) : null;
    }

    public Geopoint getDestinationCoords() {
        return destinationCoords;
    }

    public void setRealDistance(final float realDistance) {
        this.realDistance = realDistance;
    }

    public void setRouteDistance(final float routeDistance) {
        this.routeDistance = routeDistance;
    }

    private void setText(final Canvas canvas, final char symbol, final String text) {
        if (text == null) {
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
        paintText.getTextBounds(text, 0, text.length(), textBounds);
        while (textBounds.height() > maxTextWidth) {
            paintText.setTextSize(paintText.getTextSize() - 1);
            paintText.getTextBounds(text, 0, text.length(), textBounds);
        }

        final float textX = (boxWidth - 3 * boxPadding - textBounds.width()) / 2 + boxX + 2 * boxPadding;
        final float textY = (boxHeight + textBounds.height()) / 2 + boxY;
        final float yDelta = nextLine++ * (boxY + boxHeight + boxCornerRadius);

        /* Paint background box */
        canvas.drawRoundRect(
                new RectF(
                        boxX - boxShadowSize, boxY - boxShadowSize - boxCornerRadius + yDelta,
                        boxX + boxWidth + boxShadowSize + boxCornerRadius, boxY + boxHeight + boxShadowSize + yDelta
                ),
                boxCornerRadius, boxCornerRadius,
                paintBoxShadow
        );
        canvas.drawRoundRect(
                new RectF(
                        boxX, boxY - boxCornerRadius + yDelta,
                        boxX + boxWidth + boxCornerRadius, boxY + boxHeight + yDelta
                ),
                boxCornerRadius, boxCornerRadius,
                paintBox
        );

        /* Paint distance */
        if (symbol != ' ') {
            canvas.drawText(Character.toString(symbol), boxX + 2 * boxPadding, textY + yDelta, paintText);
        }
        canvas.drawText(text, textX + 5 * boxPadding, textY + yDelta, paintText);
    }

    public void drawDistance(final Canvas canvas) {
        nextLine = 0;
        if (showBothDistances && realDistance != 0.0f && distance != realDistance) {
            setText(canvas, STRAIGHT_LINE_SYMBOL, distanceText);
            setText(canvas, WAVY_LINE_SYMBOL, Units.getDistanceFromKilometers(realDistance));
        } else {
            setText(canvas, ' ', realDistance != 0.0f && distance != realDistance ? Units.getDistanceFromKilometers(realDistance) : distanceText);
        }
        if (routeDistance != 0.0f) {
            setText(canvas, ' ', Units.getDistanceFromKilometers(routeDistance));
        }
    }
}
