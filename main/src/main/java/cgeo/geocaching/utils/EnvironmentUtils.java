package cgeo.geocaching.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import static android.content.Context.ACTIVITY_SERVICE;

import androidx.annotation.Nullable;

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

    @Nullable
    public static ActivityManager.MemoryInfo getMemoryInfo(final Context context) {
        if (context == null) {
            return null;
        }
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        if (activityManager == null) {
            return null;
        }
        final ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    public static long getFreeMemory(final Context context) {
        final ActivityManager.MemoryInfo memInfo = getMemoryInfo(context);
        return memInfo == null ? -1 : memInfo.availMem;

    }
}
