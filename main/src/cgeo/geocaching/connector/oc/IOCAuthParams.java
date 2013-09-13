package cgeo.geocaching.connector.oc;

public interface IOCAuthParams {

    /**
     * The site name: 'www.openaching...'
     * 
     * @return
     */
    String getSite();

    /**
     * ResId of the Consumer key
     * 
     * @return
     */
    int getCKResId();

    /**
     * ResId of the Consumer secret
     * 
     * @return
     */
    int getCSResId();

    /**
     * ResId ot the Authorization title
     * 
     * @return
     */
    int getAuthTitelResId();

    /**
     * Preference key of the public token
     * 
     * @return
     */
    int getTokenPublicPrefKey();

    /**
     * Preference key of the secret token
     * 
     * @return
     */
    int getTokenSecretPrefKey();

    /**
     * Preference key of the temporary public token (OAuth)
     * 
     * @return
     */
    int getTempTokenPublicPrefKey();

    /**
     * Preference key of the temporary secret token (OAuth)
     * 
     * @return
     */
    int getTempTokenSecretPrefKey();

    /**
     * The URI to use as a callback (OAuth)
     *
     * @return
     */
    String getCallbackUri();
}
