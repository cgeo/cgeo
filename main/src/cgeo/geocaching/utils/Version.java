package cgeo.geocaching.utils;

import cgeo.geocaching.cgeoapplication;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import java.util.regex.Pattern;

public class Version {

    static public enum BuildKind {
        MARKET_RELEASE, RELEASE_CANDIDATE, NIGHTLY_BUILD, DEVELOPER_BUILD;

        @Override public String toString() {
            return name().toLowerCase().replace('_', ' ');
        }
    }

    private final String packageName;
    private String versionName;
    private int versionCode;
    private static Version instance = new Version();

    final private static Pattern NB_PATTERN = Pattern.compile("-NB(\\d+)?-");
    final private static Pattern RC_PATTERN = Pattern.compile("-RC(\\d+)?-");
    final private static Pattern MARKET_PATTERN = Pattern.compile("^\\d\\d\\d\\d\\d\\.\\d\\d\\.\\d\\d$");

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

    public static BuildKind lookupKind(final String versionName) {
        if (NB_PATTERN.matcher(versionName).find()) {
            return BuildKind.NIGHTLY_BUILD;
        }
        if (RC_PATTERN.matcher(versionName).find()) {
            return BuildKind.RELEASE_CANDIDATE;
        }
        if (MARKET_PATTERN.matcher(versionName).find()) {
            return BuildKind.MARKET_RELEASE;
        }
        return BuildKind.DEVELOPER_BUILD;
    }

    public static BuildKind getVersionKind() {
        return lookupKind(instance.versionName);
    }

}
