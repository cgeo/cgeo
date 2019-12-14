package cgeo.geocaching.twitter;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.OAuthAuthorizationActivity;
import cgeo.geocaching.settings.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class TwitterAuthorizationActivity extends OAuthAuthorizationActivity {

    public static final OAuthParameters TWITTER_OAUTH_PARAMS = new OAuthParameters(
            "api.twitter.com",
            "/oauth/request_token",
            "/oauth/authorize",
            "/oauth/access_token",
            true,
            Settings.getTwitterKeyConsumerPublic(),
            Settings.getTwitterKeyConsumerSecret(),
            "callback://www.cgeo.org/twitter/");

    @Override
    protected String getCreateAccountUrl() {
        return "https://twitter.com/signup";
    }

    @Override
    @NonNull
    protected final ImmutablePair<String, String> getTempTokens() {
        return Settings.getTempToken();
    }

    @Override
    protected final void setTempTokens(@Nullable final String tokenPublic, @Nullable final String tokenSecret) {
        Settings.setTwitterTempTokens(tokenPublic, tokenSecret);
    }

    @Override
    protected final void setTokens(@Nullable final String tokenPublic, @Nullable final String tokenSecret, final boolean enable) {
        Settings.setTwitterTokens(tokenPublic, tokenSecret, enable);
    }

    @Override
    @NonNull
    protected final String getAuthTitle() {
        return res.getString(R.string.auth_twitter);
    }

    @Override
    protected final String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_twitter);
    }

}
