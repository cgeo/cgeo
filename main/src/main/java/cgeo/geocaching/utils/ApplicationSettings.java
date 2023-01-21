package cgeo.geocaching.utils;

import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.content.res.Configuration;

/**
 * This utility class contains static settings that do not require a context or
 * an application. It may not depend or use any other package from c:geo.
 * <br/>
 * It is used, for example, to get some settings for the BackupAgent. In this case,
 * no application is instantiated by the OS.
 */

public class ApplicationSettings {

    private ApplicationSettings() {
        // utility class
    }

    /**
     * Get the name of the preferences file.
     *
     * @return the name of the shared preferences file without the extension
     */
    public static String getPreferencesName() {
        // There is currently no Android API to get the file name of the shared preferences. Let's hardcode
        // it without needing a CgeoApplication instance.
        return "cgeo.geocaching_preferences";
    }


    /**
     * Sets a user-defined Locale for localized strings
     *
     * @param activity Activity to set the new locale for
     */
    public static void setLocale(final Activity activity) {
        if (activity != null) {
            final Configuration conf = activity.getResources().getConfiguration();
            conf.setLocale(Settings.getApplicationLocale());
            activity.getResources().updateConfiguration(conf, activity.getResources().getDisplayMetrics());
        }
    }

}
