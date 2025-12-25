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

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.content.Context.ACTIVITY_SERVICE

import androidx.annotation.Nullable

class EnvironmentUtils {

    private EnvironmentUtils() {
        // utility class
    }

    public static Boolean isSailfishOs() {
        return Build.PRODUCT.startsWith("aosp") && Build.VERSION.SDK_INT == 27
    }

    public static Boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED == (Environment.getExternalStorageState())
    }

    public static ActivityManager.MemoryInfo getMemoryInfo(final Context context) {
        if (context == null) {
            return null
        }
        val activityManager: ActivityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE)
        if (activityManager == null) {
            return null
        }
        final ActivityManager.MemoryInfo memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }

    public static Long getFreeMemory(final Context context) {
        final ActivityManager.MemoryInfo memInfo = getMemoryInfo(context)
        return memInfo == null ? -1 : memInfo.availMem

    }
}
