package cgeo.geocaching.twitter;

import cgeo.geocaching.R;
import cgeo.geocaching.OldSettings;
import cgeo.geocaching.network.OAuthAuthorizationActivity;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class TwitterAuthorizationActivity extends OAuthAuthorizationActivity {

    public TwitterAuthorizationActivity() {
        super("api.twitter.com",
                "/oauth/request_token",
                "/oauth/authorize",
                "/oauth/access_token",
                true,
                OldSettings.getKeyConsumerPublic(),
                OldSettings.getKeyConsumerSecret());
    }

    @Override
    protected ImmutablePair<String, String> getTempToken() {
        return OldSettings.getTempToken();
    }

    @Override
    protected void setTempTokens(String tokenPublic, String tokenSecret) {
        OldSettings.setTwitterTempTokens(tokenPublic, tokenSecret);
    }

    @Override
    protected void setTokens(String tokenPublic, String tokenSecret, boolean enable) {
        OldSettings.setTwitterTokens(tokenPublic, tokenSecret, enable);
    }

    @Override
    protected String getAuthTitle() {
        return res.getString(R.string.auth_twitter);
    }

    @Override
    protected String getAuthAgain() {
        return res.getString(R.string.auth_again);
    }

    @Override
    protected String getErrAuthInitialize() {
        return res.getString(R.string.err_auth_initialize);
    }

    @Override
    protected String getAuthStart() {
        return res.getString(R.string.auth_start);
    }

    @Override
    protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed);
    }

    @Override
    protected String getErrAuthProcess() {
        return res.getString(R.string.err_auth_process);
    }

    @Override
    protected String getAuthDialogWait() {
        return res.getString(R.string.auth_dialog_wait);
    }

    @Override
    protected String getAuthDialogPinTitle() {
        return res.getString(R.string.auth_dialog_pin_title);
    }

    @Override
    protected String getAuthDialogPinMessage() {
        return res.getString(R.string.auth_dialog_pin_message);
    }

    @Override
    protected String getAboutAuth1() {
        return res.getString(R.string.about_auth_1);
    }

    @Override
    protected String getAboutAuth2() {
        return res.getString(R.string.about_auth_2);
    }

    @Override
    protected String getAuthAuthorize() {
        return res.getString(R.string.auth_authorize);
    }

    @Override
    protected String getAuthPinHint() {
        return res.getString(R.string.auth_pin_hint);
    }

    @Override
    protected String getAuthFinish() {
        return res.getString(R.string.auth_finish);
    }

}
