package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.network.OAuthAuthorizationActivity;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class OCAuthorizationActivity extends OAuthAuthorizationActivity {

    private final int siteResId = R.string.auth_ocde;

    public OCAuthorizationActivity() {
        super("www.opencaching.de",
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

    @Override
    protected String getAuthTitle() {
        return res.getString(siteResId);
    }

    @Override
    protected String getAuthAgain() {
        return res.getString(R.string.auth_again_oc);
    }

    @Override
    protected String getErrAuthInitialize() {
        return res.getString(R.string.err_auth_initialize);
    }

    @Override
    protected String getAuthStart() {
        return res.getString(R.string.auth_start_oc);
    }

    @Override
    protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_oc, getAuthTitle());
    }

    @Override
    protected String getErrAuthProcess() {
        return res.getString(R.string.err_auth_process);
    }

    @Override
    protected String getAuthDialogWait() {
        return res.getString(R.string.auth_dialog_wait_oc, getAuthTitle());
    }

    @Override
    protected String getAuthDialogPinTitle() {
        return res.getString(R.string.auth_dialog_pin_title_oc);
    }

    @Override
    protected String getAuthDialogPinMessage() {
        return res.getString(R.string.auth_dialog_pin_message_oc, getAuthTitle());
    }

    @Override
    protected String getAboutAuth1() {
        return res.getString(R.string.about_auth_1_oc, getAuthTitle());
    }

    @Override
    protected String getAboutAuth2() {
        return res.getString(R.string.about_auth_2_oc, getAuthTitle(), getAuthTitle());
    }

    @Override
    protected String getAuthAuthorize() {
        return res.getString(R.string.auth_authorize_oc);
    }

    @Override
    protected String getAuthPinHint() {
        return res.getString(R.string.auth_pin_hint_oc, getAuthTitle());
    }

    @Override
    protected String getAuthFinish() {
        return res.getString(R.string.auth_finish_oc);
    }

}
