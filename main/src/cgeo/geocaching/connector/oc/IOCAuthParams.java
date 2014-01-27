package cgeo.geocaching.connector.oc;

import org.eclipse.jdt.annotation.NonNull;

public interface IOCAuthParams {

    /**
     * The site name: 'www.opencaching...'
     *
     * @return
     */
    @NonNull
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
     * ResId of the Authorization title
     *
     * @return
     */
    int getAuthTitleResId();

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
    @NonNull
    String getCallbackUri();
}
