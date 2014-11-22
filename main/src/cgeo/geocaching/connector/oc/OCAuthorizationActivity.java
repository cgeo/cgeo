package cgeo.geocaching.connector.oc;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.OAuthAuthorizationActivity;
import cgeo.geocaching.connector.oc.OkapiError.OkapiErrors;
import cgeo.geocaching.settings.Settings;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.Nullable;

import android.os.Bundle;

public class OCAuthorizationActivity extends OAuthAuthorizationActivity {

    private int titleResId;
    private int tokenPublicPrefKey;
    private int tokenSecretPrefKey;
    private int tempTokenPublicPrefKey;
    private int tempTokenSecretPrefKey;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            titleResId = extras.getInt(Intents.EXTRA_OAUTH_TITLE_RES_ID);
            tokenPublicPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TOKEN_PUBLIC_KEY);
            tokenSecretPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TOKEN_SECRET_KEY);
            tempTokenPublicPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TEMP_TOKEN_KEY_PREF);
            tempTokenSecretPrefKey = extras.getInt(Intents.EXTRA_OAUTH_TEMP_TOKEN_SECRET_PREF);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected ImmutablePair<String, String> getTempTokens() {
        return Settings.getTokenPair(tempTokenPublicPrefKey, tempTokenSecretPrefKey);
    }

    @Override
    protected void setTempTokens(@Nullable final String tokenPublic, @Nullable final String tokenSecret) {
        Settings.setTokens(tempTokenPublicPrefKey, tokenPublic, tempTokenSecretPrefKey, tokenSecret);
    }

    @Override
    protected void setTokens(@Nullable final String tokenPublic, @Nullable final String tokenSecret, final boolean enable) {
        Settings.setTokens(tokenPublicPrefKey, tokenPublic, tokenSecretPrefKey, tokenSecret);
        if (tokenPublic != null) {
            Settings.setTokens(tempTokenPublicPrefKey, null, tempTokenSecretPrefKey, null);
        }
    }

    @Override
    protected String getAuthTitle() {
        return res.getString(titleResId);
    }

    @Override
    protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_oc, getAuthTitle());
    }

    /**
     * Return an extended error in case of an invalid time stamp
     */
    @Override
    protected String getExtendedErrorMsg(HttpResponse response) {
        OkapiError error = OkapiClient.decodeErrorResponse(response);
        if (error.getResult() == OkapiErrors.INVALID_TIMESTAMP) {
            return res.getString(R.string.init_login_popup_invalid_timestamp);
        }
        return StringUtils.EMPTY;
    }
}
