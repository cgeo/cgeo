package cgeo.geocaching.settings;

import cgeo.geocaching.R;


/**
 * provide write-access proxy to settings for testing purposes
 */
public final class TestSettings extends Settings {

    /**
     * Purely static!
     */
    private TestSettings() {
        throw new InstantiationError();
    }

    public static void setLogin(final Credentials credentials) {
        Settings.setLogin(credentials.getUsernameRaw(), credentials.getPasswordRaw());
    }

    public static void setUseImperialUnits(final boolean imperial) {
        putBoolean(R.string.pref_units_imperial, imperial);
    }

    public static void setCacheTwitterMessage(final String template) {
        putString(R.string.pref_twitter_cache_message, template);
    }

    public static void setTrackableTwitterMessage(final String template) {
        putString(R.string.pref_twitter_trackable_message, template);
    }

    public static void setSignature(final String signature) {
        putString(R.string.pref_signature, signature);
    }

}
