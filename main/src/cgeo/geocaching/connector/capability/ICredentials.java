package cgeo.geocaching.connector.capability;


/**
 * Marker interface for connectors which have user name and password as credentials.
 *
 */
public interface ICredentials {
    /**
     * Get preference key of the user name.
     *
     */
    public int getUsernamePreferenceKey();

    /**
     * Get preference key of the password.
     *
     */
    public int getPasswordPreferenceKey();

}
