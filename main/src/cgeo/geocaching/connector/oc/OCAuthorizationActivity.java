package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.network.OAuthAuthorizationActivity;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class OCAuthorizationActivity extends OAuthAuthorizationActivity {

    public OCAuthorizationActivity() {
        super(R.string.auth_ocde,
                "www.opencaching.de",
                "/okapi/services/oauth/request_token",
                "/okapi/services/oauth/authorize",
                "/okapi/services/oauth/access_token",
                false,
                cgeoapplication.getInstance().getResources().getString(R.string.oc_de_okapi_consumer_key),
                cgeoapplication.getInstance().getResources().getString(R.string.oc_de_okapi_consumer_secret));
    }

    @Override
    protected ImmutablePair<String, String> getTempToken() {
        return Settings.getTempOCDEToken();
    }

    @Override
    protected void setTempTokens(String tokenPublic, String tokenSecret) {
        Settings.setOCDETempTokens(tokenPublic, tokenSecret);
    }

    @Override
    protected void setTokens(String tokenPublic, String tokenSecret, boolean enable) {
        Settings.setOCDETokens(tokenPublic, tokenSecret, enable);
    }

}
