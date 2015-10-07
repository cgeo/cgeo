package cgeo.geocaching.utils;

/**
 * This utility class contains static settings that do not require a context or
 * an application. It may not depend or use any other package from c:geo.
 * <br/>
 * It is used, for example, to get some settings for the BackupAgent. In this case,
 * no application is instantiated by the OS.
 */

public class ApplicationSettings {

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

}
