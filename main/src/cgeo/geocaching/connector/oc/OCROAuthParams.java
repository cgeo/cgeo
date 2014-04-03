package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;

public class OCROAuthParams implements IOCAuthParams {

    @Override
    public String getSite() {
        return "oc.opencaching.ro";
    }

    @Override
    public int getCKResId() {
        return R.string.oc_ro_okapi_consumer_key;
    }

    @Override
    public int getCSResId() {
        return R.string.oc_ro_okapi_consumer_secret;
    }

    @Override
    public int getAuthTitleResId() {
        return R.string.auth_ocro;
    }

    @Override
    public int getTokenPublicPrefKey() {
        return R.string.pref_ocro_tokenpublic;
    }

    @Override
    public int getTokenSecretPrefKey() {
        return R.string.pref_ocro_tokensecret;
    }

    @Override
    public int getTempTokenPublicPrefKey() {
        return R.string.pref_temp_ocro_token_public;
    }

    @Override
    public int getTempTokenSecretPrefKey() {
        return R.string.pref_temp_ocro_token_secret;
    }

    @Override
    public String getCallbackUri() {
        return "callback://www.cgeo.org/opencaching.ro/";
    }
}
