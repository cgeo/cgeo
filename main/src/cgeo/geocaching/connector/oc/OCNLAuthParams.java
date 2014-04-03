package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;

public class OCNLAuthParams implements IOCAuthParams {

    @Override
    public String getSite() {
        return "www.opencaching.nl";
    }

    @Override
    public int getCKResId() {
        return R.string.oc_nl_okapi_consumer_key;
    }

    @Override
    public int getCSResId() {
        return R.string.oc_nl_okapi_consumer_secret;
    }

    @Override
    public int getAuthTitleResId() {
        return R.string.auth_ocnl;
    }

    @Override
    public int getTokenPublicPrefKey() {
        return R.string.pref_ocnl_tokenpublic;
    }

    @Override
    public int getTokenSecretPrefKey() {
        return R.string.pref_ocnl_tokensecret;
    }

    @Override
    public int getTempTokenPublicPrefKey() {
        return R.string.pref_temp_ocnl_token_public;
    }

    @Override
    public int getTempTokenSecretPrefKey() {
        return R.string.pref_temp_ocnl_token_secret;
    }

    @Override
    public String getCallbackUri() {
        return "callback://www.cgeo.org/opencaching.nl/";
    }
}
