package cgeo.geocaching.utils;

import cgeo.geocaching.cgeoapplication;

import android.content.Intent;
import android.content.pm.PackageManager;

public class ProcessUtils {

    public static boolean isInstalled(final String packageName) {
        return getLaunchIntent(packageName) != null;
    }

    public static Intent getLaunchIntent(final String packageName) {
        if (packageName == null) {
            return null;
        }
        final PackageManager packageManager = cgeoapplication.getInstance().getPackageManager();
        try {
            // This can throw an exception where the exception type is only defined on API Level > 3
            // therefore surround with try-catch
            return packageManager.getLaunchIntentForPackage(packageName);
        } catch (Exception e) {
            return null;
        }
    }
}
