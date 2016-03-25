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
    int getUsernamePreferenceKey();

    /**
     * Get preference key of the password.
     *
     */
    int getPasswordPreferenceKey();

    /**
     * Get preference key of the avatar.
     *
     */
    int getAvatarPreferenceKey();

}
