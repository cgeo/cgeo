package cgeo.geocaching.twitter;

import cgeo.geocaching.R;
import cgeo.geocaching.network.OAuthAuthorizationActivity;
import cgeo.geocaching.settings.Settings;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.Nullable;

public class TwitterAuthorizationActivity extends OAuthAuthorizationActivity {

    public TwitterAuthorizationActivity() {
        super("api.twitter.com",
                "/oauth/request_token",
                "/oauth/authorize",
                "/oauth/access_token",
                true,
                Settings.getKeyConsumerPublic(),
                Settings.getKeyConsumerSecret());
    }

    @Override
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
    protected final String getAuthTitle() {
        return res.getString(R.string.auth_twitter);
    }

    @Override
    protected final String getAuthAgain() {
        return res.getString(R.string.auth_again);
    }

    @Override
    protected final String getErrAuthInitialize() {
        return res.getString(R.string.err_auth_initialize);
    }

    @Override
    protected final String getAuthStart() {
        return res.getString(R.string.auth_start);
    }

    @Override
    protected final String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed);
    }

    @Override
    protected final String getErrAuthProcess() {
        return res.getString(R.string.err_auth_process);
    }

    @Override
    protected final String getAuthDialogWait() {
        return res.getString(R.string.auth_dialog_wait);
    }

    @Override
    protected final String getAuthDialogPinTitle() {
        return res.getString(R.string.auth_dialog_pin_title);
    }

    @Override
    protected final String getAuthDialogPinMessage() {
        return res.getString(R.string.auth_dialog_pin_message);
    }

    @Override
    protected final String getAboutAuth1() {
        return res.getString(R.string.about_auth_1);
    }

    @Override
    protected final String getAboutAuth2() {
        return res.getString(R.string.about_auth_2);
    }

    @Override
    protected final String getAuthAuthorize() {
        return res.getString(R.string.auth_authorize);
    }

    @Override
    protected final String getAuthPinHint() {
        return res.getString(R.string.auth_pin_hint);
    }

    @Override
    protected final String getAuthFinish() {
        return res.getString(R.string.auth_finish);
    }

}
