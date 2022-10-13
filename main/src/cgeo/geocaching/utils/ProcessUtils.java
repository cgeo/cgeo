package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

public final class ProcessUtils {

    public static final String CHROME_PACKAGE_NAME = "com.android.chrome";

    private ProcessUtils() {
        // utility class
    }

    /**
     * Preferred method to detect the availability of an external app
     */
    public static boolean isLaunchable(@Nullable final String packageName) {
        return getLaunchIntent(packageName) != null;
    }

    public static boolean isChromeLaunchable() {
        return ProcessUtils.isLaunchable(CHROME_PACKAGE_NAME);
    }

    public static List<ResolveInfo> getInstalledBrowsers(final Context context) {
        // We're using "https://example.com" as we only want to query for web browsers, not c:geo or other apps
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"));
        return context.getPackageManager().queryIntentActivities(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);
    }


    /**
     * Checks whether a launch intent is available or if the package is just installed
     * This function is relatively costly, so if you know that the package in question has
     * a launch intent, use isLaunchable() instead.
     */
    public static boolean isInstalled(@NonNull final String packageName) {
        return isLaunchable(packageName) || hasPackageInstalled(packageName);
    }

    /**
     * This will find installed applications even without launch intent (e.g. the streetview plugin).
     */
    private static boolean hasPackageInstalled(@NonNull final String packageName) {
        final List<PackageInfo> packs = CgeoApplication.getInstance().getPackageManager().getInstalledPackages(0);
        for (final PackageInfo packageInfo : packs) {
            if (packageName.equals(packageInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This will find applications, which can be launched.
     */
    @Nullable
    public static Intent getLaunchIntent(@Nullable final String packageName) {
        if (packageName == null) {
            return null;
        }
        final PackageManager packageManager = CgeoApplication.getInstance().getPackageManager();
        try {
            // This can throw an exception where the exception type is only defined on API Level > 3
            // therefore surround with try-catch
            return packageManager.getLaunchIntentForPackage(packageName);
        } catch (final Exception ignored) {
            return null;
        }
    }

    public static boolean isIntentAvailable(@NonNull final String intent) {
        return isIntentAvailable(intent, null);
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
    public static boolean isIntentAvailable(@NonNull final String action, @Nullable final Uri uri) {
        final PackageManager packageManager = CgeoApplication.getInstance().getPackageManager();
        final Intent intent;
        if (uri == null) {
            intent = new Intent(action);
        } else {
            intent = new Intent(action, uri);
        }
        final List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        final List<ResolveInfo> servicesList = packageManager.queryIntentServices(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return CollectionUtils.isNotEmpty(list) || CollectionUtils.isNotEmpty(servicesList);
    }

    public static void openMarket(final Activity activity, @NonNull final String packageName) {
        try {
            final String url = "market://details?id=" + packageName;
            final Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            activity.startActivity(marketIntent);

        } catch (final RuntimeException ignored) {
            // market not available, fall back to browser
            final String uri = "https://play.google.com/store/apps/details?id=" + packageName;
            ShareUtils.openUrl(activity, uri);
        }
    }

    public static void restartApplication(final Context c) {
        try {
            if (c != null) {
                final PackageManager pm = c.getPackageManager();
                if (pm != null) {
                    //create the intent with the default start activity for our application
                    final Intent mStartActivity = pm.getLaunchIntentForPackage(c.getPackageName());
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        // create a pending intent so the application is restarted after System.exit(0) was called.
                        final PendingIntent mPendingIntent = PendingIntent.getActivity(c, 1633838708, mStartActivity, (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0) | PendingIntent.FLAG_CANCEL_CURRENT);
                        final AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        System.exit(0);
                    } else {
                        Log.e("Was not able to restart application, mStartActivity null");
                    }
                } else {
                    Log.e("Was not able to restart application, PM null");
                }
            } else {
                Log.e("Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Log.e("Was not able to restart application");
        }
    }

}
