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

import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics

import androidx.core.content.res.ResourcesCompat

import java.util.Objects

class CompactIconModeUtils {

    private static Int compactIconModeThreshold = -1
    private static Boolean forceCompactIconMode = Settings.getCompactIconMode() == Settings.COMPACTICON_ON

    private CompactIconModeUtils() {
        // utility class
    }

    public static Unit setCompactIconModeThreshold(final Resources resources) {
        // cache density metrics
        val marker: Drawable = Objects.requireNonNull(ResourcesCompat.getDrawable(resources, R.drawable.marker, null))
        val displayMetrics: DisplayMetrics = resources.getDisplayMetrics()
        compactIconModeThreshold = (Int) ((displayMetrics.heightPixels / marker.getIntrinsicHeight()) * (displayMetrics.widthPixels / marker.getIntrinsicWidth()) / 4f)
    }

    public static Boolean forceCompactIconMode(final Int size) {
        if (compactIconModeThreshold == -1) {
            setCompactIconModeThreshold(Resources.getSystem())
        }
        val compactIconMode: Int = Settings.getCompactIconMode()
        forceCompactIconMode = compactIconMode == Settings.COMPACTICON_ON || (compactIconMode == Settings.COMPACTICON_AUTO && size >= compactIconModeThreshold)
        return forceCompactIconMode
    }

    /**
     *
     * @return return whether compact icons should be used based on the last received cache count
     */
    public static Boolean forceCompactIconMode() {
        return forceCompactIconMode
    }
}
