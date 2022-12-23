package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.AngleUtils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

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
    private final CompositeDisposable periodicUpdate = new CompositeDisposable();

    private static final class UpdateAction implements Runnable {

        private final WeakReference<CompassView> compassViewRef;

        private UpdateAction(final CompassView view) {
            this.compassViewRef = new WeakReference<>(view);
        }

        @Override
        public void run() {
            final CompassView compassView = compassViewRef.get();
            if (compassView == null) {
                return;
            }
            compassView.updateGraphics();
        }
    }

    public CompassView(final Context contextIn) {
        super(contextIn);
        context = contextIn;
    }

    public void updateGraphics() {
        final float newAzimuthShown = initialDisplay ? northMeasured : smoothUpdate(northMeasured, azimuthShown);
        final float newCacheHeadingShown = initialDisplay ? cacheHeadingMeasured : smoothUpdate(cacheHeadingMeasured, cacheHeadingShown);
        initialDisplay = false;
        if (Math.abs(AngleUtils.difference(azimuthShown, newAzimuthShown)) >= 2 ||
                Math.abs(AngleUtils.difference(cacheHeadingShown, newCacheHeadingShown)) >= 2) {
            azimuthShown = newAzimuthShown;
            cacheHeadingShown = newCacheHeadingShown;
            invalidate();
        }
    }

    public CompassView(final Context contextIn, final AttributeSet attrs) {
        super(contextIn, attrs);
        context = contextIn;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();  // call super to make lint happy
        final Resources res = context.getResources();
        compassUnderlay = BitmapFactory.decodeResource(res, R.drawable.compass_underlay);
        compassRose = BitmapFactory.decodeResource(res, R.drawable.compass_rose);
        compassArrow = BitmapFactory.decodeResource(res, R.drawable.compass_arrow);
        compassOverlay = BitmapFactory.decodeResource(res, R.drawable.compass_overlay);

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

        periodicUpdate.add(AndroidSchedulers.mainThread().schedulePeriodicallyDirect(new UpdateAction(this), 0, 40, TimeUnit.MILLISECONDS));
    }

    @Override
    public void onDetachedFromWindow() {
        periodicUpdate.clear();

        super.onDetachedFromWindow();

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

    /**
     * Update north and cache headings. This method may only be called on the UI thread.
     *
     * @param northHeading the north direction (rotation of the rose)
     * @param cacheHeading the cache direction (extra rotation of the needle)
     */
    public void updateNorth(final float northHeading, final float cacheHeading) {
        northMeasured = northHeading;
        cacheHeadingMeasured = cacheHeading;
    }

    /**
     * Compute the new value, moving by small increments.
     *
     * @param goal   the goal to reach
     * @param actual the actual value
     * @return the new value
     */
    protected static float smoothUpdate(final float goal, final float actual) {
        final double diff = AngleUtils.difference(actual, goal);

        double offset = 0;

        // If the difference is smaller than 1 degree, do nothing as it
        // causes the arrow to vibrate. Round away from 0.
        if (diff > 1.0) {
            offset = Math.ceil(diff / 10.0); // for larger angles, rotate faster
        } else if (diff < 1.0) {
            offset = Math.floor(diff / 10.0);
        }

        return AngleUtils.normalize((float) (actual + offset));
    }

    @Override
    protected void onDraw(final Canvas canvas) {

        final float azimuthTemp = azimuthShown;
        final float azimuthRelative = AngleUtils.normalize(azimuthTemp - cacheHeadingShown);

        // compass margins
        final int canvasCenterX = (compassRoseWidth / 2) + ((getWidth() - compassRoseWidth) / 2);
        final int canvasCenterY = (compassRoseHeight / 2) + ((getHeight() - compassRoseHeight) / 2);

        super.onDraw(canvas);

        canvas.save();
        canvas.setDrawFilter(setfil);

        canvas.drawBitmap(compassUnderlay, (getWidth() - compassUnderlayWidth) / 2.0f, (getHeight() - compassUnderlayHeight) / 2.0f, null);

        canvas.save();
        canvas.rotate(-azimuthTemp, canvasCenterX, canvasCenterY);
        canvas.drawBitmap(compassRose, (getWidth() - compassRoseWidth) / 2.0f, (getHeight() - compassRoseHeight) / 2.0f, null);
        canvas.restore();

        canvas.save();
        canvas.rotate(-azimuthRelative, canvasCenterX, canvasCenterY);
        canvas.drawBitmap(compassArrow, (getWidth() - compassArrowWidth) / 2.0f, (getHeight() - compassArrowHeight) / 2.0f, null);
        canvas.restore();

        canvas.drawBitmap(compassOverlay, (getWidth() - compassOverlayWidth) / 2.0f, (getHeight() - compassOverlayHeight) / 2.0f, null);

        canvas.setDrawFilter(remfil);
        canvas.restore();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(final int measureSpec) {
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        }

        final int desired = compassArrow.getWidth() + getPaddingLeft() + getPaddingRight();
        if (specMode == MeasureSpec.AT_MOST) {
            return Math.min(desired, specSize);
        }

        return desired;
    }

    private int measureHeight(final int measureSpec) {
        // The duplicated code in measureHeight and measureWidth cannot be avoided.
        // Those methods must be efficient, therefore we cannot extract the code differences and unify the remainder.
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        }

        final int desired = compassArrow.getHeight() + getPaddingTop() + getPaddingBottom();
        if (specMode == MeasureSpec.AT_MOST) {
            return Math.min(desired, specSize);
        }

        return desired;
    }
}
