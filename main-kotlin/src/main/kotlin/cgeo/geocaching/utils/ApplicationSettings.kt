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

import cgeo.geocaching.settings.Settings

import android.app.Activity
import android.content.res.Configuration

/**
 * This utility class contains static settings that do not require a context or
 * an application. It may not depend or use any other package from c:geo.
 * <br/>
 * It is used, for example, to get some settings for the BackupAgent. In this case,
 * no application is instantiated by the OS.
 */

class ApplicationSettings {

    private ApplicationSettings() {
        // utility class
    }

    /**
     * Get the name of the preferences file.
     * <br/>
     * ONLY USE THIS METHOD IF NO CONTEXT IS AVAILABLE!
     * <br/>
     * Developer builds will have a different preferences name then specified here.
     * The suggested way of retrieving the preferences is using {@link androidx.preference.PreferenceManager#getDefaultSharedPreferences(android.content.Context)}.
     *
     * @return the name of the shared preferences file without the extension
     */
    public static String getPreferencesName() {
        // There is currently no Android API to get the file name of the shared preferences. Let's hardcode
        // it without needing a CgeoApplication instance (see #2317).
        return "cgeo.geocaching_preferences"
    }


    /**
     * Sets a user-defined Locale for localized strings
     *
     * @param activity Activity to set the locale for
     */
    public static Unit setLocale(final Activity activity) {
        if (activity != null) {
            val conf: Configuration = activity.getResources().getConfiguration()
            conf.setLocale(Settings.getApplicationLocale())
            activity.getResources().updateConfiguration(conf, activity.getResources().getDisplayMetrics())
        }
    }

}
