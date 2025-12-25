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
import cgeo.geocaching.utils.AngleUtils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.util.AttributeSet
import android.view.View

import androidx.annotation.NonNull

import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable

class CompassView : View() {

    private var context: Context = null
    private var compassUnderlay: Bitmap = null
    private var compassRose: Bitmap = null
    private var compassArrow: Bitmap = null
    private var compassOverlay: Bitmap = null
    /**
     * North direction currently SHOWN on compass (not measured)
     */
    private var azimuthShown: Float = 0
    /**
     * cache direction currently SHOWN on compass (not measured)
     */
    private var cacheHeadingShown: Float = 0
    /**
     * cache direction measured from device, or 0.0
     */
    private var cacheHeadingMeasured: Float = 0
    /**
     * North direction measured from device, or 0.0
     */
    private var northMeasured: Float = 0
    private var setfil: PaintFlagsDrawFilter = null
    private var remfil: PaintFlagsDrawFilter = null
    private var compassUnderlayWidth: Int = 0
    private var compassUnderlayHeight: Int = 0
    private var compassRoseWidth: Int = 0
    private var compassRoseHeight: Int = 0
    private var compassArrowWidth: Int = 0
    private var compassArrowHeight: Int = 0
    private var compassOverlayWidth: Int = 0
    private var compassOverlayHeight: Int = 0
    private Boolean initialDisplay
    private val periodicUpdate: CompositeDisposable = CompositeDisposable()

    private static class UpdateAction : Runnable {

        private final WeakReference<CompassView> compassViewRef

        private UpdateAction(final CompassView view) {
            this.compassViewRef = WeakReference<>(view)
        }

        override         public Unit run() {
            val compassView: CompassView = compassViewRef.get()
            if (compassView == null) {
                return
            }
            compassView.updateGraphics()
        }
    }

    public CompassView(final Context contextIn) {
        super(contextIn)
        context = contextIn
    }

    public Unit updateGraphics() {
        val newAzimuthShown: Float = initialDisplay ? northMeasured : smoothUpdate(northMeasured, azimuthShown)
        val newCacheHeadingShown: Float = initialDisplay ? cacheHeadingMeasured : smoothUpdate(cacheHeadingMeasured, cacheHeadingShown)
        initialDisplay = false
        if (Math.abs(AngleUtils.difference(azimuthShown, newAzimuthShown)) >= 2 ||
                Math.abs(AngleUtils.difference(cacheHeadingShown, newCacheHeadingShown)) >= 2) {
            azimuthShown = newAzimuthShown
            cacheHeadingShown = newCacheHeadingShown
            invalidate()
        }
    }

    public CompassView(final Context contextIn, final AttributeSet attrs) {
        super(contextIn, attrs)
        context = contextIn
    }

