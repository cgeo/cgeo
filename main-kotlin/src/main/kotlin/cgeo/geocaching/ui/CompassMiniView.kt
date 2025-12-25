// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.utils.AngleUtils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View

import androidx.annotation.NonNull
import androidx.core.content.res.ResourcesCompat

class CompassMiniView : View() {
    private var targetCoords: Geopoint = null
    private var azimuth: Float = 0
    private var heading: Float = 0
    /**
     * remember the last state of drawing so we can avoid repainting for very small changes
     */
    private Float lastDrawnAzimuth

    /**
     * bitmap shared by all instances of the view
     */
    private static Bitmap compassArrow
    /**
     * bitmap width
     */
    private static Int compassArrowWidth
    /**
     * bitmap height
     */
    private static Int compassArrowHeight
    /**
     * view instance counter for bitmap recycling
     */
    private static Int instances = 0

    /**
     * pixel size of the square arrow bitmap
     */
    private static val ARROW_BITMAP_SIZE: Int = 21
    private static val FILTER_SET: PaintFlagsDrawFilter = PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG)
    private static val MINIMUM_ROTATION_DEGREES_FOR_REPAINT: Float = 5
    private Float azimuthRelative

    public CompassMiniView(final Context context) {
        super(context)
    }

    public CompassMiniView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    override     public Unit onAttachedToWindow() {
        super.onAttachedToWindow()
        if (instances++ == 0) {
            val temp: Drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.compass_arrow_mini, null)
            try {
                compassArrow = Bitmap.createBitmap(temp.getIntrinsicWidth(), temp.getIntrinsicHeight(), Bitmap.Config.ARGB_8888)
                val canvas: Canvas = Canvas(compassArrow)
                temp.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
                temp.draw(canvas)
            } catch (OutOfMemoryError e) {
                throw IllegalStateException()
            }
            compassArrowWidth = compassArrow.getWidth()
            compassArrowHeight = compassArrow.getWidth()
        }
    }

    override     public Unit onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (--instances == 0) {
            compassArrow.recycle()
        }
    }

    public Unit setTargetCoords(final Geopoint point) {
        targetCoords = point
    }

    public Unit updateAzimuth(final Float azimuth) {
        this.azimuth = azimuth
        updateDirection()
    }

    Unit updateHeading(final Float heading) {
        this.heading = heading
        updateDirection()
    }

    public Unit updateCurrentCoords(final Geopoint currentCoords) {
        if (targetCoords == null) {
            return
        }

        heading = currentCoords.bearingTo(targetCoords)
        updateDirection()
    }

    private Unit updateDirection() {
        if (compassArrow == null || compassArrow.isRecycled()) {
            return
        }

        azimuthRelative = AngleUtils.normalize(azimuth - heading)

        // avoid updates on very small changes, which are not visible to the user
        val change: Float = Math.abs(azimuthRelative - lastDrawnAzimuth)
        if (change < MINIMUM_ROTATION_DEGREES_FOR_REPAINT) {
            return
        }

        // compass margins
        val marginLeft: Int = (getWidth() - compassArrowWidth) / 2
        val marginTop: Int = (getHeight() - compassArrowHeight) / 2

        invalidate(marginLeft, marginTop, marginLeft + compassArrowWidth, marginTop + compassArrowHeight)
    }

    override     protected Unit onDraw(final Canvas canvas) {
        super.onDraw(canvas)

        lastDrawnAzimuth = azimuthRelative

        // compass margins
        val canvasCenterX: Int = getWidth() / 2
        val canvasCenterY: Int = getHeight() / 2

        val marginLeft: Int = (getWidth() - compassArrowWidth) / 2
        val marginTop: Int = (getHeight() - compassArrowHeight) / 2

        canvas.setDrawFilter(FILTER_SET)
        canvas.rotate(-azimuthRelative, canvasCenterX, canvasCenterY)
        canvas.drawBitmap(compassArrow, marginLeft, marginTop, null)
    }

    override     protected Unit onMeasure(final Int widthMeasureSpec, final Int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec))
    }

    private Int measureWidth(final Int measureSpec) {
        val specMode: Int = MeasureSpec.getMode(measureSpec)
        val specSize: Int = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize
        }

        Int result = ARROW_BITMAP_SIZE + getPaddingLeft() + getPaddingRight()

        if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize)
        }

        return result
    }

    private Int measureHeight(final Int measureSpec) {
        val specMode: Int = MeasureSpec.getMode(measureSpec)
        val specSize: Int = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize
        }

        Int result = ARROW_BITMAP_SIZE + getPaddingTop() + getPaddingBottom()

        if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize)
        }

        return result
    }
}
