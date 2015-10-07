package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
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
     */
    public static boolean isLaunchable(@Nullable final String packageName) {
        return getLaunchIntent(packageName) != null;
    }

    /**
     * Checks whether a launch intent is available or if the package is just installed
     * This function is relatively costly, so if you know that the package in question has
     * a launch intent, use isLaunchable() instead.
     *
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
     * @param action
     *            The Intent action to check for availability.
     * @param uri
     *            The Intent URI to check for availability.
     *
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
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

    @SuppressWarnings("deprecation")
    public static void openMarket(final Activity activity, @NonNull final String packageName) {
        try {
            final String url = "market://details?id=" + packageName;
            final Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            activity.startActivity(marketIntent);

        } catch (final RuntimeException ignored) {
            // market not available, fall back to browser
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

}
