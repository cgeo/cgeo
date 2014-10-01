package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.R;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.Units;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.maps.interfaces.GeneralOverlay;
import cgeo.geocaching.maps.interfaces.MapProjectionImpl;
import cgeo.geocaching.maps.interfaces.MapViewImpl;
import cgeo.geocaching.maps.interfaces.OverlayImpl;
import cgeo.geocaching.settings.Settings;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class DistanceOverlay implements GeneralOverlay {
    private Location currentLoc = null;
    private Geopoint currentCoords;
    private final Geopoint destinationCoords;
    private float currentHeading;

    private static Bitmap compassArrow;
    private static int compassArrowWidth = 0;
    private static int compassArrowHeight = 0;

    private Paint paintBox = null;
    private Paint paintBoxShadow = null;
    private Paint paintText = null;
    private Paint paintCompass = null;
    private BlurMaskFilter blurBoxShadow = null;

    private float pixelDensity = 0;
    private final float boxWidth, boxHeight, boxCornerRadius, boxShadowSize, boxPadding;
    private final float textHeight;

    private OverlayImpl ovlImpl = null;

    public DistanceOverlay(final OverlayImpl ovlImpl, final Geopoint coords, final String geocode) {
        this.ovlImpl = ovlImpl;

        if (coords == null) {
            final Viewport bounds = DataStore.getBounds(geocode);
            this.destinationCoords = bounds.center;
        } else {
            this.destinationCoords = coords;
        }

        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        if (compassArrow == null) {
            compassArrow = BitmapFactory.decodeResource(
                    CgeoApplication.getInstance().getResources(),
                    Settings.isLightSkin() ? R.drawable.compass_arrow_mini_black : R.drawable.compass_arrow_mini_white
                    );
            compassArrowWidth = compassArrow.getWidth();
            compassArrowHeight = compassArrow.getWidth();
        }

        pixelDensity = metrics.density;

        boxPadding = 2;
        boxWidth = 100 * pixelDensity + compassArrowWidth + 3 * boxPadding;
        boxHeight = 30 * pixelDensity + 2 * boxPadding;
        boxCornerRadius = 5 * pixelDensity;
        boxShadowSize = 1 * pixelDensity;
        textHeight = 20 * pixelDensity;
    }

    public void setCoordinates(final Location coordinatesIn) {
        currentLoc = coordinatesIn;
        currentCoords = new Geopoint(currentLoc);
    }

    public void setHeading(final float bearingNow) {
        currentHeading = bearingNow;
    }

    @Override
    public void draw(final Canvas canvas, final MapViewImpl mapView, final boolean shadow) {
        drawInternal(canvas, mapView.getMapProjection(), mapView);

    }

    @Override
    public void drawOverlayBitmap(final Canvas canvas, final Point drawPosition, final MapProjectionImpl projection, final byte drawZoomLevel) {
        drawInternal(canvas, projection, getOverlayImpl().getMapViewImpl());
    }

    private void drawInternal(final Canvas canvas, @SuppressWarnings("unused") final MapProjectionImpl projection, final MapViewImpl mapView) {
        if (currentLoc == null) {
            return;
        }

        if (blurBoxShadow == null) {
            blurBoxShadow = new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL);
        }

        if (paintBoxShadow == null) {
            paintBoxShadow = new Paint();
            paintBoxShadow.setAntiAlias(true);
            paintBoxShadow.setMaskFilter(blurBoxShadow);
        }

        if (paintBox == null) {
            paintBox = new Paint();
            paintBox.setAntiAlias(true);
        }

        if (paintText == null) {
            paintText = new Paint();
            paintText.setAntiAlias(true);
            paintText.setTextAlign(Paint.Align.LEFT);
            paintText.setTypeface(Typeface.DEFAULT_BOLD);
        }

        if (paintCompass == null) {
            paintCompass = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintCompass.setDither(true);
            paintCompass.setFilterBitmap(true);
        }

        if (mapView.needsInvertedColors()) {
            paintBoxShadow.setColor(0xFF000000);
            paintBox.setColor(0xFFFFFFFF);
            paintText.setColor(0xFF000000);
        } else {
            paintBoxShadow.setColor(0xFFFFFFFF);
            paintBox.setColor(0xFF000000);
            paintText.setColor(0xFFFFFFFF);
        }


        /* Calculate positions */
        final float boxX = mapView.getWidth() - boxWidth;
        final float boxY = 0;

        final float distance = currentCoords.distanceTo(destinationCoords);
        final String text = Units.getDistanceFromKilometers(distance);

        final float compassArrowX = boxX + boxPadding + (compassArrowWidth / 2);
        final float compassArrowY = boxY + (boxHeight - compassArrowHeight) / 2;
        final float compassArrowCenterX = compassArrowX + (compassArrowHeight / 2);
        final float compassArrowCenterY = compassArrowY + (compassArrowWidth / 2);
        final float compassRotation = currentCoords.bearingTo(destinationCoords) - currentHeading;

        final float maxTextWidth = boxWidth - 3 * boxPadding - compassArrowWidth;
        final Rect textBounds = new Rect();
        paintText.setTextSize(textHeight);
        paintText.getTextBounds(text, 0, text.length(), textBounds);
        while (textBounds.height() > maxTextWidth) {
            paintText.setTextSize(paintText.getTextSize() - 1);
            paintText.getTextBounds(text, 0, text.length(), textBounds);
        }

        final float textX = (boxWidth - 3 * boxPadding - compassArrowWidth - textBounds.width()) / 2 + boxX + 2 * boxPadding + compassArrowWidth;
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

        /* Paint direction arrow */
        canvas.save();
        canvas.rotate(compassRotation, compassArrowCenterX, compassArrowCenterY);
        canvas.drawBitmap(compassArrow, compassArrowX, compassArrowY, paintCompass);
        canvas.restore();

        /* Paint distance */
        canvas.drawText(text, textX, textY, paintText);
    }

    @Override
    public OverlayImpl getOverlayImpl() {
        return this.ovlImpl;
    }

}
