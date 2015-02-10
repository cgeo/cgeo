package cgeo.geocaching.connector.oc;

import org.eclipse.jdt.annotation.NonNull;

public interface IOCAuthParams {

    /**
     * The site name: 'www.opencaching...'
     *
     */
    @NonNull
    String getSite();

    /**
     * ResId of the Consumer key
     *
     */
    int getCKResId();

    /**
     * ResId of the Consumer secret
     *
     */
    int getCSResId();

    /**
     * ResId of the Authorization title
     *
     */
    int getAuthTitleResId();

    /**
     * Preference key of the public token
     *
     */
    int getTokenPublicPrefKey();

    /**
     * Preference key of the secret token
     *
     */
    int getTokenSecretPrefKey();

    /**
     * Preference key of the temporary public token (OAuth)
     *
     */
    int getTempTokenPublicPrefKey();

    /**
     * Preference key of the temporary secret token (OAuth)
     *
     */
    int getTempTokenSecretPrefKey();

    /**
     * The URI to use as a callback (OAuth)
     *
     */
    @NonNull
    String getCallbackUri();
}
