package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;

import androidx.core.content.res.ResourcesCompat;

public class CompactIconModeUtils {

    private static int compactIconModeThreshold = -1;

    private CompactIconModeUtils() {
        // utility class
    }


    public static void setCompactIconModeThreshold(final Resources resources) {
        // cache density metrics
        final Bitmap marker = ((BitmapDrawable) ResourcesCompat.getDrawable(resources, R.drawable.marker, null)).getBitmap();
        final DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        compactIconModeThreshold = (int) ((displayMetrics.heightPixels / marker.getHeight()) * (displayMetrics.widthPixels / marker.getWidth()) / 4f);
    }

    public static boolean forceCompactIconMode(final int size) {
        if (compactIconModeThreshold == -1) {
            setCompactIconModeThreshold(Resources.getSystem());
        }
        final int compactIconMode = Settings.getCompactIconMode();
        return compactIconMode == Settings.COMPACTICON_ON || (compactIconMode == Settings.COMPACTICON_AUTO && size >= compactIconModeThreshold);
    }
}
