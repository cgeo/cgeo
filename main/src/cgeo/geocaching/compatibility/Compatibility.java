package cgeo.geocaching.compatibility;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;

public final class Compatibility {

    private static final int SDK_VERSION = Build.VERSION.SDK_INT;

    private static final AndroidLevel19Interface LEVEL_19;

    static {
        LEVEL_19 = SDK_VERSION >= 19 ? new AndroidLevel19() : new AndroidLevel19Emulation();
    }

    private Compatibility() {
        // utility class
    }

    public static void importGpxFromStorageAccessFramework(@NonNull final Activity activity, final int requestCodeImportGpx) {
        LEVEL_19.importGpxFromStorageAccessFramework(activity, requestCodeImportGpx);
    }

    public static boolean isStorageAccessFrameworkAvailable() {
        return SDK_VERSION >= 19;
    }

    @SuppressWarnings("deprecation")
    // the non replacement method is only available on level 21, therefore we ignore this deprecation
    public static Drawable getDrawable(final Resources resources, final int markerId) {
        return resources.getDrawable(markerId);
    }
}
