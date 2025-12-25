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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.activity.OAuthAuthorizationActivity.OAuthParameters

import android.content.Intent

import androidx.annotation.NonNull
import androidx.annotation.StringRes

class OCAuthParams : OAuthParameters() {

    public static val OC_DE_AUTH_PARAMS: OCAuthParams = OCAuthParams("www.opencaching.de",
            R.string.oc_de_okapi_consumer_key, R.string.oc_de_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.de/", true,
            R.string.auth_ocde, R.string.pref_ocde_tokenpublic, R.string.pref_ocde_tokensecret, R.string.pref_temp_ocde_token_public, R.string.pref_temp_ocde_token_secret)

    public static val OC_NL_AUTH_PARAMS: OCAuthParams = OCAuthParams("www.opencaching.nl",
            R.string.oc_nl_okapi_consumer_key, R.string.oc_nl_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.nl/", true,
            R.string.auth_ocnl, R.string.pref_ocnl_tokenpublic, R.string.pref_ocnl_tokensecret, R.string.pref_temp_ocnl_token_public, R.string.pref_temp_ocnl_token_secret)

    public static val OC_PL_AUTH_PARAMS: OCAuthParams = OCAuthParams("opencaching.pl",
            R.string.oc_pl_okapi_consumer_key, R.string.oc_pl_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.pl/", true,
            R.string.auth_ocpl, R.string.pref_ocpl_tokenpublic, R.string.pref_ocpl_tokensecret, R.string.pref_temp_ocpl_token_public, R.string.pref_temp_ocpl_token_secret)

    public static val OC_US_AUTH_PARAMS: OCAuthParams = OCAuthParams("www.opencaching.us",
            R.string.oc_us_okapi_consumer_key, R.string.oc_us_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.us/", true,
            R.string.auth_ocus, R.string.pref_ocus_tokenpublic, R.string.pref_ocus_tokensecret, R.string.pref_temp_ocus_token_public, R.string.pref_temp_ocus_token_secret)

    public static val OC_RO_AUTH_PARAMS: OCAuthParams = OCAuthParams("www.opencaching.ro",
            R.string.oc_ro_okapi_consumer_key, R.string.oc_ro_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.ro/", true,
            R.string.auth_ocro, R.string.pref_ocro_tokenpublic, R.string.pref_ocro_tokensecret, R.string.pref_temp_ocro_token_public, R.string.pref_temp_ocro_token_secret)

    public static val OC_UK_AUTH_PARAMS: OCAuthParams = OCAuthParams("opencache.uk",
            R.string.oc_uk2_okapi_consumer_key, R.string.oc_uk2_okapi_consumer_secret, "callback://www.cgeo.org/opencache.uk/", true,
            R.string.auth_ocuk, R.string.pref_ocuk2_tokenpublic, R.string.pref_ocuk2_tokensecret, R.string.pref_temp_ocuk2_token_public, R.string.pref_temp_ocuk2_token_secret)

    @StringRes
    public final Int authTitleResId
    public final Int tokenPublicPrefKey
    public final Int tokenSecretPrefKey
    public final Int tempTokenPublicPrefKey
    public final Int tempTokenSecretPrefKey

    public OCAuthParams(final String host, @StringRes final Int consumerKeyResId, @StringRes final Int consumerSecretResId, final String callback, final Boolean https,
                        @StringRes final Int authTitleResId, final Int tokenPublicPrefKey, final Int tokenSecretPrefKey, final Int tempTokenPublicPrefKey, final Int tempTokenSecretPrefKey) {
        super(host, "/okapi/services/oauth/request_token",
                "/okapi/services/oauth/authorize",
                "/okapi/services/oauth/access_token",
                https,
                CgeoApplication.getInstance().getString(consumerKeyResId),
                CgeoApplication.getInstance().getString(consumerSecretResId),
                callback)
        this.authTitleResId = authTitleResId
        this.tokenPublicPrefKey = tokenPublicPrefKey
        this.tokenSecretPrefKey = tokenSecretPrefKey
        this.tempTokenPublicPrefKey = tempTokenPublicPrefKey
        this.tempTokenSecretPrefKey = tempTokenSecretPrefKey
    }

    override     public Unit setOAuthExtras(final Intent intent) {
        super.setOAuthExtras(intent)

        if (intent != null) {
            intent.putExtra(Intents.EXTRA_OAUTH_TITLE_RES_ID, authTitleResId)
            intent.putExtra(Intents.EXTRA_OAUTH_TOKEN_PUBLIC_KEY, tokenPublicPrefKey)
            intent.putExtra(Intents.EXTRA_OAUTH_TOKEN_SECRET_KEY, tokenSecretPrefKey)
            intent.putExtra(Intents.EXTRA_OAUTH_TEMP_TOKEN_KEY_PREF, tempTokenPublicPrefKey)
            intent.putExtra(Intents.EXTRA_OAUTH_TEMP_TOKEN_SECRET_PREF, tempTokenSecretPrefKey)
        }
    }


}
