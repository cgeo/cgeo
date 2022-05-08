package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class DisplayUtils {

    public static final float SIZE_LIST_MARKER_DP = 12f;  // size of a list marker in dp
    public static final float SIZE_CACHE_MARKER_DP = 22f; // size of a cache type marker in dp

    private DisplayUtils() {
        // Utility class, do not instantiate
    }

    public static Point getDisplaySize() {
        final Point dimensions = new Point();
        ((WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getSize(dimensions);
        return dimensions;
    }

    public static float getDisplayDensity() {
        final DisplayMetrics metrics = getDisplayMetrics();
        return metrics.density;
    }

    public static DisplayMetrics getDisplayMetrics() {
        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    public static int calculateNoOfColumns(final Context context, final float columnWidthDp) {
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (screenWidthDp / columnWidthDp + 0.5);
    }

    public static int getPxFromDp(final Resources res, final float size, final float scaleFactor) {
        final float conversionFactor = (float) res.getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        return (int) (size * conversionFactor * scaleFactor);
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
    public static int calculateMaxFontsize(final int initialFontsize, final int minFontsize, final int maxFontsize, final int availableSize) {
        int fontsize = initialFontsize;
        final TextPaint tPaint = new TextPaint();

        int height = getTextHeight(tPaint, initialFontsize, availableSize);
        if (height > availableSize) {
            while (height > availableSize && fontsize > minFontsize) {
                fontsize--;
                height = getTextHeight(tPaint, fontsize, availableSize);
            }
        } else {
            while (height < availableSize && fontsize < maxFontsize) {
                fontsize++;
                height = getTextHeight(tPaint, fontsize, availableSize);
            }
        }
        return fontsize;
    }

    /**
     * returns the height a text in the given fontSize would use
     *
     * @param tPaint        - paint object
     * @param fontSize      - font size to use
     * @param availableSize - available size for layout
     * @return actual size
     */
    public static int getTextHeight(final TextPaint tPaint, final int fontSize, final int availableSize) {
        tPaint.setTextSize(fontSize);
        final StaticLayout lsLayout = new StaticLayout("}", tPaint, availableSize, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        return lsLayout.getHeight();
    }
}
