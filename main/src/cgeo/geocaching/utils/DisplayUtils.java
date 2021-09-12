package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.WindowManager;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public class DisplayUtils {

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

    /**
     * get actual width and height of given drawable resource
     * @param res - resources to load from
     * @param resToFitIn - resource to check
     * @return actual width and height
     */
    public static Pair<Integer, Integer> getDrawableDimensions(final Resources res, @DrawableRes final int resToFitIn) {
        final Drawable calc = ResourcesCompat.getDrawable(res, resToFitIn, null);
        assert calc != null;
        return new Pair<>(calc.getIntrinsicWidth(), calc.getIntrinsicHeight());
    }

    /**
     * get actual width and height of given drawable
     * @param resToFitIn - drawable to check
     * @return actual width and height
     */
    public static Pair<Integer, Integer> getDrawableDimensions(final Drawable resToFitIn) {
        return new Pair<>(resToFitIn.getIntrinsicWidth(), resToFitIn.getIntrinsicHeight());
    }

    public static Drawable getTintedDrawable(final Resources res, @DrawableRes final int menuRes, @ColorRes final int tintColor) {
        final int textColor = res.getColor(tintColor);

        final Drawable menuDrawable = ResourcesCompat.getDrawable(res, menuRes, null);
        DrawableCompat.setTint(menuDrawable, textColor);
        return menuDrawable;
    }

    /**
     * calculate maximum font size a text can use to fit into given size
     * @param initialFontsize - font size to start tests with
     * @param minFontsize - minimum font size to return
     * @param maxFontsize - maximum font size to use
     * @param availableSize - available space for text
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
     * @param tPaint - paint object
     * @param fontSize - font size to use
     * @param availableSize - available size for layout
     * @return actual size
     */
    public static int getTextHeight(final TextPaint tPaint, final int fontSize, final int availableSize) {
        tPaint.setTextSize(fontSize);
        final StaticLayout lsLayout = new StaticLayout("}", tPaint, availableSize, Layout.Alignment.ALIGN_CENTER, 1, 0, false);
        return lsLayout.getHeight();
    }
}
