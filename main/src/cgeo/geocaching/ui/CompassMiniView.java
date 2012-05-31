package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.geopoint.Geopoint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.view.View;

final public class CompassMiniView extends View {
    private Geopoint targetCoords = null;
    private float azimuth = 0;
    private float heading = 0;
    /**
     * remember the last state of drawing so we can avoid repainting for very small changes
     */
    private float lastDrawingAzimuth;

    /**
     * lazy initialized bitmap resource depending on selected skin
     */
    private static int arrowSkin = 0;
    /**
     * bitmap shared by all instances of the view
     */
    private static Bitmap compassArrow;
    /**
     * bitmap width
     */
    private static int compassArrowWidth;
    /**
     * bitmap height
     */
    private static int compassArrowHeight;
    /**
     * view instance counter for bitmap recycling
     */
    private static int instances = 0;

    /**
     * pixel size of the square arrow bitmap
     */
    private static final int ARROW_BITMAP_SIZE = 21;
    private static final PaintFlagsDrawFilter FILTER_SET = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
    private static final PaintFlagsDrawFilter FILTER_REMOVE = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);
    private static final float MINIMUM_ROTATION_DEGREES_FOR_REPAINT = 5;

    public CompassMiniView(Context context) {
        super(context);
        initializeResources(context);
    }

    public CompassMiniView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeResources(context);
    }

    private static void initializeResources(final Context context) {
        if (arrowSkin == 0) {
            if (Settings.isLightSkin()) {
                arrowSkin = R.drawable.compass_arrow_mini_black;
            } else {
                arrowSkin = R.drawable.compass_arrow_mini_white;
            }
        }
        if (compassArrow == null) {
            compassArrow = BitmapFactory.decodeResource(context.getResources(), arrowSkin);
            compassArrowWidth = compassArrow.getWidth();
            compassArrowHeight = compassArrow.getWidth();
        }
    }

    @Override
    public void onAttachedToWindow() {
        instances++;
    }

    @Override
    public void onDetachedFromWindow() {
        instances--;
        if (instances == 0) {
            if (compassArrow != null) {
                compassArrow.recycle();
                compassArrow = null;
            }
        }
    }

    public void setTargetCoords(final Geopoint point) {
        targetCoords = point;
    }

    protected void updateAzimuth(float azimuth) {
        this.azimuth = azimuth;
        updateDirection();
    }

    protected void updateHeading(float heading) {
        this.heading = heading;
        updateDirection();
    }

    protected void updateCurrentCoords(final Geopoint currentCoords) {
        if (currentCoords == null || targetCoords == null) {
            return;
        }

        heading = currentCoords.bearingTo(targetCoords);
        updateDirection();
    }

    private void updateDirection() {
        if (compassArrow == null || compassArrow.isRecycled()) {
            return;
        }

        float azimuthRelative = calculateAzimuthRelative();

        // avoid updates on very small changes, which are not visible to the user
        float change = Math.abs(azimuthRelative - lastDrawingAzimuth);
        if (change < MINIMUM_ROTATION_DEGREES_FOR_REPAINT) {
            return;
        }

        // compass margins
        final int marginLeft = (getWidth() - compassArrowWidth) / 2;
        final int marginTop = (getHeight() - compassArrowHeight) / 2;

        invalidate(marginLeft, marginTop, (marginLeft + compassArrowWidth), (marginTop + compassArrowHeight));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float azimuthRelative = calculateAzimuthRelative();
        lastDrawingAzimuth = azimuthRelative;

        // compass margins
        canvas.setDrawFilter(FILTER_SET);

        final int canvasCenterX = getWidth() / 2;
        final int canvasCenterY = getHeight() / 2;

        final int marginLeft = (getWidth() - compassArrowWidth) / 2;
        final int marginTop = (getHeight() - compassArrowHeight) / 2;

        canvas.rotate(-azimuthRelative, canvasCenterX, canvasCenterY);
        canvas.drawBitmap(compassArrow, marginLeft, marginTop, null);
        canvas.rotate(azimuthRelative, canvasCenterX, canvasCenterY);

        canvas.setDrawFilter(FILTER_REMOVE);
    }

    private float calculateAzimuthRelative() {
        return (azimuth - heading + 360) % 360;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        }

        int result = ARROW_BITMAP_SIZE + getPaddingLeft() + getPaddingRight();

        if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize);
        }

        return result;
    }

    private int measureHeight(int measureSpec) {
        final int specMode = MeasureSpec.getMode(measureSpec);
        final int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize;
        }

        int result = ARROW_BITMAP_SIZE + getPaddingTop() + getPaddingBottom();

        if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize);
        }

        return result;
    }
}