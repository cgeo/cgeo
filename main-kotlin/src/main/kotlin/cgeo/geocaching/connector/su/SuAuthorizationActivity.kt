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

package cgeo.geocaching.connector.su

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.OAuthAuthorizationActivity
import cgeo.geocaching.settings.Settings

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import org.apache.commons.lang3.tuple.ImmutablePair

class SuAuthorizationActivity : OAuthAuthorizationActivity() {

    public static val SU_OAUTH_PARAMS: OAuthParameters = OAuthParameters(
            SuConnector.getInstance().getHost(),
            "/api/oauth/request_token.php",
            "/api/oauth/authorize.php",
            "/api/oauth/access_token.php",
            SuConnector.getInstance().isHttps(),
            CgeoApplication.getInstance().getString(R.string.su_consumer_key),
            CgeoApplication.getInstance().getString(R.string.su_consumer_secret),
            "callback://www.cgeo.org/geocachingsu/")

    @StringRes
    private val titleResId: Int = R.string.auth_su
    private val tokenPublicPrefKey: Int = R.string.pref_su_tokenpublic
    private val tokenSecretPrefKey: Int = R.string.pref_su_tokensecret
    private val tempTokenPublicPrefKey: Int = R.string.pref_temp_su_token_public
    private val tempTokenSecretPrefKey: Int = R.string.pref_temp_su_token_secret

    override     protected String getCreateAccountUrl() {
        return SuConnector.getInstance().getCreateAccountUrl()
    }

    override     protected ImmutablePair<String, String> getTempTokens() {
        return Settings.getTokenPair(tempTokenPublicPrefKey, tempTokenSecretPrefKey)
    }

    override     protected Unit setTempTokens(final String tokenPublic, final String tokenSecret) {
        Settings.setTokens(tempTokenPublicPrefKey, tokenPublic, tempTokenSecretPrefKey, tokenSecret)
    }

    override     protected Unit setTokens(final String tokenPublic, final String tokenSecret, final Boolean enable) {
        Settings.setTokens(tokenPublicPrefKey, tokenPublic, tokenSecretPrefKey, tokenSecret)
        if (tokenPublic != null) {
            Settings.setTokens(tempTokenPublicPrefKey, null, tempTokenSecretPrefKey, null)
        }
    }

    override     protected final String getAuthTitle() {
        return res.getString(titleResId)
    }

    override     protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_oc, getAuthTitle())
    }

}
