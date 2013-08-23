package cgeo.geocaching.settings;


/**
 * provide write-access proxy to settings for testing purposes
 *
 */
public class TestSettings {

    /**
     * Purely static!
     */
    private TestSettings() {
    }

    public static void setExcludeDisabledCaches(final boolean exclude) {
        Settings.setExcludeDisabledCaches(exclude);
    }

    public static void setExcludeMine(final boolean exclude) {
        Settings.setExcludeMine(exclude);
    }

    public static boolean setLogin(final String username, final String password) {
        return Settings.setLogin(username, password);
    }

    public static void setStoreOfflineMaps(final boolean offlineMaps) {
        Settings.setStoreOfflineMaps(offlineMaps);
    }

    public static void setStoreOfflineWpMaps(final boolean offlineWpMaps) {
        Settings.setStoreOfflineWpMaps(offlineWpMaps);
    }

    public static void setUseImperialUnits(final boolean imperial) {
        Settings.setUseImperialUnits(imperial);
    }

}
