package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.PeriodicHandler;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.view.View;

public class CompassView extends View {

    private Context context = null;
    private Bitmap compassUnderlay = null;
    private Bitmap compassRose = null;
    private Bitmap compassArrow = null;
    private Bitmap compassOverlay = null;
    /**
     * North direction currently SHOWN on compass (not measured)
     */
    private float azimuthShown = 0;
    /**
     * cache direction currently SHOWN on compass (not measured)
     */
    private float cacheHeadingShown = 0;
    /**
     * cache direction measured from device, or 0.0
     */
    private float cacheHeadingMeasured = 0;
    /**
     * North direction measured from device, or 0.0
     */
    private float northMeasured = 0;
    private PaintFlagsDrawFilter setfil = null;
    private PaintFlagsDrawFilter remfil = null;
    private int compassUnderlayWidth = 0;
    private int compassUnderlayHeight = 0;
    private int compassRoseWidth = 0;
    private int compassRoseHeight = 0;
    private int compassArrowWidth = 0;
    private int compassArrowHeight = 0;
    private int compassOverlayWidth = 0;
    private int compassOverlayHeight = 0;
    private boolean initialDisplay;
    private final RedrawHandler redrawHandler = new RedrawHandler();

    public CompassView(Context contextIn) {
        super(contextIn);
        context = contextIn;
    }

    public CompassView(Context contextIn, AttributeSet attrs) {
        super(contextIn, attrs);
        context = contextIn;
    }

    @Override
    public void onAttachedToWindow() {
        compassUnderlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_underlay);
        compassRose = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_rose);
        compassArrow = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_arrow);
        compassOverlay = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_overlay);

        compassUnderlayWidth = compassUnderlay.getWidth();
        compassUnderlayHeight = compassUnderlay.getWidth();
        compassRoseWidth = compassRose.getWidth();
        compassRoseHeight = compassRose.getWidth();
        compassArrowWidth = compassArrow.getWidth();
        compassArrowHeight = compassArrow.getWidth();
        compassOverlayWidth = compassOverlay.getWidth();
        compassOverlayHeight = compassOverlay.getWidth();

        setfil = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
        remfil = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);

        initialDisplay = true;
        redrawHandler.start();
    }

    @Override
    public void onDetachedFromWindow() {
        redrawHandler.stop();

        if (compassUnderlay != null) {
            compassUnderlay.recycle();
        }

        if (compassRose != null) {
            compassRose.recycle();
        }

        if (compassArrow != null) {
            compassArrow.recycle();
        }

        if (compassOverlay != null) {
            compassOverlay.recycle();
        }
    }

    public synchronized void updateNorth(float northHeadingIn, float cacheHeadingIn) {
        if (initialDisplay) {
            // We will force the compass to move brutally if this is the first
            // update since it is visible.
            azimuthShown = northHeadingIn;
            cacheHeadingShown = cacheHeadingIn;

            // it may take some time to get an initial direction measurement for the device
            if (northHeadingIn != 0.0) {
                initialDisplay = false;
            }
        }
        northMeasured = northHeadingIn;
        cacheHeadingMeasured = cacheHeadingIn;
    }

    /**
     * Compute the new value, moving by small increments.
     *
     * @param goal
     *            the goal to reach
     * @param actual
     *            the actual value
     * @return the new value
     */
    static protected float smoothUpdate(float goal, float actual) {
        final float diff = AngleUtils.difference(actual, goal);

        float offset = 0;

        // If the difference is smaller than 1 degree, do nothing as it
        // causes the arrow to vibrate. Round away from 0.
        if (diff > 1.0) {
            offset = (float) Math.ceil(diff / 10.0); // for larger angles, rotate faster
        } else if (diff < 1.0) {
            offset = (float) Math.floor(diff / 10.0);
        }

        return AngleUtils.normalize(actual + offset);
    }

    private class RedrawHandler extends PeriodicHandler {

        public RedrawHandler() {
            super(40);
        }

        @Override
        public void act() {
            final float newAzimuthShown = smoothUpdate(northMeasured, azimuthShown);
            final float newCacheHeadingShown = smoothUpdate(cacheHeadingMeasured, cacheHeadingShown);
            if (Math.abs(AngleUtils.difference(azimuthShown, newAzimuthShown)) >= 2 ||
                    Math.abs(AngleUtils.difference(cacheHeadingShown, newCacheHeadingShown)) >= 2) {
                synchronized(CompassView.this) {
                    azimuthShown = newAzimuthShown;
                    cacheHeadingShown = newCacheHeadingShown;
                }
                invalidate();
            }
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        // use local synchronized variables to avoid them being changed from the device during drawing
        float azimuthDrawn;
        float headingDrawn;

        synchronized (this) {
            azimuthDrawn = azimuthShown;
            headingDrawn = cacheHeadingShown;
        }

        float azimuthTemp = azimuthDrawn;
        final float azimuthRelative = AngleUtils.normalize(azimuthTemp - headingDrawn);

        // compass margins
        int canvasCenterX = (compassRoseWidth / 2) + ((getWidth() - compassRoseWidth) / 2);
        int canvasCenterY = (compassRoseHeight / 2) + ((getHeight() - compassRoseHeight) / 2);

        int marginLeftTemp;
        int marginTopTemp;

        super.onDraw(canvas);

        canvas.save();
        canvas.setDrawFilter(setfil);

        marginLeftTemp = (getWidth() - compassUnderlayWidth) / 2;
        marginTopTemp = (getHeight() - compassUnderlayHeight) / 2;

        canvas.drawBitmap(compassUnderlay, marginLeftTemp, marginTopTemp, null);

        marginLeftTemp = (getWidth() - compassRoseWidth) / 2;
        marginTopTemp = (getHeight() - compassRoseHeight) / 2;

        canvas.rotate(-azimuthTemp, canvasCenterX, canvasCenterY);
        canvas.drawBitmap(compassRose, marginLeftTemp, marginTopTemp, null);
        canvas.rotate(azimuthTemp, canvasCenterX, canvasCenterY);

        marginLeftTemp = (getWidth() - compassArrowWidth) / 2;
        marginTopTemp = (getHeight() - compassArrowHeight) / 2;

        canvas.rotate(-azimuthRelative, canvasCenterX, canvasCenterY);
        canvas.drawBitmap(compassArrow, marginLeftTemp, marginTopTemp, null);
        canvas.rotate(azimuthRelative, canvasCenterX, canvasCenterY);

        marginLeftTemp = (getWidth() - compassOverlayWidth) / 2;
        marginTopTemp = (getHeight() - compassOverlayHeight) / 2;

        canvas.drawBitmap(compassOverlay, marginLeftTemp, marginTopTemp, null);

        canvas.setDrawFilter(remfil);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = compassArrow.getWidth() + getPaddingLeft() + getPaddingRight();

            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    private int measureHeight(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = compassArrow.getHeight() + getPaddingTop() + getPaddingBottom();

            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }

        return result;
    }
}