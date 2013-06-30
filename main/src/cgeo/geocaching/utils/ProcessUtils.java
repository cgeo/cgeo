package cgeo.geocaching.utils;

import cgeo.geocaching.cgeoapplication;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

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
        final List<PackageInfo> packs = cgeoapplication.getInstance().getPackageManager().getInstalledPackages(0);
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
        final PackageManager packageManager = cgeoapplication.getInstance().getPackageManager();
        try {
            // This can throw an exception where the exception type is only defined on API Level > 3
            // therefore surround with try-catch
            return packageManager.getLaunchIntentForPackage(packageName);
        } catch (final Exception e) {
            return null;
        }
    }
}
