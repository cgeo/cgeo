package cgeo.geocaching.settings;

import cgeo.geocaching.R;


/**
 * provide write-access proxy to settings for testing purposes
 *
 */
public final class TestSettings extends Settings {

    /**
     * Purely static!
     */
    private TestSettings() {
        throw new InstantiationError();
    }

    public static void setExcludeDisabledCaches(final boolean exclude) {
        putBoolean(R.string.pref_excludedisabled, exclude);
    }

    public static void setExcludeMine(final boolean exclude) {
        putBoolean(R.string.pref_excludemine, exclude);
    }

    public static void setLogin(final String username, final String password) {
        Settings.setLogin(username, password);
    }

    public static void setStoreOfflineMaps(final boolean offlineMaps) {
        putBoolean(R.string.pref_offlinemaps, offlineMaps);
    }

    public static void setStoreOfflineWpMaps(final boolean offlineWpMaps) {
        putBoolean(R.string.pref_offlinewpmaps, offlineWpMaps);
    }

    public static void setUseImperialUnits(final boolean imperial) {
        putBoolean(R.string.pref_units, imperial);
    }

    public static void setCacheTwitterMessage(final String template) {
        putString(R.string.pref_twitter_cache_message, template);
    }

    public static void setTrackableTwitterMessage(final String template) {
        putString(R.string.pref_twitter_trackable_message, template);
    }

}
