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

import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.activity.OAuthAuthorizationActivity
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.oc.OkapiError.OkapiErrors
import cgeo.geocaching.settings.Settings

import android.os.Bundle

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import okhttp3.Response
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair

class OCAuthorizationActivity : OAuthAuthorizationActivity() {

    @StringRes
    private Int titleResId
    private Int tokenPublicPrefKey
    private Int tokenSecretPrefKey
    private Int tempTokenPublicPrefKey
    private Int tempTokenSecretPrefKey
    private String urlHost

    override     public Unit onCreate(final Bundle savedInstanceState) {

        val extras: Bundle = getIntent().getExtras()
        if (extras != null) {
            titleResId = extras.getInt(Intents.EXTRA_OAUTH_TITLE_RES_ID)
            tokenPublicPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TOKEN_PUBLIC_KEY)
            tokenSecretPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TOKEN_SECRET_KEY)
            tempTokenPublicPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TEMP_TOKEN_KEY_PREF)
            tempTokenSecretPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TEMP_TOKEN_SECRET_PREF)
            urlHost = extras.getString(Intents.EXTRA_OAUTH_HOST)
        }
        super.onCreate(savedInstanceState)
    }

    override     protected String getCreateAccountUrl() {
        return getConnector().getCreateAccountUrl()
    }

    private IConnector getConnector() {
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (connector.getHost().equalsIgnoreCase(urlHost)) {
                return connector
            }
        }
        throw IllegalStateException("Cannot find connector for host " + urlHost)
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

    override     protected String getAuthTitle() {
        return res.getString(titleResId)
    }

    override     protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_oc, getAuthTitle())
    }

    /**
     * Return an extended error in case of an invalid time stamp
     *
     * @param response network response
     */
    override     protected String getExtendedErrorMsg(final Response response) {
        val error: OkapiError = OkapiClient.decodeErrorResponse(response)
        if (error.getResult() == OkapiErrors.INVALID_TIMESTAMP) {
            return res.getString(R.string.init_login_popup_invalid_timestamp)
        }
        return StringUtils.EMPTY
    }
}
