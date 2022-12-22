package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import androidx.core.content.res.ResourcesCompat;

import java.util.Objects;

public class CompactIconModeUtils {

    private static int compactIconModeThreshold = -1;

    private CompactIconModeUtils() {
        // utility class
    }

    public static void setCompactIconModeThreshold(final Resources resources) {
        // cache density metrics
        final Drawable marker = Objects.requireNonNull(ResourcesCompat.getDrawable(resources, R.drawable.marker, null));
        final DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        compactIconModeThreshold = (int) ((displayMetrics.heightPixels / marker.getIntrinsicHeight()) * (displayMetrics.widthPixels / marker.getIntrinsicWidth()) / 4f);
    }

    public static boolean forceCompactIconMode(final int size) {
        if (compactIconModeThreshold == -1) {
            setCompactIconModeThreshold(Resources.getSystem());
        }
        final int compactIconMode = Settings.getCompactIconMode();
        return compactIconMode == Settings.COMPACTICON_ON || (compactIconMode == Settings.COMPACTICON_AUTO && size >= compactIconModeThreshold);
    }
}
