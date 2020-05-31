package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class DisplayUtils {

    private static final float THIN_LINE = 4f;
    private static final float HISTORY_LINE_SHADOW = 3f;
    private static final float HISTORY_LINE_INSET = HISTORY_LINE_SHADOW / 2;

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

    public static float getRouteLineWidth() {
        return THIN_LINE * getDisplayDensity();
    }

    public static float getDirectionLineWidth() {
        return THIN_LINE * getDisplayDensity();
    }

    public static float getHistoryLineShadowWidth() {
        return HISTORY_LINE_SHADOW * getDisplayDensity();
    }

    public static float getHistoryLineInsetWidth() {
        return HISTORY_LINE_INSET * getDisplayDensity();
    }

    // for drawing debug relevant lines
    public static float getDebugLineWidth() {
        return THIN_LINE * getDisplayDensity();
    }
}
