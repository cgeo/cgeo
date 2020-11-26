package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.Menu;

import androidx.core.content.res.ResourcesCompat;

public class CompactIconModeUtils {

    private static int compactIconModeThreshold = -1;

    private CompactIconModeUtils() {
        // utility class
    }


    public static void onPrepareOptionsMenu(final Menu menu) {
        switch (Settings.getCompactIconMode()) {
            case Settings.COMPACTICON_OFF:
                menu.findItem(R.id.menu_map_compactIconModeOff).setChecked(true);
                break;
            case Settings.COMPACTICON_ON:
                menu.findItem(R.id.menu_map_compactIconModeOn).setChecked(true);
                break;
            case Settings.COMPACTICON_AUTO:
                menu.findItem(R.id.menu_map_compactIconModeAuto).setChecked(true);
                break;
            default:
                // do nothing
        }
    }

    public static boolean onOptionsItemSelected(final int id, final Runnable setCompactIconMode) {
        if (id == R.id.menu_map_compactIconModeOff) {
            Settings.setCompactIconMode(Settings.COMPACTICON_OFF);
        } else if (id == R.id.menu_map_compactIconModeOn) {
            Settings.setCompactIconMode(Settings.COMPACTICON_ON);
        } else if (id == R.id.menu_map_compactIconModeAuto) {
            Settings.setCompactIconMode(Settings.COMPACTICON_AUTO);
        } else {
            return false;
        }
        setCompactIconMode.run();
        return true;
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
