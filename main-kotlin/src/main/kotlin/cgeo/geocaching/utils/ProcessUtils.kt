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

import cgeo.geocaching.CgeoApplication

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List

import org.apache.commons.collections4.CollectionUtils

class ProcessUtils {

    public static val CHROME_PACKAGE_NAME: String = "com.android.chrome"

    private ProcessUtils() {
        // utility class
    }

    /**
     * Preferred method to detect the availability of an external app
     */
    public static Boolean isLaunchable(final String packageName) {
        return getLaunchIntent(packageName) != null
    }

    public static Boolean isChromeLaunchable() {
        return ProcessUtils.isLaunchable(CHROME_PACKAGE_NAME)
    }

    public static List<ResolveInfo> getInstalledBrowsers(final Context context) {
        // We're using "https://example.com" as we only want to query for web browsers, not c:geo or other apps
        val browserIntent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        return context.getPackageManager().queryIntentActivities(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
    }


    /**
     * Checks whether a launch intent is available or if the package is just installed
     * This function is relatively costly, so if you know that the package in question has
     * a launch intent, use isLaunchable() instead.
     */
    public static Boolean isInstalled(final String packageName) {
        return isLaunchable(packageName) || hasPackageInstalled(packageName)
    }

    /**
     * This will find installed applications even without launch intent (e.g. the streetview plugin).
     * <br>
     * Be aware:
     * Starting with Android 11 getInstalledPackages() will only return packages declared in AndroidManifest.xml
     * (Add a lint exception, as we have cross-checked our current usages for this method)
     */
    @SuppressLint("QueryPermissionsNeeded")
    private static Boolean hasPackageInstalled(final String packageName) {
        val packs: List<PackageInfo> = CgeoApplication.getInstance().getPackageManager().getInstalledPackages(0)
        for (final PackageInfo packageInfo : packs) {
            if (packageName == (packageInfo.packageName)) {
                return true
            }
        }
        return false
    }

    /**
     * This will find applications, which can be launched.
     */
    public static Intent getLaunchIntent(final String packageName) {
        if (packageName == null) {
            return null
        }
        val packageManager: PackageManager = CgeoApplication.getInstance().getPackageManager()
        try {
            // This can throw an exception where the exception type is only defined on API Level > 3
            // therefore surround with try-catch
            return packageManager.getLaunchIntentForPackage(packageName)
        } catch (final Exception ignored) {
            return null
        }
    }
    public static Drawable getApplicationIcon(final String packageName) {
        val packageManager: PackageManager = CgeoApplication.getInstance().getPackageManager()
        try {
            return packageManager.getApplicationIcon(packageName)
        } catch (final Exception ignored) {
            return null
        }
    }

    public static Boolean isIntentAvailable(final String intent) {
        return isIntentAvailable(intent, null)
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param action The Intent action to check for availability.
     * @param uri    The Intent URI to check for availability.
     * @return True if an Intent with the specified action can be sent and
     * responded to, false otherwise.
     */
    public static Boolean isIntentAvailable(final String action, final Uri uri) {
        val packageManager: PackageManager = CgeoApplication.getInstance().getPackageManager()
        final Intent intent
        if (uri == null) {
            intent = Intent(action)
        } else {
            intent = Intent(action, uri)
        }
        val list: List<ResolveInfo> = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY)
        val servicesList: List<ResolveInfo> = packageManager.queryIntentServices(intent,
                PackageManager.MATCH_DEFAULT_ONLY)
        return CollectionUtils.isNotEmpty(list) || CollectionUtils.isNotEmpty(servicesList)
    }

    public static Unit openMarket(final Activity activity, final String packageName) {
        try {
            val url: String = "market://details?id=" + packageName
            val marketIntent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            activity.startActivity(marketIntent)

        } catch (final RuntimeException ignored) {
            // market not available, fall back to browser
            val uri: String = "https://play.google.com/store/apps/details?id=" + packageName
            ShareUtils.openUrl(activity, uri)
        }
    }

    public static Unit restartApplication(final Context c) {
        try {
            if (c != null) {
                val pm: PackageManager = c.getPackageManager()
                if (pm != null) {
                    //create the intent with the default start activity for our application
                    val mStartActivity: Intent = pm.getLaunchIntentForPackage(c.getPackageName())
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        // create a pending intent so the application is restarted after System.exit(0) was called.
                        val mPendingIntent: PendingIntent = PendingIntent.getActivity(c, 1633838708, mStartActivity, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT)
                        val mgr: AlarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE)
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent)
                        System.exit(0)
                    } else {
                        Log.e("Was not able to restart application, mStartActivity null")
                    }
                } else {
                    Log.e("Was not able to restart application, PM null")
                }
            } else {
                Log.e("Was not able to restart application, Context null")
            }
        } catch (Exception ex) {
            Log.e("Was not able to restart application")
        }
    }

}
