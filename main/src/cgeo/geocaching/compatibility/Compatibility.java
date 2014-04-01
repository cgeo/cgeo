package cgeo.geocaching.compatibility;

import cgeo.geocaching.utils.AngleUtils;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;
import android.graphics.Point;
import android.os.Build;

import java.io.File;

public final class Compatibility {

    private final static int sdkVersion = Build.VERSION.SDK_INT;
    private final static boolean isLevel8 = sdkVersion >= 8;

    private final static AndroidLevel8Interface level8;
    private final static AndroidLevel11Interface level11;
    private final static AndroidLevel13Interface level13;
    private final static AndroidLevel19Interface level19;

    static {
        level8 = isLevel8 ? new AndroidLevel8() : new AndroidLevel8Emulation();
        level11 = sdkVersion >= 11 ? new AndroidLevel11() : new AndroidLevel11Emulation();
        level13 = sdkVersion >= 13 ? new AndroidLevel13() : new AndroidLevel13Emulation();
        level19 = sdkVersion >= 19 ? new AndroidLevel19() : new AndroidLevel19Emulation();
    }

    /**
     * Add 90, 180 or 270 degrees to the given rotation.
     *
     * @param directionNowPre
     *            the direction in degrees before adjustment
     * @param activity
     *            the activity whose rotation is used to adjust the direction
     * @return the adjusted direction, in the [0, 360[ range
     */
    public static float getDirectionNow(final float directionNowPre, final Activity activity) {
        return AngleUtils.normalize(directionNowPre + level8.getRotationOffset(activity));
    }

    public static void dataChanged(final String name) {
        level8.dataChanged(name);
    }

    public static void invalidateOptionsMenu(final Activity activity) {
        level11.invalidateOptionsMenu(activity);
    }

    public static int getDisplayWidth() {
        return level13.getDisplayWidth();
    }

    public static Point getDisplaySize() {
        return level13.getDisplaySize();
    }

    public static File getExternalPictureDir() {
        return level8.getExternalPictureDir();
    }

    public static void importGpxFromStorageAccessFramework(final @NonNull Activity activity, int requestCodeImportGpx) {
        level19.importGpxFromStorageAccessFramework(activity, requestCodeImportGpx);
    }

    public static boolean isStorageAccessFrameworkAvailable() {
        return sdkVersion >= 19;
    }
}