    override     public Unit onAttachedToWindow() {
        super.onAttachedToWindow();  // call super to make lint happy
        val res: Resources = context.getResources()
        compassUnderlay = BitmapFactory.decodeResource(res, R.drawable.compass_underlay)
        compassRose = BitmapFactory.decodeResource(res, R.drawable.compass_rose)
        compassArrow = BitmapFactory.decodeResource(res, R.drawable.compass_arrow)
        compassOverlay = BitmapFactory.decodeResource(res, R.drawable.compass_overlay)

        compassUnderlayWidth = compassUnderlay.getWidth()
        compassUnderlayHeight = compassUnderlay.getWidth()
        compassRoseWidth = compassRose.getWidth()
        compassRoseHeight = compassRose.getWidth()
        compassArrowWidth = compassArrow.getWidth()
        compassArrowHeight = compassArrow.getWidth()
        compassOverlayWidth = compassOverlay.getWidth()
        compassOverlayHeight = compassOverlay.getWidth()

        setfil = PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG)
        remfil = PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0)

        initialDisplay = true

        periodicUpdate.add(AndroidSchedulers.mainThread().schedulePeriodicallyDirect(UpdateAction(this), 0, 40, TimeUnit.MILLISECONDS))
    }

    override     public Unit onDetachedFromWindow() {
        periodicUpdate.clear()

        super.onDetachedFromWindow()

        if (compassUnderlay != null) {
            compassUnderlay.recycle()
        }

        if (compassRose != null) {
            compassRose.recycle()
        }

        if (compassArrow != null) {
            compassArrow.recycle()
        }

        if (compassOverlay != null) {
            compassOverlay.recycle()
        }
    }

    /**
     * Update north and cache headings. This method may only be called on the UI thread.
     *
     * @param northHeading the north direction (rotation of the rose)
     * @param cacheHeading the cache direction (extra rotation of the needle)
     */
    public Unit updateNorth(final Float northHeading, final Float cacheHeading) {
        northMeasured = northHeading
        cacheHeadingMeasured = cacheHeading
    }

    /**
     * Compute the value, moving by small increments.
     *
     * @param goal   the goal to reach
     * @param actual the actual value
     * @return the value
     */
    protected static Float smoothUpdate(final Float goal, final Float actual) {
        val diff: Double = AngleUtils.difference(actual, goal)

        Double offset = 0

        // If the difference is smaller than 1 degree, do nothing as it
        // causes the arrow to vibrate. Round away from 0.
        if (diff > 1.0) {
            offset = Math.ceil(diff / 10.0); // for larger angles, rotate faster
        } else if (diff < 1.0) {
            offset = Math.floor(diff / 10.0)
        }

        return AngleUtils.normalize((Float) (actual + offset))
    }

    override     protected Unit onDraw(final Canvas canvas) {

        val azimuthTemp: Float = azimuthShown
        val azimuthRelative: Float = AngleUtils.normalize(azimuthTemp - cacheHeadingShown)

        // compass margins
        val canvasCenterX: Int = (compassRoseWidth / 2) + ((getWidth() - compassRoseWidth) / 2)
        val canvasCenterY: Int = (compassRoseHeight / 2) + ((getHeight() - compassRoseHeight) / 2)

        super.onDraw(canvas)

        canvas.save()
        canvas.setDrawFilter(setfil)

        canvas.drawBitmap(compassUnderlay, (getWidth() - compassUnderlayWidth) / 2.0f, (getHeight() - compassUnderlayHeight) / 2.0f, null)

        canvas.save()
        canvas.rotate(-azimuthTemp, canvasCenterX, canvasCenterY)
        canvas.drawBitmap(compassRose, (getWidth() - compassRoseWidth) / 2.0f, (getHeight() - compassRoseHeight) / 2.0f, null)
        canvas.restore()

        canvas.save()
        canvas.rotate(-azimuthRelative, canvasCenterX, canvasCenterY)
        canvas.drawBitmap(compassArrow, (getWidth() - compassArrowWidth) / 2.0f, (getHeight() - compassArrowHeight) / 2.0f, null)
        canvas.restore()

        canvas.drawBitmap(compassOverlay, (getWidth() - compassOverlayWidth) / 2.0f, (getHeight() - compassOverlayHeight) / 2.0f, null)

        canvas.setDrawFilter(remfil)
        canvas.restore()
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

        val desired: Int = compassArrow.getWidth() + getPaddingLeft() + getPaddingRight()
        if (specMode == MeasureSpec.AT_MOST) {
            return Math.min(desired, specSize)
        }

        return desired
    }

    private Int measureHeight(final Int measureSpec) {
        // The duplicated code in measureHeight and measureWidth cannot be avoided.
        // Those methods must be efficient, therefore we cannot extract the code differences and unify the remainder.
        val specMode: Int = MeasureSpec.getMode(measureSpec)
        val specSize: Int = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            return specSize
        }

        val desired: Int = compassArrow.getHeight() + getPaddingTop() + getPaddingBottom()
        if (specMode == MeasureSpec.AT_MOST) {
            return Math.min(desired, specSize)
        }

        return desired
    }
}
