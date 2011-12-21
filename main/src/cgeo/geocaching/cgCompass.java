package cgeo.geocaching;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class cgCompass extends View {

    private changeThread watchdog = null;
    private volatile boolean wantStop = false;
    private Context context = null;
    private Bitmap compassUnderlay = null;
    private Bitmap compassRose = null;
    private Bitmap compassArrow = null;
    private Bitmap compassOverlay = null;
    /**
     * North direction currently SHOWN on compass (not measured)
     */
    private double azimuthShown = 0.0;
    /**
     * cache direction currently SHOWN on compass (not measured)
     */
    private double cacheHeadingShown = 0.0;
    /**
     * cache direction measured from device, or 0.0
     */
    private double cacheHeadingMeasured = 0.0;
    /**
     * North direction measured from device, or 0.0
     */
    private double northMeasured = 0.0;
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
    private Handler changeHandler = new Handler() {

        @Override
        public void handleMessage(Message message) {
            try {
                invalidate();
            } catch (Exception e) {
                Log.e(Settings.tag, "cgCompass.changeHandler: " + e.toString());
            }
        }
    };

    public cgCompass(Context contextIn) {
        super(contextIn);
        context = contextIn;
    }

    public cgCompass(Context contextIn, AttributeSet attrs) {
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
        wantStop = false;

        watchdog = new changeThread();
        watchdog.start();
    }

    @Override
    public void onDetachedFromWindow() {
        wantStop = true;

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

    protected synchronized void updateNorth(double northHeadingIn, double cacheHeadingIn) {
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
    static protected double smoothUpdate(double goal, double actual) {
        double diff = goal - actual;
        final boolean largeDiff = Math.abs(diff) > 5;

        double offset = 0.0;

        if (diff < 0.0) {
            diff += 360.0;
        } else if (diff >= 360.0) {
            diff -= 360.0;
        }

        // If the difference is smaller than 1 degree, do nothing as it
        // causes the arrow to vibrate.
        if (diff > 1.0 && diff <= 180.0) {
            offset = largeDiff ? 2.0 : 1.0;
        } else if (diff > 180.0 && diff < 359.0) {
            offset = largeDiff ? -2.0 : -1.0;
        }

        return actual + offset;
    }

    private class changeThread extends Thread {

        @Override
        public void run() {
            while (!wantStop) {
                try {
                    sleep(50);
                } catch (Exception e) {
                    // nothing
                }

                synchronized (cgCompass.this) {
                    azimuthShown = smoothUpdate(northMeasured, azimuthShown);
                    cacheHeadingShown = smoothUpdate(cacheHeadingMeasured, cacheHeadingShown);
                }

                changeHandler.sendMessage(new Message());
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // use local synchronized variables to avoid them being changed from the device during drawing
        double azimuthDrawn;
        double headingDrawn;

        synchronized (this) {
            azimuthDrawn = azimuthShown;
            headingDrawn = cacheHeadingShown;
        }

        double azimuthTemp = azimuthDrawn;
        double azimuthRelative = azimuthTemp - headingDrawn;
        if (azimuthRelative < 0) {
            azimuthRelative += 360;
        } else if (azimuthRelative >= 360) {
            azimuthRelative -= 360;
        }

        // compass margins
        int canvasCenterX = (compassRoseWidth / 2) + ((getWidth() - compassRoseWidth) / 2);
        int canvasCenterY = (compassRoseHeight / 2) + ((getHeight() - compassRoseHeight) / 2);

        int marginLeftTemp = 0;
        int marginTopTemp = 0;

        super.onDraw(canvas);

        canvas.save();
        canvas.setDrawFilter(setfil);

        marginLeftTemp = (getWidth() - compassUnderlayWidth) / 2;
        marginTopTemp = (getHeight() - compassUnderlayHeight) / 2;

        canvas.drawBitmap(compassUnderlay, marginLeftTemp, marginTopTemp, null);

        marginLeftTemp = (getWidth() - compassRoseWidth) / 2;
        marginTopTemp = (getHeight() - compassRoseHeight) / 2;

        canvas.rotate((float) -azimuthTemp, canvasCenterX, canvasCenterY);
        canvas.drawBitmap(compassRose, marginLeftTemp, marginTopTemp, null);
        canvas.rotate((float) azimuthTemp, canvasCenterX, canvasCenterY);

        marginLeftTemp = (getWidth() - compassArrowWidth) / 2;
        marginTopTemp = (getHeight() - compassArrowHeight) / 2;

        canvas.rotate((float) -azimuthRelative, canvasCenterX, canvasCenterY);
        canvas.drawBitmap(compassArrow, marginLeftTemp, marginTopTemp, null);
        canvas.rotate((float) azimuthRelative, canvasCenterX, canvasCenterY);

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
        int result = 0;
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
        int result = 0;
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