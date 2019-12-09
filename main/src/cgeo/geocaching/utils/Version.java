package cgeo.geocaching.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import androidx.annotation.Nullable;

public class Version {

    private Version() {
        // Do not instantiate
    }

    private static PackageInfo getPackageInfo(final Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            Log.e("Version.getPackageInfo: unable to get package information", e);
            return null;
        }
    }

    /**
     * Get the current package version name if available.
     *
     * @param context the context to use
     * @return the current package version name, or "" if unavailable
     */
    public static String getVersionName(final Context context) {
        final PackageInfo packageInfo = getPackageInfo(context);
        return packageInfo != null ? packageInfo.versionName : "";
    }

    /**
     * Get the current package version code if available.
     *
     * @param context the context to use
     * @return the current package version code, or -1 if unavailable
     */
    public static int getVersionCode(final Context context) {
        final PackageInfo packageInfo = getPackageInfo(context);
        return packageInfo != null ? packageInfo.versionCode : -1;
    }

    /**
     * Get the current package installer if available.
     *
     * @param context the context to use
     * @return the current package installer
     */
    @Nullable
    public static String getPackageInstaller(final Context context) {
        return context.getPackageManager().getInstallerPackageName(context.getPackageName());
    }

}
