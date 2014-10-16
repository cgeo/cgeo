package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import org.apache.commons.collections4.CollectionUtils;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.List;

public final class ProcessUtils {

    private ProcessUtils() {
        // utility class
    }

    /**
     * Preferred method to detect the availability of an external app
     *
     * @param packageName
     * @return
     */
    public static boolean isLaunchable(final String packageName) {
        return getLaunchIntent(packageName) != null;
    }

    /**
     * Checks whether a launch intent is available or if the package is just installed
     * This function is relatively costly, so if you know that the package in question has
     * a launch intent, use isLaunchable() instead.
     *
     * @param packageName
     * @return
     */
    public static boolean isInstalled(final String packageName) {
        return isLaunchable(packageName) || hasPackageInstalled(packageName);
    }

    /**
     * This will find installed applications even without launch intent (e.g. the streetview plugin).
     */
    private static boolean hasPackageInstalled(final String packageName) {
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
    public static Intent getLaunchIntent(final String packageName) {
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

    public static boolean isIntentAvailable(final String intent) {
        return isIntentAvailable(intent, null);
    }

    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * 
     * @param action
     *            The Intent action to check for availability.
     * @param uri
     *            The Intent URI to check for availability.
     * 
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
     */
    public static boolean isIntentAvailable(final String action, final Uri uri) {
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

}
