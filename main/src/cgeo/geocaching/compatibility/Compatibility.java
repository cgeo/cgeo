package cgeo.geocaching.compatibility;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;
import android.graphics.Point;
import android.os.Build;

public final class Compatibility {

    private final static int sdkVersion = Build.VERSION.SDK_INT;

    private final static AndroidLevel11Interface level11;
    private final static AndroidLevel13Interface level13;
    private final static AndroidLevel19Interface level19;

    static {
        level11 = sdkVersion >= 11 ? new AndroidLevel11() : new AndroidLevel11Emulation();
        level13 = sdkVersion >= 13 ? new AndroidLevel13() : new AndroidLevel13Emulation();
        level19 = sdkVersion >= 19 ? new AndroidLevel19() : new AndroidLevel19Emulation();
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

    public static void importGpxFromStorageAccessFramework(final @NonNull Activity activity, int requestCodeImportGpx) {
        level19.importGpxFromStorageAccessFramework(activity, requestCodeImportGpx);
    }

    public static boolean isStorageAccessFrameworkAvailable() {
        return sdkVersion >= 19;
    }
}
