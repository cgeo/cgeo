package cgeo.geocaching.utils;

import android.os.Build;
import android.os.Environment;

public class EnvironmentUtils {
    private static final boolean DEFAULT_TRANSPARENT_BACKGROUND;

    static {
        DEFAULT_TRANSPARENT_BACKGROUND = !isSailfishOs();
    }

    private EnvironmentUtils() {
        // utility class
    }

    public static boolean isSailfishOs() {
        return Build.PRODUCT.startsWith("aosp") && Build.VERSION.SDK_INT == 27;
    }

    public static boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static boolean defaultBackgroundTransparent() {
        return DEFAULT_TRANSPARENT_BACKGROUND;
    }
}
