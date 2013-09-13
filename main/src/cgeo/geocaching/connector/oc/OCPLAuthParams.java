package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;

public class OCPLAuthParams implements IOCAuthParams {

    @Override
    public String getSite() {
        return "www.opencaching.pl";
    }

    @Override
    public int getCKResId() {
        return R.string.oc_pl_okapi_consumer_key;
    }

    @Override
    public int getCSResId() {
        return R.string.oc_pl_okapi_consumer_secret;
    }

    @Override
    public int getAuthTitelResId() {
        return R.string.auth_ocpl;
    }

    @Override
    public int getTokenPublicPrefKey() {
        return R.string.pref_ocpl_tokenpublic;
    }

    @Override
    public int getTokenSecretPrefKey() {
        return R.string.pref_ocpl_tokensecret;
    }

    @Override
    public int getTempTokenPublicPrefKey() {
        return R.string.pref_temp_ocpl_token_public;
    }

    @Override
    public int getTempTokenSecretPrefKey() {
        return R.string.pref_temp_ocpl_token_secret;
    }

    @Override
    public String getCallbackUri() {
        return null;
    }
}
