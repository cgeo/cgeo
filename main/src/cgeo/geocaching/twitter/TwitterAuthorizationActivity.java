package cgeo.geocaching.twitter;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.network.OAuthAuthorizationActivity;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class TwitterAuthorizationActivity extends OAuthAuthorizationActivity {

    public TwitterAuthorizationActivity() {
        super(R.string.auth_twitter, "api.twitter.com", "/oauth/request_token", "/oauth/authorize", "/oauth/access_token", true, Settings.getKeyConsumerPublic(), Settings.getKeyConsumerSecret());
    }

    @Override
    protected ImmutablePair<String, String> getTempToken() {
        return Settings.getTempToken();
    }

    @Override
    protected void setTempTokens(String tokenPublic, String tokenSecret) {
        Settings.setTwitterTempTokens(tokenPublic, tokenSecret);
    }

    @Override
    protected void setTokens(String tokenPublic, String tokenSecret, boolean enable) {
        Settings.setTwitterTokens(tokenPublic, tokenSecret, enable);
    }
}
