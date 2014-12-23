package cgeo.geocaching.connector.oc;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.OAuthAuthorizationActivity.OAuthParameters;

import org.eclipse.jdt.annotation.NonNull;

import android.content.Intent;

public class OCAuthParams extends OAuthParameters {

    public static final OCAuthParams OC_DE_AUTH_PARAMS = new OCAuthParams("www.opencaching.de", false,
            R.string.oc_de_okapi_consumer_key, R.string.oc_de_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.de/",
            R.string.auth_ocde, R.string.pref_ocde_tokenpublic, R.string.pref_ocde_tokensecret, R.string.pref_temp_ocde_token_public, R.string.pref_temp_ocde_token_secret);

    public static final OCAuthParams OC_NL_AUTH_PARAMS = new OCAuthParams("www.opencaching.nl", false,
            R.string.oc_nl_okapi_consumer_key, R.string.oc_nl_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.nl/",
            R.string.auth_ocnl, R.string.pref_ocnl_tokenpublic, R.string.pref_ocnl_tokensecret, R.string.pref_temp_ocnl_token_public, R.string.pref_temp_ocnl_token_secret);

    public static final OCAuthParams OC_PL_AUTH_PARAMS = new OCAuthParams("www.opencaching.pl", false,
            R.string.oc_pl_okapi_consumer_key, R.string.oc_pl_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.pl/",
            R.string.auth_ocpl, R.string.pref_ocpl_tokenpublic, R.string.pref_ocpl_tokensecret, R.string.pref_temp_ocpl_token_public, R.string.pref_temp_ocpl_token_secret);

    public static final OCAuthParams OC_US_AUTH_PARAMS = new OCAuthParams("www.opencaching.us", false,
            R.string.oc_us_okapi_consumer_key, R.string.oc_us_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.us/",
            R.string.auth_ocus, R.string.pref_ocus_tokenpublic, R.string.pref_ocus_tokensecret, R.string.pref_temp_ocus_token_public, R.string.pref_temp_ocus_token_secret);

    public static final OCAuthParams OC_RO_AUTH_PARAMS = new OCAuthParams("www.opencaching.ro", false,
            R.string.oc_ro_okapi_consumer_key, R.string.oc_ro_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.ro/",
            R.string.auth_ocro, R.string.pref_ocro_tokenpublic, R.string.pref_ocro_tokensecret, R.string.pref_temp_ocro_token_public, R.string.pref_temp_ocro_token_secret);

    public static final OCAuthParams OC_UK_AUTH_PARAMS = new OCAuthParams("www.opencaching.org.uk", false,
            R.string.oc_uk_okapi_consumer_key, R.string.oc_uk_okapi_consumer_secret, "callback://www.cgeo.org/opencaching.org.uk/",
            R.string.auth_ocuk, R.string.pref_ocuk_tokenpublic, R.string.pref_ocuk_tokensecret, R.string.pref_temp_ocuk_token_public, R.string.pref_temp_ocuk_token_secret);

    public final int authTitleResId;
    public final int tokenPublicPrefKey;
    public final int tokenSecretPrefKey;
    public final int tempTokenPublicPrefKey;
    public final int tempTokenSecretPrefKey;

    public OCAuthParams(@NonNull final String host, final boolean https, final int consumerKeyResId, final int consumerSecretResId, @NonNull final String callback,
            final int authTitleResId, final int tokenPublicPrefKey, final int tokenSecretPrefKey, final int tempTokePublicPrefKey, final int tempTokenSecretPrefKey) {
        super(host, "/okapi/services/oauth/request_token",
                "/okapi/services/oauth/authorize",
                "/okapi/services/oauth/access_token",
                https,
                CgeoApplication.getInstance().getString(consumerKeyResId),
                CgeoApplication.getInstance().getString(consumerSecretResId),
                callback);
        this.authTitleResId = authTitleResId;
        this.tokenPublicPrefKey = tokenPublicPrefKey;
        this.tokenSecretPrefKey = tokenSecretPrefKey;
        this.tempTokenPublicPrefKey = tempTokePublicPrefKey;
        this.tempTokenSecretPrefKey = tempTokenSecretPrefKey;
    }

    @Override
    public void setOAuthExtras(final Intent intent) {
        super.setOAuthExtras(intent);

        if (intent != null) {
            intent.putExtra(Intents.EXTRA_OAUTH_TITLE_RES_ID, authTitleResId);
            intent.putExtra(Intents.EXTRA_OAUTH_TOKEN_PUBLIC_KEY, tokenPublicPrefKey);
            intent.putExtra(Intents.EXTRA_OAUTH_TOKEN_SECRET_KEY, tokenSecretPrefKey);
            intent.putExtra(Intents.EXTRA_OAUTH_TEMP_TOKEN_KEY_PREF, tempTokenPublicPrefKey);
            intent.putExtra(Intents.EXTRA_OAUTH_TEMP_TOKEN_SECRET_PREF, tempTokenSecretPrefKey);
        }
    }


}
