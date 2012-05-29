package cgeo.geocaching.utils;

import cgeo.geocaching.cgeoapplication;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class Version {

    private final String packageName;
    private String versionName;
    private int versionCode;
    private static Version instance = new Version();

    private Version() {
        final Context app = cgeoapplication.getInstance();
        packageName = app.getPackageName();
        try {
            final PackageInfo packageInfo = app.getPackageManager().getPackageInfo(packageName, 0);
            versionName = packageInfo.versionName;
            versionCode = packageInfo.versionCode;
        } catch (final NameNotFoundException e) {
            // This cannot happen, let it crash later by letting the variables initialized to their default value
            Log.e("Version: unable to get package information", e);
        }
    }

    public static String getPackageName() {
        return instance.packageName;
    }

    public static String getVersionName() {
        return instance.versionName;
    }

    public static int getVersionCode() {
        return instance.versionCode;
    }

}
