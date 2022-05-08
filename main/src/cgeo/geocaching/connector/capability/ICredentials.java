package cgeo.geocaching.connector.capability;


import cgeo.geocaching.settings.Credentials;

/**
 * Marker interface for connectors which have user name and password as credentials.
 */
public interface ICredentials {
    Credentials getCredentials();

    /**
     * Get preference key of the user name.
     */
    int getUsernamePreferenceKey();

    /**
     * Get preference key of the password.
     */
    int getPasswordPreferenceKey();

}
