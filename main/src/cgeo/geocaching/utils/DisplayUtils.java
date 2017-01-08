package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.content.Context;
import android.graphics.Point;
import android.view.WindowManager;

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
}
