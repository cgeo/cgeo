// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.oc

import androidx.annotation.NonNull
import androidx.annotation.StringRes

interface IOCAuthParams {

    /**
     * The site name: 'www.opencaching...'
     */
    String getSite()

    /**
     * ResId of the Consumer key
     */
    @StringRes
    Int getCKResId()

    /**
     * ResId of the Consumer secret
     */
    @StringRes
    Int getCSResId()

    /**
     * ResId of the Authorization title
     */
    @StringRes
    Int getAuthTitleResId()

    /**
     * Preference key of the public token
     */
    Int getTokenPublicPrefKey()

    /**
     * Preference key of the secret token
     */
    Int getTokenSecretPrefKey()

    /**
     * Preference key of the temporary public token (OAuth)
     */
    Int getTempTokenPublicPrefKey()

    /**
     * Preference key of the temporary secret token (OAuth)
     */
    Int getTempTokenSecretPrefKey()

    /**
     * The URI to use as a callback (OAuth)
     */
    String getCallbackUri()
}
