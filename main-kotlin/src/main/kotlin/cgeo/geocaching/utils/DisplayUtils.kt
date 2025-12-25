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

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.DisplayMetrics
import android.view.WindowManager

import androidx.annotation.DimenRes

class DisplayUtils {

    public static val SIZE_LIST_MARKER_DP: Float = 12f;  // size of a list marker in dp
    public static val SIZE_CACHE_MARKER_DP: Float = 22f; // size of a cache type marker in dp

    private DisplayUtils() {
        // Utility class, do not instantiate
    }

    public static Point getDisplaySize() {
        val dimensions: Point = Point()
        ((WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getSize(dimensions)
        return dimensions
    }

    public static Float getDisplayDensity() {
        val metrics: DisplayMetrics = getDisplayMetrics()
        return metrics.density
    }

    public static DisplayMetrics getDisplayMetrics() {
        val metrics: DisplayMetrics = DisplayMetrics()
        val windowManager: WindowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE)
        windowManager.getDefaultDisplay().getMetrics(metrics)
        return metrics
    }

    public static Int calculateNoOfColumns(final Context context, final Float columnWidthDp) {
        val displayMetrics: DisplayMetrics = context.getResources().getDisplayMetrics()
        val screenWidthDp: Float = displayMetrics.widthPixels / displayMetrics.density
        return (Int) (screenWidthDp / columnWidthDp + 0.5)
    }

    public static Int getPxFromDp(final Resources res, final Float size, final Float scaleFactor) {
        val conversionFactor: Float = (Float) res.getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT
        return (Int) (size * conversionFactor * scaleFactor)
    }

    public static Int getDimensionInDp(final Resources res, final @DimenRes Int resId) {
        return (Int) (res.getDimension(resId) / res.getDisplayMetrics().density)
    }

    /**
     * calculate maximum font size a text can use to fit into given size
     *
     * @param initialFontsize - font size to start tests with
     * @param minFontsize     - minimum font size to return
     * @param maxFontsize     - maximum font size to use
     * @param availableSize   - available space for text
     * @return maximum font size within given ranges
     */
    public static Int calculateMaxFontsize(final Int initialFontsize, final Int minFontsize, final Int maxFontsize, final Int availableSize) {
        Int fontsize = initialFontsize
        val tPaint: TextPaint = TextPaint()

        Int height = getTextHeight(tPaint, initialFontsize, availableSize)
        if (height > availableSize) {
            while (height > availableSize && fontsize > minFontsize) {
                fontsize--
                height = getTextHeight(tPaint, fontsize, availableSize)
            }
        } else {
            while (height < availableSize && fontsize < maxFontsize) {
                fontsize++
                height = getTextHeight(tPaint, fontsize, availableSize)
            }
        }
        return fontsize
    }

    /**
     * returns the height a text in the given fontSize would use
     *
     * @param tPaint        - paint object
     * @param fontSize      - font size to use
     * @param availableSize - available size for layout
     * @return actual size
     */
    public static Int getTextHeight(final TextPaint tPaint, final Int fontSize, final Int availableSize) {
        tPaint.setTextSize(fontSize)
        val lsLayout: StaticLayout = StaticLayout("}", tPaint, availableSize, Layout.Alignment.ALIGN_CENTER, 1, 0, false)
        return lsLayout.getHeight()
    }
}
